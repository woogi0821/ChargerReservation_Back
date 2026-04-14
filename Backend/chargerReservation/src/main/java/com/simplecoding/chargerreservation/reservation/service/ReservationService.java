package com.simplecoding.chargerreservation.reservation.service;

import com.simplecoding.chargerreservation.common.SmsService;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChargerSocketController chargerSocketController;
    private final MemberRepository memberRepository;
    private final SmsService smsService;

    @Transactional
    public ReservationDto.Response createReservation(Long memberId, ReservationDto.Request req) {
        long activeCount = reservationRepository.countByMemberIdAndStatusIn(
                memberId, List.of("RESERVED" , "CHARGING")
        );
        //한개의 계정에서 2개 이상의 예약이 생길때 예외처리
        if (activeCount >= 2){
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 2건의 활성 예약이 존재하여 더 이상 예약 할 수 없습니다.");
        }
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        boolean isOccupied = reservationRepository.isChargerCurrentlyOccupied(
                req.getChargerId(),graceDeadline
        );
        if (isOccupied){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 해당 충전기는 사용 중이거나 예약중 입니다.");
        }
        int durationHours = "RAPID".equalsIgnoreCase(req.getChargerType()) ? 1:7;
        LocalDateTime estimatedEndTime = req.getStartTime().plusHours(durationHours);
        String generatedPin = String.format("%04d",(int)(Math.random()*10000));

        Reservation reservation = Reservation.builder()
                .memberId(memberId)
                .chargerId(req.getChargerId())
                .carNumber(req.getCarNumber())
                .reservationPin(generatedPin)
                .startTime(req.getStartTime())
                .endTime(estimatedEndTime)
                .status("RESERVED")
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        chargerSocketController.pushStatus(req.getChargerId(),"RESERVED");

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

        return ReservationDto.Response.builder()
                .id(savedReservation.getId())
                .chargerId(savedReservation.getChargerId())
                .carNumber(savedReservation.getCarNumber())
                .reservationPin(savedReservation.getReservationPin())
                .startTime(savedReservation.getStartTime())
                .endTime(savedReservation.getEndTime())
                .status(savedReservation.getStatus())
                .isAlertSent(savedReservation.getIsAlertSent())
                .build();
    }
    public List<ReservationDto.Response> getMyReservations(Long memberId){
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
                        .build()).collect(Collectors.toList());
    }
    @Transactional
    public void cancelReservation(Long reservationId,Long memberId){
        //검증단계 1 -> reservationId + memberId 동시조회
        //두 조건이 맞아야 조회되므로 남의 예약 취소 자동 차단
        Reservation reservation = reservationRepository
                .findByIdAndMemberId(reservationId, memberId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"예약을 찾을 수 없거나 본인의 예약이 아닙니다."));
                //검증단계 2 -> 이미 충전 중인 예약은 취소 불가
                if ("CHARGING".equals(reservation.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"충전 중인 예약은 취소할 수 없습니다. 키오스크에서 직접 종료해 주세요.");
                }
                //검증단계 3 -> RESERVED상태일때만 취소 허용
                if (!"RESERVED".equals(reservation.getStatus())){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"취소가능한 상태가 아닙니다. (현재 상태 : " + reservation.getStatus() + ")");
                }
                //Entity의 ChangeStatus()메소드를 통해 상태 변경
                //@Transactional이 붙어 있으므로 메소드 종료 시 자동으로 UPDATE 쿼리 실행(더티 체킹)
                reservation.changeStatus("CANCELED");
    }
    public boolean isChargerAvailable (String chargerId){
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        return !reservationRepository.isChargerCurrentlyOccupied(chargerId,graceDeadline);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processNoShow(){
        LocalDateTime graceDeadline = LocalDateTime.now().minusMinutes(15);
        List<Reservation> noShows = reservationRepository
                .findByStatusAndStartTimeBefore("RESERVED", graceDeadline);
        noShows.forEach(r -> r.changeStatus("NO_SHOW"));
    }

    @Scheduled(fixedDelay =  60000)
    @Transactional
    public void processExpiredCharging(){
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository
                .findByStatusAndEndTimeBefore("CHARGING", now);
        expired.forEach(r -> {r.endCharging("DONE",now);
        chargerSocketController.pushStatus(r.getChargerId(),"DONE");
        log.info("충전 시간 초과 자동 종료 - 충전기 : {}, 예약ID : {}",r.getChargerId(), r.getId());}
        );
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
                .build();
    }
}
