package com.simplecoding.chargerreservation.charger.entity;

import com.simplecoding.chargerreservation.station.entity.StationEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHARGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ChargerId.class)
@DynamicUpdate
public class ChargerEntity {

    @Id
    @Column(name = "STAT_ID", length = 40)
    private String statId;

    @Id
    @Column(name = "CHARGER_ID", length = 20)
    private String chargerId;

    @Column(name = "CHARGER_TYPE", length = 20)
    private String chargerType;

    @Column(name = "STAT", length = 2)
    private String stat;

    @Column(name = "STAT_UPD_DT", length = 20)
    private String statUpdDt;

    @Column(name = "OUTPUT", precision = 10)
    private Integer output;

    @Column(name = "METHOD", length = 100)
    private String method;

    // --- 새로 추가된 컬럼 시작 ---

    @Column(name = "LAST_TSDT", length = 14)
    private String lastTsdt; // 마지막 충전 시작일시

    @Column(name = "LAST_TEDT", length = 14)
    private String lastTedt; // 마지막 충전 종료일시

    @Column(name = "NOW_TSDT", length = 14)
    private String nowTsdt;  // 현재 충전 시작일시

    // --- 새로 추가된 컬럼 끝 ---

    @Column(name = "CREATED_AT", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STAT_ID", insertable = false, updatable = false)
    private StationEntity station;
}