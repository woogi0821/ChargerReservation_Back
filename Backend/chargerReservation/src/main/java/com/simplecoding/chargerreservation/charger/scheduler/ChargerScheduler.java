package com.simplecoding.chargerreservation.charger.scheduler;

import com.simplecoding.chargerreservation.charger.service.ChargerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargerScheduler {

    private final ChargerService chargerService;

    /**
     * [실시간 충전기 상태 업데이트 스케줄러]
     * 주기: 5분 (300,000ms)
     * 역할: 공공 API를 호출하여 변동된 충전기 상태를 DB에 동기화함.
     */
    @Scheduled(fixedDelay = 600000)
    public void updateChargerStatus() {
        log.info("⏰ [ChargerScheduler] 실시간 상태 동기화 시작 (10분 주기)");

        try {
            // 정해진 패키지 내의 ChargerService 호출
            chargerService.updateRecentChargerStatus();

            log.info("⏰ [ChargerScheduler] 실시간 상태 동기화 완료");
        } catch (Exception e) {
            log.error("❌ [ChargerScheduler] 동기화 중 오류 발생: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}