package com.simplecoding.chargerreservation.station.service;

import com.simplecoding.chargerreservation.common.MapStruct;
import com.simplecoding.chargerreservation.station.dto.MarkerDto;
import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.dto.StationStatsDto;
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

    // ✅ 추가 — 메인페이지 통계용
    @Transactional(readOnly = true)
    public StationStatsDto getStationStats() {
        long totalStations     = stationRepository.count();
        long totalChargers     = chargerRepository.count();
        long availableChargers = chargerRepository.countByStat("2");
        return new StationStatsDto(totalStations, totalChargers, availableChargers);
    }

    @Transactional(readOnly = true)
    public List<MarkerDto> getStationMarkers(Double lat, Double lng) {
        double radius = 1.5;
        List<MarkerProjection> projections = stationRepository.findMarkersWithinRadius(lat, lng, radius);

        return projections.stream()
                .map(p -> {
                    StationDto tempDto = mapStruct.toDto(p);
                    tempDto.setStatusInfo(
                            p.getAvailableCount() != null ? p.getAvailableCount() : 0,
                            p.getTotalCount() != null ? p.getTotalCount() : 0,
                            p.getBrokenCount() != null ? p.getBrokenCount() : 0
                    );
                    return MarkerDto.builder()
                            .statId(p.getStatId())
                            .statNm(p.getStatNm())
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .markerColor(tempDto.getMarkerColor())
                            .warningLevel(tempDto.getWarningLevel())
                            .occupancy(tempDto.getOccupancy())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StationDto> getStationsWithDistancePaged(Double lat, Double lng, int page) {
        int year = 2026;
        String season = "봄가을";

        List<MarkerProjection> list = stationRepository.findTop100StationsWithinRadius(
                lat, lng, 1.5, year, season);

        if (list.isEmpty()) return Collections.emptyList();

        List<String> statIds = list.stream().map(MarkerProjection::getStatId).collect(Collectors.toList());
        List<ChargerEntity> allChargers = chargerRepository.findByStatIdIn(statIds);

        Map<String, List<ChargerEntity>> chargerMap = allChargers.stream()
                .collect(Collectors.groupingBy(ChargerEntity::getStatId));

        Set<String> fastTypes = Set.of("01", "03", "04", "05", "06", "08");
        Set<String> brokenStats = Set.of("1", "4", "5");

        return list.stream()
                .map(p -> {
                    StationDto dto = mapStruct.toDto(p);
                    dto.setStatId(p.getStatId());
                    dto.setStatNm(p.getStatNm());
                    dto.setAddr(p.getAddr());
                    dto.setDistance(p.getDistance());
                    dto.setBnm(p.getBnm());
                    dto.setLimitYn(p.getLimitYn());
                    dto.setLimitDetail(p.getLimitDetail());
                    dto.setParkingFree(p.getParkingFree());
                    dto.setCurrentPrice(p.getCurrentPrice());
                    dto.setSlowPrice(p.getSlowPrice());

                    List<ChargerEntity> chargers = chargerMap.getOrDefault(p.getStatId(), Collections.emptyList());

                    int total = chargers.size();
                    int available = (int) chargers.stream().filter(c -> "2".equals(c.getStat())).count();
                    int broken = (int) chargers.stream().filter(c -> brokenStats.contains(c.getStat())).count();

                    dto.setStatusInfo(available, total, broken);

                    Map<Boolean, List<ChargerEntity>> split = chargers.stream()
                            .collect(Collectors.partitioningBy(c -> fastTypes.contains(c.getChargerType())));

                    processTypeDetail(dto, "급속", split.get(true), brokenStats);
                    processTypeDetail(dto, "완속", split.get(false), brokenStats);

                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StationDto getStationDetail(String statId, String currentMonth, double lat, double lng) {
        int currYear = 2026;
        int lastYear = 2025;

        String season = determineSeason(Integer.parseInt(currentMonth));

        List<Object[]> results = stationRepository.findStationDetailWithPriceHistory(
                statId, season, currYear, lastYear, lat, lng
        );

        if (results == null || results.isEmpty()) throw new RuntimeException("충전소 정보가 없습니다.");

        Object[] firstRow = results.get(0);
        StationDto dto = new StationDto();

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

        int total = ((Number) firstRow[10]).intValue();
        int available = ((Number) firstRow[11]).intValue();
        int broken = ((Number) firstRow[12]).intValue();
        dto.setStatusInfo(available, total, broken);

        List<ChargerEntity> chargers = chargerRepository.findByStatId(statId);
        Set<String> brokenStats = Set.of("4", "5");
        updateTypeStatus(dto, chargers, brokenStats);

        Double fastCurr = null;
        Double fastLast = null;
        Double slowCurr = null;
        Double slowLast = null;

        for (Object[] r : results) {
            if (r[13] != null && r[14] != null && r[15] != null) {
                double price = ((Number) r[13]).doubleValue();
                int year = ((Number) r[14]).intValue();
                String speedType = String.valueOf(r[15]);

                if ("급속".equals(speedType)) {
                    if (year == currYear) fastCurr = price;
                    else if (year == lastYear) fastLast = price;
                } else if ("완속".equals(speedType)) {
                    if (year == currYear) slowCurr = price;
                    else if (year == lastYear) slowLast = price;
                }
            }
        }

        dto.setPriceComparison(fastCurr, fastLast, slowCurr, slowLast, currentMonth);

        return dto;
    }

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
        return !"02".equals(type);
    }

    @Transactional(readOnly = true)
    public List<StationDto> searchStationsNearby(String keyword, Double lat, Double lng) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();

        List<MarkerProjection> results = stationRepository.findNearbyByKeyword(keyword.trim(), lat, lng);
        if (results.isEmpty()) return Collections.emptyList();

        List<String> statIds = results.stream().map(MarkerProjection::getStatId).collect(Collectors.toList());
        List<ChargerEntity> allChargers = chargerRepository.findByStatIdIn(statIds);
        Map<String, List<ChargerEntity>> chargerMap = allChargers.stream()
                .collect(Collectors.groupingBy(ChargerEntity::getStatId));

        Set<String> fastTypes = Set.of("01", "03", "04", "05", "06", "08");
        Set<String> brokenStats = Set.of("1", "4", "5");

        return results.stream()
                .map(p -> {
                    StationDto dto = mapStruct.toDto(p);
                    dto.setDistance(p.getDistance());

                    List<ChargerEntity> chargers = chargerMap.getOrDefault(p.getStatId(), Collections.emptyList());

                    int total = chargers.size();
                    int available = (int) chargers.stream().filter(c -> "2".equals(c.getStat())).count();
                    int broken = (int) chargers.stream().filter(c -> brokenStats.contains(c.getStat())).count();
                    dto.setStatusInfo(available, total, broken);

                    Map<Boolean, List<ChargerEntity>> split = chargers.stream()
                            .collect(Collectors.partitioningBy(c -> fastTypes.contains(c.getChargerType())));

                    processTypeDetail(dto, "급속", split.get(true), brokenStats);
                    processTypeDetail(dto, "완속", split.get(false), brokenStats);

                    return dto;
                })
                .collect(Collectors.toList());
    }

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

                log.info("✔ {}페이지 완료 / 누적 유니크 충전소: {}", pageNo, statIdSet.size());

                if (buffer.size() >= 10000) {
                    executeBatchMerge(buffer);
                    buffer.clear();
                }

                if (items.length() < numOfRows) hasMore = false;
                else pageNo++;

            } catch (Exception e) {
                log.error("수집 중 에러: {}", e.getMessage());
                break;
            }
        }

        if (!buffer.isEmpty()) executeBatchMerge(buffer);
        log.info("▶▶▶ [STATION] 총 {}건 수집 및 저장 완료", statIdSet.size());
    }

    private void executeBatchMerge(List<JSONObject> list) {
        log.info(">>> DB MERGE 실행: {}건", list.size());

        String sql = "MERGE INTO STATION s USING DUAL ON (s.STAT_ID = ?) " +
                "WHEN MATCHED THEN UPDATE SET " +
                "s.STAT_NM=?, s.ADDR=?, s.LOCATION=?, s.LAT=?, s.LNG=?, " +
                "s.USE_TIME=?, s.BNM=?, s.ZCODE=?, s.ZSCODE=?, s.KIND=?, " +
                "s.PARKING_FREE=?, s.LIMIT_YN=?, s.LIMIT_DETAIL=?, " +
                "s.UPDATED_AT = SYSDATE " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(STAT_ID, STAT_NM, ADDR, LOCATION, LAT, LNG, USE_TIME, BNM, ZCODE, ZSCODE, KIND, PARKING_FREE, LIMIT_YN, LIMIT_DETAIL, UPDATED_AT) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JSONObject item = list.get(i);
                String sid = item.optString("statId", "").trim().toUpperCase().replaceAll("\\s+", "");

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