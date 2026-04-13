package com.simplecoding.chargerreservation.station.repository;

import com.simplecoding.chargerreservation.station.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, String> {

    /**
     * 1. [마커 전용 조회] DB에서 혼잡도와 거리를 미리 계산해서 가져옴 (Interface Projection 활용)
     * - 반환 타입이 MarkerProjection이므로 계산된 값이 유실되지 않음
     */

    @Query(value = "SELECT " +
            "    s.STAT_ID AS statId, " +
            "    s.STAT_NM AS statNm, " +
            "    s.LAT AS lat, " +
            "    s.LNG AS lng, " +
            "    ROUND(6371 * acos(LEAST(1, GREATEST(-1, " +
            "        sin(TRUNC(:lat, 8) * (acos(-1)/180)) * sin(TRUNC(s.LAT, 8) * (acos(-1)/180)) + " +
            "        cos(TRUNC(:lat, 8) * (acos(-1)/180)) * cos(TRUNC(s.LAT, 8) * (acos(-1)/180)) * " +
            "        cos((TRUNC(s.LNG, 8) * (acos(-1)/180)) - (TRUNC(:lng, 8) * (acos(-1)/180))) " +
            "    ))), 2) AS distance, " +
            "    (SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID AND c.STAT = '2') AS availableCount, " +
            "    (SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID) AS totalCount, " +
            "    (SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID AND c.STAT IN ('1', '4', '5')) AS brokenCount " +
            "FROM STATION s " +
            "WHERE ROUND(6371 * acos(LEAST(1, GREATEST(-1, " +
            "        sin(TRUNC(:lat, 8) * (acos(-1)/180)) * sin(TRUNC(s.LAT, 8) * (acos(-1)/180)) + " +
            "        cos(TRUNC(:lat, 8) * (acos(-1)/180)) * cos(TRUNC(s.LAT, 8) * (acos(-1)/180)) * " +
            "        cos((TRUNC(s.LNG, 8) * (acos(-1)/180)) - (TRUNC(:lng, 8) * (acos(-1)/180))) " +
            "    ))), 2) <= :radius " +
            "ORDER BY distance ASC " +
            "FETCH NEXT 100 ROWS ONLY",
            nativeQuery = true)
    List<MarkerProjection> findMarkersWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius
    );
    /**
     * 2. [목록 전용 조회] 페이징 + 요금 정보 포함
     * - 운영사(BNM)가 일치하고, 현재 연도/계절/타입에 맞는 요금을 가져옵니다.
     */
    @Query(value = "SELECT t.* FROM (" +
            "    SELECT s.STAT_ID as statId, s.STAT_NM as statNm, s.ADDR as addr, " +
            "           s.BNM as bnm, s.LAT as lat, s.LNG as lng, s.USE_TIME as useTime, " +
            "           s.PARKING_FREE as parkingFree, s.LIMIT_YN as limitYn, s.LIMIT_DETAIL as limitDetail, " +
            "           p1.UNIT_PRICE as currentPrice, " + // ✨ 급속 요금
            "           p2.UNIT_PRICE as slowPrice, " +    // ✨ 완속 요금 추가
            "           ROUND(6371 * acos(LEAST(1, GREATEST(-1, " +
            "               sin(:lat * 3.141592653589793 / 180) * sin(s.LAT * 3.141592653589793 / 180) + " +
            "               cos(:lat * 3.141592653589793 / 180) * cos(s.LAT * 3.141592653589793 / 180) * " +
            "               cos((s.LNG - :lng) * 3.141592653589793 / 180) " +
            "           ))), 2) AS distance " +
            "    FROM STATION s " +
            "    LEFT JOIN CHARGER_PRICE p1 ON s.BNM = p1.BNM " +
            "        AND p1.SPEED_TYPE = '급속' " + // 급속 고정
            "        AND p1.APPLY_YEAR = :year AND p1.SEASON = :season " +
            "    LEFT JOIN CHARGER_PRICE p2 ON s.BNM = p2.BNM " +
            "        AND p2.SPEED_TYPE = '완속' " + // 완속 고정
            "        AND p2.APPLY_YEAR = :year AND p2.SEASON = :season " +
            "    WHERE s.LAT IS NOT NULL AND s.LNG IS NOT NULL " +
            ") t " +
            "WHERE t.distance <= :radius " +
            "ORDER BY t.distance ASC " +
            "OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY", nativeQuery = true)
    List<MarkerProjection> findStationsWithinRadiusWithPaging(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("year") Integer year,
            @Param("season") String season,
            @Param("offset") int offset,
            @Param("size") int size);
    /**
     * 3. 통합 키워드 검색
     */
    @Query("SELECT s FROM StationEntity s " +
            "WHERE s.statNm LIKE %:keyword% " +
            "OR s.addr LIKE %:keyword% " +
            "OR s.bnm LIKE %:keyword% " +
            "ORDER BY s.statNm ASC")
    List<StationEntity> findByIntegratedSearch(@Param("keyword") String keyword);

    /**
     * 3. [상세 조회용] 특정 충전소 정보 + 2개년 요금 히스토리 조인
     * - 규칙 3: 올해와 작년 요금을 한 번에 가져와서 비교할 수 있게 함
     */
    @Query(value = "SELECT " +
            "s.STAT_NM, s.ADDR, s.BNM, s.LOCATION, s.USE_TIME, " + // 0, 1, 2, 3, 4
            "s.LIMIT_YN, s.PARKING_FREE, s.LIMIT_DETAIL, s.UPDATED_AT, " + // 5, 6, 7, 8
            "ROUND(6371 * acos(LEAST(1, GREATEST(-1, " + // 9 (거리)
            "    sin(:lat * 3.141592653589793 / 180) * sin(s.LAT * 3.141592653589793 / 180) + " +
            "    cos(:lat * 3.141592653589793 / 180) * cos(s.LAT * 3.141592653589793 / 180) * " +
            "    cos((s.LNG - :lng) * 3.141592653589793 / 180) " +
            "))), 2) AS distance, " +
            "(SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID) AS total, " + // 10
            "(SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID AND c.STAT = '2') AS available, " + // 11
            "(SELECT COUNT(*) FROM CHARGER c WHERE c.STAT_ID = s.STAT_ID AND c.STAT IN ('4', '5')) AS broken, " + // 12
            "p.UNIT_PRICE, p.APPLY_YEAR, p.SPEED_TYPE " + // 💡 13, 14, 15(타입 추가)
            "FROM STATION s " +
            "LEFT JOIN CHARGER_PRICE p ON TRIM(s.BNM) = TRIM(p.BNM) " +
            "AND p.SPEED_TYPE IN ('급속', '완속') " + // 💡 특정 타입이 아닌 둘 다 조회
            "AND p.SEASON = :season " +
            "AND (p.APPLY_YEAR = :currYear OR p.APPLY_YEAR = :lastYear) " +
            "WHERE s.STAT_ID = :statId " +
            "ORDER BY p.APPLY_YEAR DESC", nativeQuery = true)
    List<Object[]> findStationDetailWithPriceHistory(
            @Param("statId") String statId,
            @Param("season") String season, // 💡 :type 파라미터는 이제 필요 없음
            @Param("currYear") Integer currYear,
            @Param("lastYear") Integer lastYear,
            @Param("lat") double lat,
            @Param("lng") double lng
    );

    // 나머지 메서드들 (동일)
    Optional<StationEntity> findByStatId(String statId);
    List<StationEntity> findByZcode(String zcode);
    List<StationEntity> findByBnmContaining(String bnm);
    Optional<StationEntity> findByLatAndLng(Double lat, Double lng);
}