package com.simplecoding.chargerreservation.charger.repository;

import com.simplecoding.chargerreservation.charger.dto.ChargerDto;
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

    // 기존 것(상세보기용)도 두고, 목록용을 하나 더 추가하는 겁니다.
    List<ChargerEntity> findByStatId(String statId); // 단일 조회용 (상세페이지)

    List<ChargerEntity> findByStatIdIn(List<String> statIds); // 대량 조회용 (목록페이지 - 성능 최적화)

    /**
     * 2. 상태 업데이트 (Bulk Update)
     * Repository 레벨의 @Transactional은 제거했습니다. (서비스에서 묶어 처리 권장)
     */
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

    /**
     * 3. [추가 추천] 대량 수집용 Native MERGE 쿼리 (Oracle 전용)
     * JPA saveAll보다 훨씬 빠릅니다. (필요 시 활용하세요)
     */
    @Modifying
    @Query(value = "MERGE INTO CHARGER c " +
            "USING DUAL ON (c.STAT_ID = :statId AND c.CHGER_ID = :chargerId) " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET c.STAT = :stat, c.STAT_UPD_DT = :statUpdDt, c.UPDATED_AT = CURRENT_TIMESTAMP " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (STAT_ID, CHGER_ID, STAT, STAT_UPD_DT, CREATED_AT) " +
            "  VALUES (:statId, :chargerId, :stat, :statUpdDt, CURRENT_TIMESTAMP)", nativeQuery = true)
    void mergeChargerStatus(@Param("statId") String statId, @Param("chargerId") String chargerId,
                            @Param("stat") String stat, @Param("statUpdDt") String statUpdDt);

    @Query("""
    SELECT c FROM ChargerEntity c JOIN FETCH c.station WHERE c.statId = :statId
""")
    List<ChargerEntity> findByStatIdWithStation(@Param("statId") String statId);
}