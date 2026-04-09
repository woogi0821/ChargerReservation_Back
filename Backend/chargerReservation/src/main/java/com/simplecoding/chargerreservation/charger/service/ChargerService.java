package com.simplecoding.chargerreservation.charger.service;

import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerService {

    private final JdbcTemplate jdbcTemplate;
    private final ChargerRepository chargerRepository;

    /**
     * [스케줄러용] 최근 10분간 변동된 충전기 상태만 업데이트
     */
    @Transactional
    public void updateRecentChargerStatus() {
        String serviceKey = "6ebd5febab70800594860d7682eab328c14df15b1e1dfac30a7a011942ee6c3f";
        String url = "http://apis.data.go.kr/B552584/EvCharger/getChargerStatus";

        RestTemplate restTemplate = new RestTemplate();
        int pageNo = 1;
        int numOfRows = 8000;
        boolean hasMore = true;
        int totalUpdated = 0;

        while (hasMore) {
            try {
                URI uri = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("period", 20)
                        .queryParam("dataType", "JSON")
                        .build(true).toUri();

                String response = restTemplate.getForObject(uri, String.class);
                if (response == null || !response.contains("item")) break;

                JSONObject json = new JSONObject(response);
                JSONObject itemsObj = json.optJSONObject("items");
                if (itemsObj == null) break;

                Object itemObj = itemsObj.get("item");
                JSONArray items;
                if (itemObj instanceof JSONArray) {
                    items = (JSONArray) itemObj;
                } else {
                    items = new JSONArray().put(itemObj);
                }

                if (items.length() > 0) {
                    updateOnlyStatus(items);
                    totalUpdated += items.length();
                    log.info("✔ [스케줄러] {}페이지 {}건 업데이트 완료", pageNo, items.length());
                }

                if (items.length() < numOfRows) {
                    hasMore = false;
                } else {
                    pageNo++;
                }

            } catch (Exception e) {
                log.error("!!! 스케줄러 실행 중 에러 (Page {}): {}", pageNo, e.getMessage());
                break;
            }
        }
        log.info("▶ [스케줄러] 총 {}건의 실시간 상태 동기화 완료", totalUpdated);
    }

    /**
     * [수정됨] 상태값 및 실시간 충전 이력 3종 업데이트
     */
    private void updateOnlyStatus(JSONArray items) {
        String sql = "UPDATE CHARGER SET STAT = ?, STAT_UPD_DT = ?, " +
                "LAST_TSDT = ?, LAST_TEDT = ?, NOW_TSDT = ?, UPDATED_AT = SYSDATE " +
                "WHERE STAT_ID = ? AND CHARGER_ID = ?";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JSONObject item = items.getJSONObject(i);
                ps.setString(1, item.optString("stat", "9"));
                ps.setString(2, item.optString("statUpdDt", ""));
                ps.setString(3, item.optString("lastTsdt", "")); // 추가
                ps.setString(4, item.optString("lastTedt", "")); // 추가
                ps.setString(5, item.optString("nowTsdt", ""));  // 추가
                ps.setString(6, item.optString("statId").trim().toUpperCase());
                ps.setString(7, item.optString("chgerId").trim().toUpperCase());
            }

            @Override
            public int getBatchSize() {
                return items.length();
            }
        });
    }
//    region

    /**
     * 공공 API 수집부터 DB MERGE까지 한 번에 처리
     */
// 1. 클래스 상단 @Transactional은 제거하세요!
    public void collectAllChargerData() {
        String serviceKey = "6ebd5febab70800594860d7682eab328c14df15b1e1dfac30a7a011942ee6c3f";
        String url = "http://apis.data.go.kr/B552584/EvCharger/getChargerInfo";

        RestTemplate restTemplate = new RestTemplate();
        // uniqueKeys는 메모리 폭발의 위험이 있으므로, 정말 중복이 심한 게 아니라면 제거를 고려하거나
        // 최소한의 필드만 담으세요. (여기서는 유지하되 buffer 관리를 철저히 합니다)
        Set<String> uniqueKeys = new HashSet<>();
        List<JSONObject> buffer = new ArrayList<>();

        log.info("▶▶▶ [STATION] 유효 ID 조회 중...");
        List<String> validIds = jdbcTemplate.queryForList("SELECT STAT_ID FROM STATION", String.class);
        Set<String> validStatIdSet = new HashSet<>(validIds);
        log.info("▶▶▶ [STATION] 총 {}개의 부모 키 로드 완료", validStatIdSet.size());

        int pageNo = 1;
        int numOfRows = 5000;
        boolean hasMore = true;

        while (hasMore) {
            try {
                log.info("📡 API 호출 중... (페이지: {})", pageNo); // 진행 상황 로그 추가

                URI uri = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("dataType", "JSON")
                        .build(true).toUri();

                String response = restTemplate.getForObject(uri, String.class);
                if (response == null || !response.startsWith("{")) break;

                JSONObject json = new JSONObject(response);
                JSONObject itemsObj = json.optJSONObject("items");
                if (itemsObj == null) break;

                JSONArray items = itemsObj.optJSONArray("item");
                if (items == null || items.length() == 0) break;

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String sid = item.optString("statId", "").trim().toUpperCase();
                    String cid = item.optString("chgerId", "").trim().toUpperCase();

                    if (!sid.isEmpty() && !cid.isEmpty() && validStatIdSet.contains(sid)) {
                        String compositeKey = sid + "_" + cid;
                        if (!uniqueKeys.contains(compositeKey)) {
                            uniqueKeys.add(compositeKey);
                            buffer.add(item);
                        }
                    }
                }

                // 1,000개 단위로 즉시 저장하여 메모리 비우기
                if (buffer.size() >= 5000) {
                    saveToDb(buffer);
                    log.info("✔ {}개 데이터 중간 저장 완료 (누적 중복 제거 키: {})", buffer.size(), uniqueKeys.size());
                    buffer.clear();
                    // 중간중간 메모리 정리를 위해 uniqueKeys가 너무 커지면 비워주는 전략도 필요할 수 있습니다.
                    if (uniqueKeys.size() > 100000) uniqueKeys.clear();
                }

                if (items.length() < numOfRows) hasMore = false;
                else pageNo++;

            } catch (Exception e) {
                log.error("!!! 수집 중 에러 발생: {}", e.getMessage());
                break;
            }
        }

        if (!buffer.isEmpty()) {
            saveToDb(buffer);
        }
        log.info("전체 수집 완료!");
    }

    /**
     * 개별 트랜잭션으로 분리하여 즉시 DB 반영
     */
    @Transactional // 저장할 때만 짧게 트랜잭션을 겁니다.
    public void saveToDb(List<JSONObject> list) {
        String sql = "MERGE INTO CHARGER c USING DUAL ON (c.STAT_ID = ? AND c.CHARGER_ID = ?) " +
                "WHEN MATCHED THEN UPDATE SET " +
                "c.CHARGER_TYPE = ?, c.STAT = ?, c.STAT_UPD_DT = ?, c.OUTPUT = ?, c.METHOD = ?, " +
                "c.LAST_TSDT = ?, c.LAST_TEDT = ?, c.NOW_TSDT = ?, c.UPDATED_AT = SYSDATE " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(STAT_ID, CHARGER_ID, CHARGER_TYPE, STAT, STAT_UPD_DT, OUTPUT, METHOD, LAST_TSDT, LAST_TEDT, NOW_TSDT, CREATED_AT, UPDATED_AT) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)"; // CREATED_AT 등 추가

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                JSONObject item = list.get(i);
                String sid = item.optString("statId").trim().toUpperCase();
                String cid = item.optString("chgerId").trim().toUpperCase();

                ps.setString(1, sid);
                ps.setString(2, cid);
                ps.setString(3, item.optString("chgerType"));
                ps.setString(4, item.optString("stat", "9"));
                ps.setString(5, item.optString("statUpdDt"));
                ps.setInt(6, item.optInt("output", 0));
                ps.setString(7, item.optString("method"));
                ps.setString(8, item.optString("lastTsdt", ""));
                ps.setString(9, item.optString("lastTedt", ""));
                ps.setString(10, item.optString("nowTsdt", ""));

                ps.setString(11, sid);
                ps.setString(12, cid);
                ps.setString(13, item.optString("chgerType"));
                ps.setString(14, item.optString("stat", "9"));
                ps.setString(15, item.optString("statUpdDt"));
                ps.setInt(16, item.optInt("output", 0));
                ps.setString(17, item.optString("method"));
                ps.setString(18, item.optString("lastTsdt", ""));
                ps.setString(19, item.optString("lastTedt", ""));
                ps.setString(20, item.optString("nowTsdt", ""));
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }
}