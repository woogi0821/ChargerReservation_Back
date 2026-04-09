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
    @Transactional(readOnly = true)
    public StationDto getStationDetail(String statId, String type, String currentMonth, double lat, double lng) {
        // 1. 연도 설정 (데이터에 2026, 2025가 있으므로 이에 맞춤)
        int currYear = 2026;
        int lastYear = 2025;

        // 💡 2. 계절 판별 (DB 값인 "봄가을"로 리턴되도록 수정됨)
        String season = determineSeason(Integer.parseInt(currentMonth));

        List<Object[]> results = stationRepository.findStationDetailWithPriceHistory(
                statId, type, season, currYear, lastYear, lat, lng
        );

        if (results == null || results.isEmpty()) throw new RuntimeException("정보 없음");

        Object[] row = results.get(0);
        StationDto dto = new StationDto();

        // [1, 2, 9, 10, 11] 기본 정보
        dto.setStatNm(String.valueOf(row[0]));
        dto.setAddr(String.valueOf(row[1]));
        dto.setBnm(String.valueOf(row[2]));
        dto.setLocation(row[3] != null ? row[3].toString() : "정보없음");
        dto.setUseTime(String.valueOf(row[4]));

        // [6, 7, 8] 제약 사항
        dto.setLimitYn(String.valueOf(row[5]));
        dto.setParkingFree(String.valueOf(row[6]));
        dto.setLimitDetail(row[7] != null ? row[7].toString() : "-");

        // [12] 업데이트 날짜
        if (row[8] != null) {
            dto.setLastUpdated(((java.sql.Timestamp) row[8]).toLocalDateTime().toLocalDate().toString());
        }

        // [3] 거리 및 [5] 현황
        dto.setDistance(((Number) row[9]).doubleValue());
        int total = ((Number) row[10]).intValue();
        int available = ((Number) row[11]).intValue();
        int broken = ((Number) row[12]).intValue();
        // 📊 현황 포맷: 2/4 (고장 2)
        dto.setOccupancy(available + "/" + total + " (고장 " + broken + ")");

        // 💡 [4] 요금 계산 (Number 타입으로 안전하게 캐스팅)
        Double currentPrice = null;
        Double lastYearPrice = null;

        for (Object[] r : results) {
            if (r[13] != null && r[14] != null) {
                double price = ((Number) r[13]).doubleValue();
                int year = ((Number) r[14]).intValue();

                if (year == currYear) currentPrice = price;
                else if (year == lastYear) lastYearPrice = price;
            }
        }

        dto.setCurrentPrice(currentPrice);
        dto.setLastYearPrice(lastYearPrice);
        dto.setPriceDiff(currentPrice != null && lastYearPrice != null ? currentPrice - lastYearPrice : null);
        dto.setSeason(season); // "봄가을"

        return dto;
    }

    // 💡 슬래시 제거 버전
    private String determineSeason(int month) {
        if (month >= 3 && month <= 5 || month >= 9 && month <= 11) return "봄가을";
        if (month >= 6 && month <= 8) return "여름";
        return "겨울";
    }
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

