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
            "           p.UNIT_PRICE as currentPrice, " + // ✨ 요금 단가 추가
            "           ROUND(6371 * acos(LEAST(1, GREATEST(-1, " +
            "               sin(:lat * 3.141592653589793 / 180) * sin(s.LAT * 3.141592653589793 / 180) + " +
            "               cos(:lat * 3.141592653589793 / 180) * cos(s.LAT * 3.141592653589793 / 180) * " +
            "               cos((s.LNG - :lng) * 3.141592653589793 / 180) " +
            "           ))), 2) AS distance " +
            "    FROM STATION s " +
            "    LEFT JOIN CHARGER_PRICE p ON s.BNM = p.BNM " + // ✨ 규칙 1: 운영사 일치 조인
            "    AND p.SPEED_TYPE = :type " +
            "    AND p.APPLY_YEAR = :year " +
            "    AND p.SEASON = :season " +
            "    WHERE s.LAT IS NOT NULL AND s.LNG IS NOT NULL " +
            ") t " +
            "WHERE t.distance <= :radius " +
            "ORDER BY t.distance ASC " +
            "OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY", nativeQuery = true)
    List<MarkerProjection> findStationsWithinRadiusWithPaging(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("type") String type,   // ✨ 추가
            @Param("year") Integer year,  // ✨ 추가
            @Param("season") String season, // ✨ 추가
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
    @Query("SELECT s, p FROM StationEntity s " +
            "LEFT JOIN ChargerPriceEntity p ON s.bnm = p.bnm " +
            "WHERE s.statId = :statId " +
            "AND p.speedType = :type " +
            "AND p.season = :season " +
            "AND (p.applyYear = :currYear OR p.applyYear = :lastYear) " +
            "ORDER BY p.applyYear DESC")
    List<Object[]> findStationDetailWithPriceHistory(
            @Param("statId") String statId,
            @Param("type") String type,
            @Param("season") String season,
            @Param("currYear") Integer currYear,
            @Param("lastYear") Integer lastYear
    );

    // 나머지 메서드들 (동일)
    Optional<StationEntity> findByStatId(String statId);
    List<StationEntity> findByZcode(String zcode);
    List<StationEntity> findByBnmContaining(String bnm);
    Optional<StationEntity> findByLatAndLng(Double lat, Double lng);
}