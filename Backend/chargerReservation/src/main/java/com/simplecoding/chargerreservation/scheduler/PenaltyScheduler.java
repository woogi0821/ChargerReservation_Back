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
    // ⚠️ 비활성화: ReservationService.processNoShowPipeline()으로 통합됨
    // 해당 스케줄러가 동시에 실행되면 동일 예약에 SMS가 중복 발송되는 문제가 있었음
    // (경고 SMS + 패널티 SMS를 ReservationService 단일 스케줄러에서 순서대로 처리)
//    @Transactional
//    @Scheduled(cron = "0 * * * * *")
//    public void checkNoShowAndCancel() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime targetTime = now.minusMinutes(1);
//        log.info("⏰ 스케줄러 가동: {}분 기준 노쇼 탐지 시작", targetTime.getMinute());
//        List<Reservation> alertTargets = reservationRepository.findNoShowAlertTargets(targetTime);
//        for (Reservation res : alertTargets) {
//            try {
//                String userPhone = res.getMember().getPhone();
//                String userName = res.getMember().getName();
//                res.markAlertAsSent();
//                smsService.sendPenaltyMessage(
//                        userPhone,
//                        userName,
//                        "예약 시간 1분 경과 안내",
//                        "10분 내 미충전 시 자동 취소 및 패널티가 부여됩니다."
//                );
//                log.info("🚫 자동 취소 완료: 사용자={}, 예약ID={}", res.getMember().getName(), res.getId());
//            } catch (Exception e) {
//                log.error("❌ 자동 취소 실패 (예약ID: {}): {}", res.getId(), e.getMessage());
//            }
//        }
//    }
}
