package com.simplecoding.chargerreservation.charger.repository;

import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.charger.entity.ChargerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerRepository extends JpaRepository<ChargerEntity, ChargerId> {

    List<ChargerEntity> findByStatId(String statId);

    List<ChargerEntity> findByStatIdIn(List<String> statIds);

    // ✅ 기존 — 대시보드 고장 충전기 카운트용
    long countByStatIn(List<String> stats);

    // ✅ 추가 — 메인페이지 통계용 (가용 충전기 카운트)
    long countByStat(String stat);

    @Modifying
    @Query("UPDATE ChargerEntity c SET " +
            "c.stat = :stat, " +
            "c.statUpdDt = :statUpdDt, " +
            "c.lastTsdt = :lastTsdt, " +
            "c.lastTedt = :lastTedt, " +
            "c.nowTsdt = :nowTsdt, " +
            "c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.statId = :statId AND c.chargerId = :chargerId")
    int updateChargerStatus(
            @Param("statId") String statId,
            @Param("chargerId") String chargerId,
            @Param("stat") String stat,
            @Param("statUpdDt") String statUpdDt,
            @Param("lastTsdt") String lastTsdt,
            @Param("lastTedt") String lastTedt,
            @Param("nowTsdt") String nowTsdt
    );

    @Modifying
    @Query(value = "MERGE INTO CHARGER c " +
            "USING DUAL ON (c.STAT_ID = :statId AND c.CHGER_ID = :chargerId) " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET c.STAT = :stat, c.STAT_UPD_DT = :statUpdDt, c.UPDATED_AT = CURRENT_TIMESTAMP " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (STAT_ID, CHGER_ID, STAT, STAT_UPD_DT, CREATED_AT) " +
            "  VALUES (:statId, :chargerId, :stat, :statUpdDt, CURRENT_TIMESTAMP)", nativeQuery = true)
    void mergeChargerStatus(
            @Param("statId") String statId,
            @Param("chargerId") String chargerId,
            @Param("stat") String stat,
            @Param("statUpdDt") String statUpdDt
    );

    @Query("""
    SELECT c FROM ChargerEntity c JOIN FETCH c.station WHERE c.statId = :statId
""")
    List<ChargerEntity> findByStatIdWithStation(@Param("statId") String statId);
}