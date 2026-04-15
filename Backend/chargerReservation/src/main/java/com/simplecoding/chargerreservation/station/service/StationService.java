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
import java.time.LocalDate;
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
     * [기능] 주변 충전소 100개 통합 조회 (상세 정보 포함)
     * - 페이징을 제거하고 1.5km 이내의 가까운 충전소 100개를 한 번에 가져옵니다.
     */
    @Transactional(readOnly = true)
    public List<StationDto> getTop100Stations(Double lat, Double lng) {
        // 1. 환경 설정
        int year = 2026;
        String season = "봄가을";

        // 2. Repository 호출 (페이징 파라미터 삭제)
        // SQL 쿼리명이 findTop100StationsWithinRadius로 바뀌었다고 가정합니다.
        List<MarkerProjection> list = stationRepository.findTop100StationsWithinRadius(
                lat, lng, 1.5, year, season);

        if (list.isEmpty()) return Collections.emptyList();

        // 💡 N+1 문제 해결: 100개 충전소의 충전기 정보를 한 번에 조회
        List<String> statIds = list.stream()
                .map(MarkerProjection::getStatId)
                .collect(Collectors.toList());

        List<ChargerEntity> allChargers = chargerRepository.findByStatIdIn(statIds);
        Map<String, List<ChargerEntity>> chargerMap = allChargers.stream()
                .collect(Collectors.groupingBy(ChargerEntity::getStatId));

        Set<String> fastTypes = Set.of("01", "03", "04", "05", "06", "08");
        Set<String> brokenStats = Set.of("1", "4", "5");

        return list.stream()
                .map(p -> {
                    // 1. 기본 정보 매핑
                    StationDto dto = new StationDto(); // mapStruct 대신 직접 매핑 예시
                    dto.setStatId(p.getStatId());
                    dto.setStatNm(p.getStatNm());
                    dto.setAddr(p.getAddr());
                    dto.setDistance(p.getDistance());
                    dto.setBnm(p.getBnm());
                    dto.setLimitYn(p.getLimitYn());
                    dto.setLimitDetail(p.getLimitDetail());
                    dto.setParkingFree(p.getParkingFree());

                    // 요금 정보
                    dto.setCurrentPrice(p.getCurrentPrice()); // 급속
                    dto.setSlowPrice(p.getSlowPrice());       // 완속

                    // 2. 충전기 상세 현황
                    List<ChargerEntity> chargers = chargerMap.getOrDefault(p.getStatId(), Collections.emptyList());

                    int total = chargers.size();
                    int available = (int) chargers.stream().filter(c -> "2".equals(c.getStat())).count();
                    int broken = (int) chargers.stream().filter(c -> brokenStats.contains(c.getStat())).count();

                    // DTO의 상태 정보 업데이트 (available/total/broken)
                    dto.setStatusInfo(available, total, broken);

                    // 3. 급속/완속 상세 텍스트 및 필터용 데이터 세팅
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
    @Transactional(readOnly = true)
    public StationDto getStationDetail(String statId, String currentMonth, double lat, double lng) {
        // 1. 연도 설정 (데이터에 2026, 2025가 있으므로 이에 맞춤)
        int currYear = 2026;
        int lastYear = 2025;

        // 2. 계절 판별
        String season = determineSeason(Integer.parseInt(currentMonth));

        // 💡 쿼리에서 'type' 파라미터가 제거되었으므로 인자에서 제외
        List<Object[]> results = stationRepository.findStationDetailWithPriceHistory(
                statId, season, currYear, lastYear, lat, lng
        );

        if (results == null || results.isEmpty()) throw new RuntimeException("충전소 정보가 없습니다.");

        Object[] firstRow = results.get(0);
        StationDto dto = new StationDto();

        // --- [기본 정보 매핑] ---
        dto.setStatNm(String.valueOf(firstRow[0]));
        dto.setAddr(String.valueOf(firstRow[1]));
        dto.setBnm(String.valueOf(firstRow[2]));
        dto.setLocation(firstRow[3] != null ? firstRow[3].toString() : "정보없음");
        dto.setUseTime(String.valueOf(firstRow[4]));
        dto.setLimitYn(String.valueOf(firstRow[5]));
        dto.setParkingFree(String.valueOf(firstRow[6]));
        dto.setLimitDetail(firstRow[7] != null ? firstRow[7].toString() : "-");

        if (firstRow[8] != null) {
            dto.setLastUpdated(((java.sql.Timestamp) firstRow[8]).toLocalDateTime().toLocalDate().toString());
        }

        dto.setDistance(((Number) firstRow[9]).doubleValue());

        // --- [실시간 현황 정보 매핑] ---
        int total = ((Number) firstRow[10]).intValue();
        int available = ((Number) firstRow[11]).intValue();
        int broken = ((Number) firstRow[12]).intValue();
        dto.setStatusInfo(available, total, broken); // DTO 내부 로직 활용

        // 💡 상세 현황(급속/완속) 텍스트를 위해 충전기 리스트 기반 세팅 추가
        // 이 부분은 기존에 구현하신 processTypeDetail 로직이 있다면 호출하거나,
        // 아래와 같이 간단히 세팅할 수 있습니다.
        List<ChargerEntity> chargers = chargerRepository.findByStatId(statId);
        Set<String> brokenStats = Set.of("4", "5");

        // 급속/완속 분류 및 상태 텍스트 생성
        updateTypeStatus(dto, chargers, brokenStats);

        // --- [요금 정보 분류 매핑] ---
        Double fastCurr = null;
        Double fastLast = null;
        Double slowCurr = null;
        Double slowLast = null;

        for (Object[] r : results) {
            if (r[13] != null && r[14] != null && r[15] != null) {
                double price = ((Number) r[13]).doubleValue();
                int year = ((Number) r[14]).intValue();
                String speedType = String.valueOf(r[15]); // 💡 Repository에 추가한 SPEED_TYPE

                if ("급속".equals(speedType)) {
                    if (year == currYear) fastCurr = price;
                    else if (year == lastYear) fastLast = price;
                } else if ("완속".equals(speedType)) {
                    if (year == currYear) slowCurr = price;
                    else if (year == lastYear) slowLast = price;
                }
            }
        }

        // 💡 새로 만든 DTO의 메서드로 모든 요금 정보를 한 번에 세팅
        dto.setPriceComparison(fastCurr, fastLast, slowCurr, slowLast, currentMonth);

        return dto;
    }

    /**
     * 급속/완속 상태 텍스트 세팅 헬퍼 메서드
     */
    private void updateTypeStatus(StationDto dto, List<ChargerEntity> chargers, Set<String> brokenStats) {
        Map<String, List<ChargerEntity>> grouped = chargers.stream()
                .collect(Collectors.groupingBy(c -> isFast(c.getChargerType()) ? "급속" : "완속"));

        grouped.forEach((type, list) -> {
            int total = list.size();
            int available = (int) list.stream().filter(c -> "2".equals(c.getStat())).count();
            int broken = (int) list.stream().filter(c -> brokenStats.contains(c.getStat())).count();
            dto.setTypeDetailStatus(type, available, total, broken);
        });
    }

    private String determineSeason(int month) {
        if ((month >= 3 && month <= 5) || (month >= 9 && month <= 11)) return "봄가을";
        if (month >= 6 && month <= 8) return "여름";
        return "겨울";
    }

    private boolean isFast(String type) {
        // 기존에 사용하시던 급속 판별 로직 (01, 03, 05 등)
        return !"02".equals(type);
    }
    /**
     * [기능] 충전소 통합 검색
     * - 검색어(키워드)를 입력받아 충전소명이나 주소 등에서 일치하는 데이터를 찾습니다.
     * - 좌표 기준이 아니므로 거리순 정렬보다는 매칭 결과 위주로 반환합니다.
     */
// StationService.java

    @Transactional(readOnly = true)
    public List<StationDto> searchStationsNearby(String keyword, Double lat, Double lng) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();

        // 1. 키워드 검색 결과 (Projection)
        List<MarkerProjection> results = stationRepository.findNearbyByKeyword(keyword.trim(), lat, lng);
        if (results.isEmpty()) return Collections.emptyList();

        // 2. 실시간 상태 조회를 위한 ID 추출 및 충전기 데이터 일괄 조회 (N+1 방지)
        List<String> statIds = results.stream().map(MarkerProjection::getStatId).collect(Collectors.toList());
        List<ChargerEntity> allChargers = chargerRepository.findByStatIdIn(statIds);
        Map<String, List<ChargerEntity>> chargerMap = allChargers.stream().collect(Collectors.groupingBy(ChargerEntity::getStatId));

        Set<String> fastTypes = Set.of("01", "03", "04", "05", "06", "08");
        Set<String> brokenStats = Set.of("1", "4", "5");

        // 3. 변환 및 가공
        return results.stream()
                .map(p -> {
                    StationDto dto = mapStruct.toDto(p);
                    dto.setDistance(p.getDistance());

                    List<ChargerEntity> chargers = chargerMap.getOrDefault(p.getStatId(), Collections.emptyList());

                    // 🔥 마커 색상 계산 (이걸 안 하면 회색 마커)
                    int total = chargers.size();
                    int available = (int) chargers.stream().filter(c -> "2".equals(c.getStat())).count();
                    int broken = (int) chargers.stream().filter(c -> brokenStats.contains(c.getStat())).count();
                    dto.setStatusInfo(available, total, broken);

                    // 🔥 급속/완속 현황 텍스트 생성 (이걸 안 하면 '정보없음' 표시)
                    Map<Boolean, List<ChargerEntity>> split = chargers.stream()
                            .collect(Collectors.partitioningBy(c -> fastTypes.contains(c.getChargerType())));

                    processTypeDetail(dto, "급속", split.get(true), brokenStats);
                    processTypeDetail(dto, "완속", split.get(false), brokenStats);

                    return dto;
                })
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

        // SQL 수정: UPDATE SET과 INSERT VALUES 끝에 UPDATED_AT 추가
        String sql = "MERGE INTO STATION s USING DUAL ON (s.STAT_ID = ?) " +
                "WHEN MATCHED THEN UPDATE SET " +
                "s.STAT_NM=?, s.ADDR=?, s.LOCATION=?, s.LAT=?, s.LNG=?, " +
                "s.USE_TIME=?, s.BNM=?, s.ZCODE=?, s.ZSCODE=?, s.KIND=?, " +
                "s.PARKING_FREE=?, s.LIMIT_YN=?, s.LIMIT_DETAIL=?, " +
                "s.UPDATED_AT = SYSDATE " + // 1. 업데이트 시 현재 시간 기록
                "WHEN NOT MATCHED THEN INSERT " +
                "(STAT_ID, STAT_NM, ADDR, LOCATION, LAT, LNG, USE_TIME, BNM, ZCODE, ZSCODE, KIND, PARKING_FREE, LIMIT_YN, LIMIT_DETAIL, UPDATED_AT) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)"; // 2. 신규 삽입 시 현재 시간 기록

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JSONObject item = list.get(i);
                String sid = item.optString("statId", "").trim().toUpperCase().replaceAll("\\s+", "");

                // --- [UPDATE 및 ON 조건용 파라미터] ---
                ps.setString(1, sid);                  // ON 조건 (STAT_ID)
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
                // (15번은 SQL에서 직접 SYSDATE를 넣으므로 ps.set은 필요 없음)

                // --- [INSERT용 파라미터] ---
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
                // (마지막 SYSDATE도 자동 삽입됨)
            }

            @Override
            public int getBatchSize() { return list.size(); }
        });
    }
}

