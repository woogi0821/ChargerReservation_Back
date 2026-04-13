package com.simplecoding.chargerreservation.station.service;

import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import com.simplecoding.chargerreservation.station.dto.MarkerDto;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.entity.StationEntity;
import com.simplecoding.chargerreservation.station.repository.StationRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class StationServiceTest {

    @Autowired
    private StationService stationService;
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ChargerRepository chargerRepository;


    @Test
    @DisplayName("부산시청 기준 반경 1.5km 충전소 마커 조회 테스트 (거리 제거 및 현황 중심)")
    void getStationMarkers() {
        // 1. Given: 부산시청 좌표 설정
        Double busanCityHallLat = 35.1798;
        Double busanCityHallLng = 129.0750;

        // 2. When: 서비스 호출
        List<MarkerDto> markers = stationService.getStationMarkers(busanCityHallLat, busanCityHallLng);

        // 3. Then: 검증 및 로그 출력
        System.out.println("=== [부산시청 인근 충전소 마커 조회 결과 (마커용)] ===");

        if (markers.isEmpty()) {
            System.out.println("❌ 해당 반경 내에 충전소가 없습니다. (DB 데이터를 확인해주세요)");
        } else {
            markers.forEach(marker -> {
                // ✅ [수정] 거리(%5.2fkm) 출력 부분 삭제
                System.out.printf("📍 충전소명: %-20s\n", marker.getStatNm());
                System.out.printf("   └─ [3단계] 현황: %-15s | [1단계] 색상: %-7s | [2단계] 경고: %-7s\n",
                        marker.getOccupancy(), marker.getMarkerColor(), marker.getWarningLevel());
                System.out.println("------------------------------------------------------------------");

                // 4. 검증 로직
                assertThat(marker.getStatId()).isNotNull();
                assertThat(marker.getStatNm()).isNotNull();

                // ✅ [수정] distance 필드가 삭제되었으므로 관련 검증 제외
                // assertThat(marker.getDistance()).isNotNull(); <-- 이 줄은 에러가 나므로 삭제되었습니다.

                // 색상 및 경고 단계 유효성 체크
                assertThat(marker.getMarkerColor()).isIn("green", "amber", "red", "gray", "black");
                assertThat(marker.getWarningLevel()).isIn("NONE", "PARTIAL", "TOTAL");

                // 현황 텍스트(occupancy)가 비어있지 않은지 확인
                assertThat(marker.getOccupancy()).isNotBlank();
            });

            System.out.println("✅ 총 " + markers.size() + "개의 마커 데이터가 성공적으로 검증되었습니다.");
            System.out.println("ℹ️ 마커 데이터에는 보안 및 요구사항에 따라 거리(distance) 정보가 포함되지 않습니다.");
        }
    }

    @Test
    @DisplayName("사용자 위치 기준 충전소 목록 및 요금 검증 테스트")
    void getStationsWithDistancePaged() {
        // 1. Given: 테스트 기준 위치 설정
        Double lat = 35.1797865;
        Double lng = 129.0750585;
        int page = 0;

        // 2. When: 서비스 호출
        List<StationDto> stations = stationService.getStationsWithDistancePaged(lat, lng, page);

        // 3. Then: 결과 출력 및 검증
        System.out.println("\n==================================================");
        System.out.println("   [ 전기차 충전소 목록 조회 결과 ]");
        System.out.println("==================================================");

        if (stations.isEmpty()) {
            System.out.println("⚠️ 반경 내에 데이터가 없습니다.");
        } else {
            stations.forEach(station -> {
                // --- 기존 출력 항목 ---
                System.out.println("📍 충전소명: " + station.getStatNm() + " (" + station.getBnm() + ")"); // 1. 충전소명
                System.out.println("   📏 거리: " + station.getDistance() + "km"); // 2. 거리

                // ✨ [신규 추가 기능] 요금 정보 (상위 10개사 외에는 안내 문구 출력)
                if (station.getCurrentPrice() != null && station.getCurrentPrice() > 0) {
                    System.out.println("   💰 현재요금: " + station.getPriceDisplayText());
                } else {
                    System.out.println("   💰 현재요금: 현장에서 확인하세요");              }

                System.out.println("   🔓 개방유무: " + station.getOpenStatus()); // 3. 개방유무
                System.out.println("   🅿️ 주차여부: " + station.getParkingInfo()); // 4. 주차여부

                // 6. 현황 (급속/완속)
                if (station.getFastChargerStatus() != null) {
                    System.out.println("   🚀 현황(급속): " + station.getFastChargerStatus());
                }
                if (station.getSlowChargerStatus() != null) {
                    System.out.println("   🐢 현황(완속): " + station.getSlowChargerStatus());
                }

                System.out.println("   🏠 주소: " + station.getAddr()); // 7. 주소
                System.out.println("--------------------------------------------------");
            });

            // --- 검증(Assertion) ---
            assertThat(stations).isNotEmpty();
            assertThat(stations.get(0).getDistance()).isNotNull();
            assertThat(stations.get(0).getAddr()).isNotBlank();
        }
    }
//    @Test
//    @DisplayName("충전소 상세 정보(12가지 항목) 및 충전기 목록 통합 검증 테스트")
//    void getStationDetailFullTest() {
//        // 1. 테스트 준비
//        String targetStatId = "ME181321";
//        String speedType = "급속";
//        String currentMonth = "04"; // 4월 (봄/가을)
//
//        // ⭐ [추가] 범내골역 좌표 (사용자 현재 위치 가정)
//        double beomnaegolLat = 35.1485;
//        double beomnaegolLng = 129.0637;
//
//        log.info("🔍 [테스트 시작] 상세 조회 검증 - ID: {}, 기준위치: 범내골", targetStatId);
//
//        // 2. 서비스 호출 (이제 좌표 파라미터를 함께 던집니다)
//        // 이 호출을 통해 쿼리 내 거리 계산 + DTO 내 현황(occupancy) 조립이 일어납니다.
//        StationDto detail = stationService.getStationDetail(
//                targetStatId, speedType, currentMonth, beomnaegolLat, beomnaegolLng
//        );
//
//        // 3. 전수 검증 (Assertion)
//        assertAll(
//                // [1, 2, 9, 10, 11] 기본 정보 (DB 컬럼 직결)
//                () -> assertNotNull(detail.getStatNm(), "1. 충전소명이 누락되었습니다."),
//                () -> assertNotNull(detail.getAddr(), "2. 주소가 누락되었습니다."),
//                () -> assertNotNull(detail.getBnm(), "9. 기관명이 누락되었습니다."),
//                () -> assertNotNull(detail.getLocation(), "10. 상세위치가 누락되었습니다."),
//                () -> assertNotNull(detail.getUseTime(), "11. 이용시간이 누락되었습니다."),
//
//                // [3] 거리 (쿼리에서 계산된 값)
//                () -> {
//                    assertNotNull(detail.getDistance(), "3. 거리 정보가 누락되었습니다.");
//                    log.info("📍 계산된 거리: {}km", detail.getDistance());
//                },
//
//                // [4] 요금 정보 (Service에서 조립)
//                () -> {
//                    assertNotNull(detail.getSeason(), "4-1. 계절 정보가 없습니다.");
//                    assertEquals("봄가을", detail.getSeason(), "4-2. 4월은 '봄가을'이어야 합니다."); // 💡 수정
//                    log.info("💰 요금 비교: 현재({}) / 작년({}) / 차이({})",
//                            detail.getCurrentPrice(), detail.getLastYearPrice(), detail.getPriceDiff());
//                },
//
//                // [5] 현황 (DTO.setStatusInfo() 결과물)
//                () -> {
//                    assertNotNull(detail.getOccupancy(), "5. 현황 정보(occupancy)가 누락되었습니다.");
//                    assertTrue(detail.getOccupancy().contains("/"), "5. 현황 형식이 올바르지 않습니다. (현재: " + detail.getOccupancy() + ")");
//                    log.info("📊 실시간 현황: {}", detail.getOccupancy());
//                },
//
//                // [6, 7, 8] 제약사항 (DB 컬럼 직결)
//                () -> assertNotNull(detail.getLimitYn(), "6. 주차가능유무 정보가 누락되었습니다."),
//                () -> assertNotNull(detail.getParkingFree(), "7. 주차요금 정보가 누락되었습니다."),
//                () -> assertNotNull(detail.getLimitDetail(), "8. 이용자제한 상세내용이 누락되었습니다."),
//
//                // [12] 업데이트 날짜 (Service에서 LocalDateTime -> String 변환)
//                () -> {
//                    assertNotNull(detail.getLastUpdated(), "12. 업데이트 날짜(UPDATED_AT)가 누락되었습니다.");
//                    log.info("📅 최종 업데이트 일시: {}", detail.getLastUpdated());
//                },
//
//                // [보너스] 연결된 충전기 대수 검증
//                () -> {
//                    if (detail.getChargers() != null) {
//                        assertEquals(100, detail.getChargers().size(), "이 충전소는 정확히 100대여야 합니다.");
//                    }
//                }
//        );
//
//        log.info("--------------------------------------------------");
//        log.info("1. 명칭: {}, 2. 주소: {}", detail.getStatNm(), detail.getAddr());
//        log.info("3. 거리: {}km", detail.getDistance());
//        log.info("4. 요금: {}원 (작년: {}, 차이: {}), 계절: {}",
//                detail.getCurrentPrice(), detail.getLastYearPrice(), detail.getPriceDiff(), detail.getSeason());
//        log.info("5. 현황: {}", detail.getOccupancy());
//        log.info("6. 주차유무: {}, 7. 주차료: {}, 8. 제한: {}",
//                detail.getLimitYn(), detail.getParkingFree(), detail.getLimitDetail());
//        log.info("9. 기관: {}, 10. 상세위치: {}, 11. 이용시간: {}",
//                detail.getBnm(), detail.getLocation(), detail.getUseTime());
//        log.info("12. 업데이트: {}", detail.getLastUpdated());
//        log.info("--------------------------------------------------");
//    }

    @Test
    @DisplayName("통합 검색 테스트 (이름/주소/운영사)")
    void searchStations() {
        String keyword = "서산";
        log.info("🔎 [테스트 시작] 통합 검색 키워드: '{}'", keyword);

        // 1. 님이 만드신 통합 검색 메서드 호출
        List<StationEntity> results = stationRepository.findByIntegratedSearch(keyword);

        assertAll(
                () -> assertFalse(results.isEmpty(), "검색 결과가 최소 1건은 있어야 합니다."),
                () -> {
                    StationEntity first = results.get(0);
                    log.info("📍 검색된 첫 번째 데이터: [이름: {}, 주소: {}, 운영사: {}]",
                            first.getStatNm(), first.getAddr(), first.getBnm());

                    // 2. 검증 로직을 쿼리와 일치시킵니다 (이름 OR 주소 OR 운영사)
                    boolean isMatch = first.getStatNm().contains(keyword) ||
                            first.getAddr().contains(keyword) ||
                            (first.getBnm() != null && first.getBnm().contains(keyword));

                    assertTrue(isMatch, "검색 결과는 이름, 주소, 운영사 중 하나에 키워드를 포함해야 합니다.");
                    log.info("✅ 전체 검색 결과 수: {}건", results.size());
                }
        );
    }


    @Test
    @DisplayName("전국 충전소 데이터 대량 수집 및 MERGE 테스트")
    void collectAllStationDataTest() {
        // 1. 실행 전 데이터 건수 확인
        Integer beforeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM STATION", Integer.class);
        log.info("📊 수집 전 STATION 테이블 건수: {}", beforeCount);

        // 2. 수집 메서드 실행
        log.info("🚀 대량 수집 프로세스 시작...");
        stationService.collectAllStationData();

        // 3. 실행 후 데이터 건수 확인
        Integer afterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM STATION", Integer.class);
        log.info("📊 수집 후 STATION 테이블 건수: {}", afterCount);

        // 4. 기본 건수 검증
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);

        // 5. 샘플 데이터 조회
        if (afterCount > 0) {
            String sampleName = jdbcTemplate.queryForObject(
                    "SELECT STAT_NM FROM STATION WHERE ROWNUM = 1", String.class);
            log.info("✅ 수집된 샘플 충전소명: {}", sampleName);

            // ⭐ 6. [중요] UPDATED_AT 데이터가 실제로 들어갔는지 전수 검사 혹은 샘플 검사
            // 방금 Merge 문에 SYSDATE를 넣었으므로, 오늘 날짜로 들어온 데이터가 있어야 함
            Integer updatedAtCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM STATION WHERE UPDATED_AT IS NOT NULL", Integer.class);

            log.info("📅 UPDATED_AT이 채워진 데이터 건수: {}", updatedAtCount);

            assertThat(updatedAtCount)
                    .withFailMessage("❌ DB에 UPDATED_AT 데이터가 들어가지 않았습니다!")
                    .isGreaterThan(0);
        }
    }
}