package com.simplecoding.chargerreservation.charger.service;

import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class ChargerServiceTest {

    @Autowired
    private ChargerService chargerService;

    @Autowired
    private ChargerRepository chargerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
// charger데이터 넣기
    @Test
    @Rollback(false) // 중요: 실제 DB에 데이터가 들어가는 것을 확인하려면 false 유지
    @DisplayName("공공 API 충전기 데이터 수집 및 DB 저장 통합 테스트")
    void collectAllChargerDataTest() {
        log.info("========= [테스트 시작] 데이터 수집 및 MERGE 실행 =========");

        // 1. 대용량 처리를 위해 넉넉한 타임아웃 설정
        assertTimeout(Duration.ofMinutes(20), () -> { // 10분도 부족할 수 있으니 20분 권장
            long startTime = System.currentTimeMillis();

            try {
                // 2. 실제 수집 로직 실행
                // 주의: 이 메서드 내부에서 @Transactional이 제거되어 있어야 메모리 멈춤이 없습니다.
                chargerService.collectAllChargerData();

                long endTime = System.currentTimeMillis();
                log.info("▶ 수집 및 저장 완료 소요 시간: {}초", (endTime - startTime) / 1000.0);

            } catch (Exception e) {
                log.error("❌ 테스트 실행 중 치명적 에러 발생: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });

        // 3. 결과 검증 (DB 조회)
        // 데이터가 워낙 많으므로 count 조회 시에도 시간이 걸릴 수 있습니다.
        long count = chargerRepository.count();
        log.info("▶ DB에 저장된 총 충전기 수: {}건", count);

        assertTrue(count > 0, "DB에 데이터가 하나도 저장되지 않았습니다. API 응답이나 SQL을 확인하세요.");
        log.info("========= [테스트 종료] 모든 검증 통과 =========");
    }

    @Test
    @DisplayName("실제 API 호출 및 DB 반영 확인 테스트 (롤백 없음)")
    void updateRecentChargerStatusTest() {
        log.info(">>>>>> [테스트 시작] 최근 10분간 변동된 모든 데이터 수집");

        // 1. 서비스 호출
        // 이제 내부 while문에 의해 5,000건이 넘어도 모든 페이지를 다 가져옵니다.
        chargerService.updateRecentChargerStatus();

        log.info(">>>>>> [테스트 종료] 콘솔 로그에서 '총 OO건 동기화 완료' 문구를 확인하세요.");
    }
}