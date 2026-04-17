package com.simplecoding.chargerreservation.scheduler;


import com.simplecoding.chargerreservation.common.SmsService;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j // 로그 기록남기기 위한 도구
@Component
@RequiredArgsConstructor // Repository와 Service를 자동으로 연결해줍니다.
public class PenaltyScheduler {

    private final ReservationRepository reservationRepository;
    private final SmsService smsService;
    @Transactional
    // 매 분 0초마다 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 * * * * *")
    public void checkNoShowAndCancel() {
        LocalDateTime now = LocalDateTime.now();
        // 🎯 1분 전이 예약 시작 시간이었던 타겟 조회 (14:01에 14:00 예약자 찾기)
        LocalDateTime targetTime = now.minusMinutes(1);

        log.info("⏰ 스케줄러 가동: {}분 기준 노쇼 탐지 시작", targetTime.getMinute());

        // 1. Repository에서 "1분 지난 사람"을 리스트로 뽑아온다.
        List<Reservation> alertTargets = reservationRepository.findNoShowAlertTargets(targetTime);

        // 2. 그 리스트를 for문으로 돌린다.
        for (Reservation res : alertTargets) {
            try {
                // 3. res.getMember().getPhone()으로 번호를 알아내서 문자를 쏜다.
                // (주의: Member 엔티티에 getPhoneNumber() 혹은 getPhone() 메서드가 있어야 함)

                String userPhone = res.getMember().getPhone();
                String userName = res.getMember().getName();
                res.markAlertAsSent(); // 엔티티에 만든 그 메서드! 'Y'로 바꾸는 역할

                smsService.sendPenaltyMessage(
                        userPhone,
                        userName,
                        "예약 시간 1분 경과 안내",
                        "10분 내 미충전 시 자동 취소 및 패널티가 부여됩니다."
                );

                log.info("🚫 자동 취소 완료: 사용자={}, 예약ID={}", res.getMember().getName(), res.getId());
            } catch (Exception e) {
                log.error("❌ 자동 취소 실패 (예약ID: {}): {}", res.getId(), e.getMessage());
            }
        }
    }
}
