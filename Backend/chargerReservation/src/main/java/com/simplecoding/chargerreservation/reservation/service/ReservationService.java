package com.simplecoding.chargerreservation.reservation.service;

import com.simplecoding.chargerreservation.common.SmsService;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.notification.repository.NotificationRepository;
import com.simplecoding.chargerreservation.penalty.dto.PenaltyRequestDto;
import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import com.simplecoding.chargerreservation.reservation.dto.ReservationDto;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import com.simplecoding.chargerreservation.websocket.ChargerSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.simplecoding.chargerreservation.member.entity.QMember.member;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChargerSocketController chargerSocketController;
    private final MemberRepository memberRepository;
    private final SmsService smsService;
    private final PenaltyService penaltyService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public ReservationDto.Response createReservation(Long memberId, ReservationDto.Request req) {
        long activeCount = reservationRepository.countByMemberIdAndStatusIn(
                memberId, List.of("RESERVED", "CHARGING")
        );
        if (activeCount >= 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 2건의 활성 예약이 존재하여 더 이상 예약 할 수 없습니다.");
        }
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        boolean isOccupied = reservationRepository.isChargerCurrentlyOccupied(
                req.getChargerId(), graceDeadline
        );
        if (isOccupied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 해당 충전기는 사용 중이거나 예약중 입니다.");
        }
        int durationHours = "RAPID".equalsIgnoreCase(req.getChargerType()) ? 1 : 7;
        LocalDateTime estimatedEndTime = req.getStartTime().plusHours(durationHours);
        SecureRandom secureRandom = new SecureRandom();
        String generatedPin = String.format("%06d", secureRandom.nextInt(1000000));

        Reservation reservation = Reservation.builder()
                .memberId(memberId)
                .chargerId(req.getChargerId())
                .carNumber(req.getCarNumber())
                .reservationPin(generatedPin)
                .startTime(req.getStartTime())
                .endTime(estimatedEndTime)
                .status("RESERVED")
                .statId(req.getStatId())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 1. 먼저 DB에서 회원 정보를 확실히 가져옵니다 (순서를 위로 올림)
        Member memberEntity = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        // 🔔 [추가 시작] 알림 생성 및 DB 저장
        try {
            // Notification 엔티티 생성 (프로젝트의 Notification 엔티티 필드명에 맞춰주세요)
            com.simplecoding.chargerreservation.notification.entity.Notification notification =
                    com.simplecoding.chargerreservation.notification.entity.Notification.builder()
                            .member(memberEntity)
                            .title("⚡ 예약 완료")
                            .message("충전소 예약이 정상적으로 완료되었습니다.")
                            .notiType(com.simplecoding.chargerreservation.notification.entity.NotiType.RESERVATION)
                            .targetUrl("/mypage") // 알림 클릭 시 마이페이지로 이동
                            .isRead("N")
                            .createdAt(LocalDateTime.now())
                            .build();

            notificationRepository.save(notification);
            log.info("알림 생성 완료 - 회원 ID: {}, 예약 ID: {}", memberId, savedReservation.getId());
        } catch (Exception e) {
            // 알림 생성이 실패해도 예약 프로세스에 영향을 주지 않도록 로그만 남깁니다.
            log.error("알림 생성 중 오류 발생: {}", e.getMessage());
        }

        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
            smsService.sendPinMessage(
                    member.getPhone(),
                    member.getName(),
                    generatedPin,
                    savedReservation.getStartTime(),
                    savedReservation.getEndTime()
            );
            log.info("PIN SMS 발송 완료 - 회원 : {}", member.getName());

        } catch (Exception e) {
            log.warn("PIN SMS 발송 실패 (예약 ID : {}) : {}", savedReservation.getId(), e.getMessage());
        }

        chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "RESERVED");
        chargerSocketController.pushNewReservation(req.getStatId(), req.getChargerId());

        return ReservationDto.Response.builder()
                .id(savedReservation.getId())
                .chargerId(savedReservation.getChargerId())
                .carNumber(savedReservation.getCarNumber())
                .reservationPin(savedReservation.getReservationPin())
                .startTime(savedReservation.getStartTime())
                .endTime(savedReservation.getEndTime())
                .status(savedReservation.getStatus())
                .isAlertSent(savedReservation.getIsAlertSent())
                .statId(savedReservation.getStatId())
                .build();

    }

    public List<ReservationDto.Response> getMyReservations(Long memberId) {
        List<Reservation> reservations = reservationRepository.findByMemberIdOrderByStartTimeDesc(memberId);
        return reservations.stream()
                .map(r -> ReservationDto.Response.builder()
                        .id(r.getId())
                        .chargerId(r.getChargerId())
                        .carNumber(r.getCarNumber())
                        .reservationPin(r.getReservationPin())
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .status(r.getStatus())
                        .actualEndTime(r.getActualEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository
                .findByIdAndMemberId(reservationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "예약을 찾을 수 없거나 본인의 예약이 아닙니다."));
        if ("CHARGING".equals(reservation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "충전 중인 예약은 취소할 수 없습니다. 키오스크에서 직접 종료해 주세요.");
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "취소가능한 상태가 아닙니다. (현재 상태 : " + reservation.getStatus() + ")");
        }
        reservation.changeStatus("CANCELED");
        chargerSocketController.pushStatus(reservation.getStatId(), reservation.getChargerId(), "AVAILABLE");
    }

    public boolean isChargerAvailable(String chargerId) {
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        return !reservationRepository.isChargerCurrentlyOccupied(chargerId, graceDeadline);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void sendNoShowAlertSms() {
        LocalDateTime targetTime = LocalDateTime.now().minusMinutes(1);
        List<Reservation> targets = reservationRepository.findNoShowAlertTargets(targetTime);

        targets.forEach(r -> {
            try {
                memberRepository.findById(r.getMemberId()).ifPresent(member -> smsService.sendPenaltyMessage(
                        member.getPhone(),
                        member.getName(),
                        "노쇼 위험 - 15분 내 충전 미시작",
                        "15분 후 자동 취소 및 패널티 부여"
                )
                );
                r.markAlertAsSent();
                log.info("노쇼 경고 SMS 발송 완료 - 예약ID = {}", r.getId());
            } catch (Exception e) {
                log.warn("노쇼 경고 SMS발송 실패 - 예약ID = {}", r.getId(), e.getMessage());
            }
        });
    }

    // 노쇼 처리: 예약 시간 15분 경과 후에도 RESERVED 상태면 NO_SHOW로 변경 + 패널티 부여
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processNoShow() {
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        List<Reservation> noShows = reservationRepository
                .findByStatusAndStartTimeBefore("RESERVED", graceDeadline);
        noShows.forEach(r -> {
            r.changeStatus("NO_SHOW");
            PenaltyRequestDto penaltyDto = new PenaltyRequestDto();
            penaltyDto.setMemberId(String.valueOf(r.getMemberId()));
            penaltyDto.setReservationId(r.getId());
            penaltyDto.setCarNumber(r.getCarNumber());
            penaltyDto.setReason("예약 시간 15분 경과 노쇼 패널티 부여");
            penaltyService.processPenaltyStep(penaltyDto, 3);
            log.info("노쇼 패널티 처리 완료: 회원ID={}, 예약ID={}", r.getMemberId(), r.getId());
        });
    }

    // 충전 시간 초과 자동 종료: endTime이 지난 CHARGING 상태를 DONE으로 변경
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processExpiredCharging() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository
                .findByStatusAndEndTimeBefore("CHARGING", now);
        expired.forEach(r -> {
            r.endCharging("DONE", now);
            chargerSocketController.pushStatus(r.getStatId(), r.getChargerId(), "DONE");
            log.info("충전 시간 초과 자동 종료 - 충전기 : {}, 예약ID : {}", r.getChargerId(), r.getId());
        });
    }

    public ReservationDto.Response getReservation(Long reservationId, Long memberId) {
        Reservation r = reservationRepository
                .findByIdAndMemberId(reservationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "예약을 찾을 수 없거나 본인의 예약이 아닙니다."));

        return ReservationDto.Response.builder()
                .id(r.getId())
                .chargerId(r.getChargerId())
                .carNumber(r.getCarNumber())
                .reservationPin(r.getReservationPin())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .actualEndTime(r.getActualEndTime())
                .isAlertSent(r.getIsAlertSent())
                .statId(r.getStatId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReservationDto.Response> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
                .map(res -> new ReservationDto.Response(res))
                .collect(Collectors.toList());
    }
}
