package com.simplecoding.chargerreservation.penalty.service;

import com.simplecoding.chargerreservation.common.SmsService;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.notification.entity.NotiType;
import com.simplecoding.chargerreservation.notification.service.NotificationService;
import com.simplecoding.chargerreservation.penalty.dto.PenaltyRequestDto;
import com.simplecoding.chargerreservation.penalty.dto.PenaltyResponseDto;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import com.simplecoding.chargerreservation.penalty.repository.PenaltyRepository;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.simplecoding.chargerreservation.member.entity.QMember.member;

@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final SmsService smsService;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    //      1. 패널티 등록 및 문자 발송 (단계별 처리)
    @Transactional
    public PenaltyResponseDto processPenaltyStep(PenaltyRequestDto requestDto, int step) {
        PenaltyHistory penalty = new PenaltyHistory();
        penalty.setMemberId(requestDto.getMemberId());
        penalty.setReservationId(requestDto.getReservationId());
        penalty.setCarNumber(requestDto.getCarNumber());
        penalty.setReason(requestDto.getReason());
        penalty.setNudgeCount(step);

        // DB 저장
        PenaltyHistory savedPenalty = penaltyRepository.save(penalty);

        // 메시지 발송 시뮬레이션
        sendStepSms(savedPenalty);

        return convertToResponseDto(savedPenalty);
    }


    //     2. [조회용] 특정 회원의 패널티 내역 전체 가져오기 (리액트 모달용)
    public List<PenaltyResponseDto> getMemberPenalties(String memberId) {
        return penaltyRepository.findByMemberId(memberId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }


    //      3. [예약팀 협업용] 오늘 이 회원이 예약 가능한지 확인
    public boolean isRestrictedToday(String memberId) {
        // 오늘 00:00:00 ~ 23:59:59 범위 설정
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // 오늘 날짜로 '3단계(최종)' 기록이 하나라도 있으면 true 반환(예약막음)
        return penaltyRepository.existsByMemberIdAndNudgeCountAndInsertTimeBetween(memberId, 3, start, end);
    }


//     [보조] 단계별 메시지 생성 및 전송

    private void sendStepSms(PenaltyHistory penalty) {
        // 1. DB에서 진짜 회원 정보(이름, 폰번호) 가져오기
        Member member = memberRepository.findById(Long.parseLong(penalty.getMemberId()))
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + penalty.getMemberId()));
        String name = member.getName();
        String phone = member.getPhone();
        String reason = "";
        String until = "오늘 자정";
        switch (penalty.getNudgeCount()) {
            case 1:
                reason = "충전 완료 후 미출차";
                until = "현재";
                break;
            case 2:
                reason = "출차 지연 10분 경과";
                until = "현재";
                break;
            case 3:
                reason = "예약 시간 15분 경과 패널티";
                until = "오늘 자정";
                break;
        }
        // 🎯 [실제 전송 구간]
        try {
            // 지환님 팀의 smsService.sendPenaltyMessage 형식을 따릅니다.
            // (수신번호, 사용자이름, 제목, 내용) 순서로 호출
            smsService.sendPenaltyMessage(
                    phone,  // penalty.getMemberId() 대신 진짜 번호 대입
                    name,   // "고객" 대신 진짜 이름 대입
                    reason,
                    until
            );

            // 전송 성공 시 상태값 변경
            penalty.setNotiSentYn("Y");
            log.info("✅ 패널티 {}단계 문자 발송 성공: 대상={},사유={}, 제한={}", penalty.getNudgeCount(), name, reason, until);
            // 2. 웹 알림 저장 (새로 추가할 부분)
            notificationService.createNotification(
                    member,
                    "패널티 안내",
                    reason + " 기록으로 인해 " + until + "까지 제한됩니다.",
                    NotiType.PENALTY,
                    "/mypage/penalty"
            );
            penalty.setNotiSentYn("Y");
            log.info("✅ 패널티 알림 통합 발송 성공: 대상={}", name);

        } catch (Exception e) {
            log.error("❌ 패널티 문자 발송 실패: {}", e.getMessage());
            penalty.setNotiSentYn("N"); // 실패 시 N으로 기록
        }
    }


//     [보조] Entity를 ResponseDto로 변환 (Builder 사용)

    private PenaltyResponseDto convertToResponseDto(PenaltyHistory penalty) {
        return PenaltyResponseDto.builder()
                .penaltyId(penalty.getPenaltyId())
                .memberId(penalty.getMemberId())
                .carNumber(penalty.getCarNumber())
                .reason(penalty.getReason())
                .nudgeCount(penalty.getNudgeCount())
                .status(penalty.getStatus())
                .notiSentYn(penalty.getNotiSentYn())
                .insertTime(penalty.getInsertTime())
                .build();
    }
    @Transactional
    public void processManualPenalty(Long reservationId, String reason) {
        // 1. DB에서 해당 예약 정보 가져오기
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 2. [Validation] 이미 취소되거나 완료된 예약인지 확인
        if (!"RESERVED".equals(res.getStatus())) {
            throw new IllegalStateException("이미 처리되었거나 취소된 예약입니다.");
        }

        // 3. 문자 발송 (SmsService 호출)
        // (주의: Member 연관관계가 설정되어 있어야 res.getMember() 사용 가능)
        smsService.sendPenaltyMessage(
// *******   Merge후 꼭 확인 (getName, getPhone 이름 동일한지) **************************
                res.getMember().getPhone(),
                res.getMember().getName(),
                "관리자 부여 패널티 안내",
                reason
        );

        // 4. DB 상태 업데이트
        res.changeStatus("CANCELLED_PENALTY"); // 관리자가 직접 준 패널티라는 뜻
        res.markAlertAsSent(); // 스케줄러가 또 건드리지 못하게 마킹

        // 패널티 발송 로직 끝부분에 추가
        notificationService.createNotification(
                res.getMember(),
                "패널티 안내",
                "장기 점유로 인해 오늘 자정까지 이용이 제한됩니다.",
                NotiType.PENALTY,
                "/mypage/penalty"
        );
    }

}