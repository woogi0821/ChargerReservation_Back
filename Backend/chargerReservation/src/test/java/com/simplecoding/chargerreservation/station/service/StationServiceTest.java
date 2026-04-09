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
    @Test
    @DisplayName("충전소 상세 정보 및 소속 충전기 목록 조회 테스트")
    void getStationDetail() {
        // 1. 테스트할 샘플 ID (아까 확인하신 충전기 100대 있는 곳)
        String targetStatId = "PL031084";

        log.info("🔍 [테스트 시작] 충전소 상세 조회 ID: {}", targetStatId);

        // 2. 서비스 호출 (StationEntity 내부에 List<ChargerEntity>가 FetchType.LAZY 등으로 걸려있다고 가정)
        // 또는 별도의 DTO(StationDetailDto)를 반환하는 로직
        StationEntity station = stationRepository.findById(targetStatId)
                .orElseThrow(() -> new RuntimeException("해당 충전소가 DB에 없습니다."));

        // 3. 해당 충전소에 속한 충전기 목록 조회 (Repository 활용)
        List<ChargerEntity> chargers = chargerRepository.findByStatId(targetStatId);

        // 4. 검증 (Assertion)
        assertAll(
                () -> assertNotNull(station, "충전소 정보가 null이면 안 됩니다."),
                () -> assertEquals(targetStatId, station.getStatId(), "조회된 ID가 일치해야 합니다."),
                () -> assertFalse(chargers.isEmpty(), "해당 충전소에 충전기가 최소 1개 이상은 있어야 합니다."),
                () -> {
                    log.info("✅ 충전소명: {}", station.getStatNm());
                    log.info("✅ 주소: {}", station.getAddr());
                    log.info("✅ 연결된 충전기 수: {}대", chargers.size());

                    // 아까 100개 확인하신 곳이라면 숫자가 일치하는지 확인
                    if (targetStatId.equals("PL031084")) {
                        assertEquals(100, chargers.size(), "이 충전소는 정확히 100대여야 합니다.");
                    }
                }
        );

        // 5. 샘플 데이터 출력 (상태값이 잘 들어왔는지 확인)
        if (!chargers.isEmpty()) {
            ChargerEntity sample = chargers.get(0);
            log.info("📋 충전기 샘플 [ID: {}, 상태: {}, 업데이트시간: {}]",
                    sample.getChargerId(), sample.getStat(), sample.getStatUpdDt());
        }

        log.info("========= [테스트 종료] 상세 조회 검증 완료 =========");
    }

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


//  station테이블 데이터 넣기
    @Test
    @DisplayName("전국 충전소 데이터 대량 수집 및 MERGE 테스트")
    void collectAllStationDataTest() {
        // 1. 실행 전 데이터 건수 확인
        Integer beforeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM STATION", Integer.class);
        log.info("📊 수집 전 STATION 테이블 건수: {}", beforeCount);

        // 2. 수집 메서드 실행 (Batch Merge 로직 작동)
        log.info("🚀 대량 수집 프로세스 시작...");
        try {
            stationService.collectAllStationData();
        } catch (Exception e) {
            log.error("❌ 수집 중 오류 발생: {}", e.getMessage());
            throw e;
        }

        // 3. 실행 후 데이터 건수 확인 및 검증
        Integer afterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM STATION", Integer.class);
        log.info("📊 수집 후 STATION 테이블 건수: {}", afterCount);

        // 4. 검증: 데이터가 최소한 이전보다 많거나 같아야 함 (Merge이므로)
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);

        // 5. 샘플 데이터 하나 조회해서 확인 (Optional)
        if (afterCount > 0) {
            String sampleName = jdbcTemplate.queryForObject(
                    "SELECT STAT_NM FROM STATION WHERE ROWNUM = 1", String.class);
            log.info("✅ 수집된 샘플 충전소명: {}", sampleName);
        }
    }
}