package com.simplecoding.chargerreservation.station.service;

import com.simplecoding.chargerreservation.chargerPrice.entity.ChargerPriceEntity;
import com.simplecoding.chargerreservation.common.MapStruct;
import com.simplecoding.chargerreservation.station.dto.MarkerDto;
import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.entity.StationEntity;
import com.simplecoding.chargerreservation.station.repository.MarkerProjection;
import com.simplecoding.chargerreservation.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {

    private final JdbcTemplate jdbcTemplate;
    private final StationRepository stationRepository;
    private final com.simplecoding.chargerreservation.charger.repository.ChargerRepository chargerRepository;
    private final MapStruct mapStruct;

    // ==========================================
    // 1. 데이터 조회 및 검색 로직 (사용자 API용)
    // ==========================================

    /**
     * [기능] 지도 표시용 마커 데이터 조회 (최적화 버전)
     * - 반경 3km 이내의 충전소를 100개씩 끊어서 가져옵니다.
     * - 지도에는 많은 정보가 필요 없으므로 필수 좌표 정보(MarkerDto)만 반환하여 가볍게 유지합니다.
     */
    @Transactional(readOnly = true)
    public List<MarkerDto> getStationMarkers(Double lat, Double lng) {
        double radius = 1.5;
        // MarkerProjection을 통해 DB에서 이미 계산된 통계 데이터를 가져옴
        List<MarkerProjection> projections = stationRepository.findMarkersWithinRadius(lat, lng, radius);

        return projections.stream()
                .map(p -> {
                    // 1. MapStruct를 통해 Projection -> StationDto로 1차 변환 (주차/개방 정보 자동 포함)
                    StationDto tempDto = mapStruct.toDto(p);

                    // 2. 상태 계산 (기존 로직 유지하되 DTO 메서드 활용)
                    tempDto.setStatusInfo(
                            p.getAvailableCount() != null ? p.getAvailableCount() : 0,
                            p.getTotalCount() != null ? p.getTotalCount() : 0,
                            p.getBrokenCount() != null ? p.getBrokenCount() : 0
                    );

                    // 3. MarkerDto로 최종 변환 (화면 전달용 가벼운 객체)
                    return MarkerDto.builder()
                            .statId(p.getStatId())
                            .statNm(p.getStatNm()) // 👈 tempDto.getStatNm() 대신 p.getStatNm() 사용!
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .markerColor(tempDto.getMarkerColor())
                            .warningLevel(tempDto.getWarningLevel())
                            .occupancy(tempDto.getOccupancy())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * [기능] 주변 충전소 목록 조회 (상세 정보 포함 리스트)
     * - 마커 조회와 비슷하지만, 충전소 이름, 주소 등 상세 정보를 담은 StationDto를 반환합니다.
     * - 무한 스크롤이나 목록 페이징 UI에 사용됩니다.
     */
    @Transactional(readOnly = true)
    public List<StationDto> getStationsWithDistancePaged(Double lat, Double lng, int page) {
        // 1. 상수 및 현재 환경 설정 (실제로는 날짜 기반 유틸리티 사용 권장)
        int year = 2026;
        String season = "봄가을";
        String type = "급속"; // 목록에서 기본으로 보여줄 타입 (필요시 파라미터로 받음)

        Set<String> fastTypes = Set.of("01", "03", "04", "05", "06", "08");
        Set<String> brokenStats = Set.of("1", "4", "5");

        // 2. Repository 호출 (수정된 파라미터 반영: type, year, season 추가)
        // [중요] Repository에서 p.unitPrice AS currentPrice를 가져오도록 쿼리가 수정되어 있어야 함
        List<MarkerProjection> list = stationRepository.findStationsWithinRadiusWithPaging(
                lat, lng, 1.5, type, year, season, page * 20, 20);

        return list.stream()
                .map(p -> {
                    // 1. MapStruct 및 기본 정보 세팅
                    StationDto dto = mapStruct.toDto(p);
                    dto.setStatNm(p.getStatNm());
                    dto.setAddr(p.getAddr());
                    dto.setDistance(p.getDistance());
                    dto.setBnm(p.getBnm()); // 운영사 정보 확인
                    dto.setCurrentPrice(p.getCurrentPrice());

                    // ✨ [신규] 조인된 요금 정보 세팅 (규칙 2)
                    // MarkerProjection에 Double getCurrentPrice()가 추가되어 있어야 합니다.
                    dto.setCurrentPrice(p.getCurrentPrice());

                    // 2. 충전기 상세 현황 계산
                    // Tip: 성능 최적화가 필요하다면 이 부분을 In절 쿼리로 한방에 가져오는게 좋음
                    List<ChargerEntity> chargers = chargerRepository.findByStatId(p.getStatId());

                    int total = chargers.size();
                    int available = (int) chargers.stream().filter(c -> "2".equals(c.getStat())).count();
                    int broken = (int) chargers.stream().filter(c -> brokenStats.contains(c.getStat())).count();

                    dto.setStatusInfo(available, total, broken);

                    // 3. 급속/완속 상세 텍스트 세팅
                    Map<Boolean, List<ChargerEntity>> split = chargers.stream()
                            .collect(Collectors.partitioningBy(c -> fastTypes.contains(c.getChargerType())));

                    processTypeDetail(dto, "급속", split.get(true), brokenStats);
                    processTypeDetail(dto, "완속", split.get(false), brokenStats);

                    return dto;
                }).collect(Collectors.toList());
    }

    /**
     * [기능] 충전소 상세 정보 단건 조회
     * - 특정 마커를 클릭했을 때 해당 충전소 1개의 상세 정보를 가져옵니다.
     * - 테스트 코드에서 호출하는 핵심 메서드입니다.
     */
//    @Transactional(readOnly = true)
//    public StationDto getStationDetail(String statId, String type, String currentMonth) {
//        // 1. 올해와 작년 연도 계산
//        int currYear = LocalDate.now().getYear();
//        int lastYear = currYear - 1;
//
//        // 현재 계절 판별 (DTO의 로직을 활용하기 위해 month 전달)
//        String season = determineSeason(currentMonth);
//
//        // 2. [명령 하달] 충전소 정보와 요금 히스토리를 한 번에 조회 (리포지토리 방문)
//        List<Object[]> results = stationRepository.findStationDetailWithPriceHistory(
//                statId, type, season, currYear, lastYear
//        );
//
//        if (results.isEmpty()) {
//            throw new RuntimeException("해당 충전소 정보를 불러올 수 없습니다. ID: " + statId);
//        }
//
//        // 3. [데이터 추출] 0번 로우에서 엔티티를 꺼내 DTO로 변환
//        StationEntity entity = (StationEntity) results.get(0)[0];
//        StationDto dto = mapStruct.toDto(entity);
//
//        // 4. [요금 조립] 리스트를 돌며 올해/작년 요금을 찾아 DTO에 세팅
//        Double currPrice = null;
//        Double lastPrice = null;
//
//        for (Object[] row : results) {
//            ChargerPriceEntity priceEntity = (ChargerPriceEntity) row[1];
//            if (priceEntity.getApplyYear() == currYear) currPrice = ChargerPriceEntity.getPrice();
//            else if (priceEntity.getApplyYear() == lastYear) lastPrice = priceEntity.getPrice();
//        }
//
//        // 5. [올라가는 길] DTO 내부 로직 실행 (계절, 요금차이 계산)
//        dto.setPriceComparison(currPrice, lastPrice, Integer.parseInt(currentMonth));
//
//        // 6. [최종 반환] 모든 정보가 꽉 찬 DTO가 컨트롤러로 올라감
//        return dto;
//    }
//
//    // 계절 판별 보조 메서드
//    private String determineSeason(String monthStr) {
//        int month = Integer.parseInt(monthStr);
//        if (month >= 3 && month <= 5 || month >= 9 && month <= 11) return "봄/가을";
//        if (month >= 6 && month <= 8) return "여름";
//        return "겨울";
//    }
    /**
     * [기능] 충전소 통합 검색
     * - 검색어(키워드)를 입력받아 충전소명이나 주소 등에서 일치하는 데이터를 찾습니다.
     * - 좌표 기준이 아니므로 거리순 정렬보다는 매칭 결과 위주로 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<StationDto> searchStations(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();

        List<StationEntity> entities = stationRepository.findByIntegratedSearch(keyword.trim());

        // [수정] 수동 루프 삭제 -> MapStruct 스트림 활용
        return entities.stream()
                .map(mapStruct::toDto)
                .collect(Collectors.toList());
    }

// 보조 메서드: 타입별 상태 세팅 (코드 중복 방지)
private void processTypeDetail(StationDto dto, String type, List<ChargerEntity> list, Set<String> brokenStats) {
    if (list.isEmpty()) return;
    int total = list.size();
    int avail = (int) list.stream().filter(c -> "2".equals(c.getStat())).count();
    int broken = (int) list.stream().filter(c -> brokenStats.contains(c.getStat())).count();
    dto.setTypeDetailStatus(type, avail, total, broken);
}



    // ==========================================
    // 2. 수집 로직 (JdbcTemplate & Merge 활용)
    // ==========================================

    /**
     * 공공 API로부터 모든 충전소 데이터를 수집하여 DB에 MERGE
     */
    public void collectAllStationData() {
        String serviceKey = "6ebd5febab70800594860d7682eab328c14df15b1e1dfac30a7a011942ee6c3f";
        String url = "http://apis.data.go.kr/B552584/EvCharger/getChargerInfo";

        RestTemplate restTemplate = new RestTemplate();
        Set<String> statIdSet = new HashSet<>();
        List<JSONObject> buffer = new ArrayList<>();

        int pageNo = 1;
        int numOfRows = 9999;
        boolean hasMore = true;

        log.info("▶▶▶ [STATION] 수집 시작");

        while (hasMore) {
            try {
                URI uri = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("dataType", "JSON")
                        .build(true)
                        .toUri();

                String response = restTemplate.getForObject(uri, String.class);

                if (response == null || !response.startsWith("{")) {
                    log.error("응답 오류 발생");
                    break;
                }

                JSONObject json = new JSONObject(response);
                JSONObject itemsObj = json.optJSONObject("items");
                if (itemsObj == null) { hasMore = false; break; }

                JSONArray items = itemsObj.optJSONArray("item");
                if (items == null || items.length() == 0) { hasMore = false; break; }

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String statId = item.optString("statId", "").trim().toUpperCase().replaceAll("\\s+", "");

                    if (!statId.isEmpty() && !statIdSet.contains(statId)) {
                        statIdSet.add(statId);
                        buffer.add(item);
                    }
                }

                log.info("✔ {}페이지 완료 / 현재 누적 유니크 충전소: {}", pageNo, statIdSet.size());

                if (buffer.size() >= 10000) {
                    executeBatchMerge(buffer);
                    buffer.clear();
                }

                if (items.length() < numOfRows) hasMore = false;
                else pageNo++;

            } catch (Exception e) {
                log.error("!!! 수집 중 에러: {}", e.getMessage());
                break;
            }
        }

        if (!buffer.isEmpty()) executeBatchMerge(buffer);
        log.info("▶▶▶ [STATION] 총 {}건 수집 및 저장 완료", statIdSet.size());
    }

    /**
     * Oracle MERGE INTO Batch 실행
     */
    private void executeBatchMerge(List<JSONObject> list) {
        log.info(">>> DB MERGE 실행: {}건", list.size());

        String sql = "MERGE INTO STATION s USING DUAL ON (s.STAT_ID = ?) " +
                "WHEN MATCHED THEN UPDATE SET " +
                "s.STAT_NM=?, s.ADDR=?, s.LOCATION=?, s.LAT=?, s.LNG=?, " +
                "s.USE_TIME=?, s.BNM=?, s.ZCODE=?, s.ZSCODE=?, s.KIND=?, " +
                "s.PARKING_FREE=?, s.LIMIT_YN=?, s.LIMIT_DETAIL=? " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(STAT_ID, STAT_NM, ADDR, LOCATION, LAT, LNG, USE_TIME, BNM, ZCODE, ZSCODE, KIND, PARKING_FREE, LIMIT_YN, LIMIT_DETAIL) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JSONObject item = list.get(i);
                String sid = item.optString("statId", "").trim().toUpperCase().replaceAll("\\s+", "");

                // 파라미터 세팅 (1~14: ON 및 UPDATE / 15~28: INSERT)
                ps.setString(1, sid);
                ps.setString(2, item.optString("statNm", ""));
                ps.setString(3, item.optString("addr", ""));
                ps.setString(4, item.optString("location", ""));
                ps.setDouble(5, item.optDouble("lat", 0.0));
                ps.setDouble(6, item.optDouble("lng", 0.0));
                ps.setString(7, item.optString("useTime", ""));
                ps.setString(8, item.optString("bnm", ""));
                ps.setString(9, item.optString("zcode", ""));
                ps.setString(10, item.optString("zscode", ""));
                ps.setString(11, item.optString("kind", ""));

                String pfr = item.optString("parkingFree", "N").trim();
                ps.setString(12, pfr.length() > 1 ? pfr.substring(0, 1) : pfr);

                String lyn = item.optString("limitYn", "N").trim();
                ps.setString(13, lyn.length() > 1 ? lyn.substring(0, 1) : lyn);

                ps.setString(14, item.optString("limitDetail", ""));

                // INSERT용 동일 데이터 반복
                ps.setString(15, sid);
                ps.setString(16, item.optString("statNm", ""));
                ps.setString(17, item.optString("addr", ""));
                ps.setString(18, item.optString("location", ""));
                ps.setDouble(19, item.optDouble("lat", 0.0));
                ps.setDouble(20, item.optDouble("lng", 0.0));
                ps.setString(21, item.optString("useTime", ""));
                ps.setString(22, item.optString("bnm", ""));
                ps.setString(23, item.optString("zcode", ""));
                ps.setString(24, item.optString("zscode", ""));
                ps.setString(25, item.optString("kind", ""));
                ps.setString(26, pfr.length() > 1 ? pfr.substring(0, 1) : pfr);
                ps.setString(27, lyn.length() > 1 ? lyn.substring(0, 1) : lyn);
                ps.setString(28, item.optString("limitDetail", ""));
            }

            @Override
            public int getBatchSize() { return list.size(); }
        });
    }


}

