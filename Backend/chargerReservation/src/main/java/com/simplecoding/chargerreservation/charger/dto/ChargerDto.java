package com.simplecoding.chargerreservation.charger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.station.entity.StationEntity;
import lombok.*;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargerDto {

    // 1. 메모리 최적화: 급속 타입 코드를 상수로 선언 (객체 재생성 방지)
    private static final Set<String> FAST_TYPES = Set.of("01", "03", "04", "05", "06", "08");

    // 2. 편의성: 충전기 상태 코드를 한글 텍스트로 매핑
    private static final Map<String, String> STAT_LABELS = Map.of(
            "1", "통신이상", "2", "충전대기", "3", "충전중",
            "4", "운영중단", "5", "점검중", "9", "상태미확인"
    );

    @JsonProperty("statId")
    private String statId;

    @JsonProperty("chgerId")
    private String chargerId;

    @JsonProperty("chgerType")
    private String chargerType;

    @JsonProperty("stat")
    private String stat;

    private String statNm;        // "충전중", "대기중" 등 한글명 추가

    @JsonProperty("statUpdDt")
    private String statUpdDt;

    @JsonProperty("output")
    private Integer output;

    @JsonProperty("method")
    private String method;

    @JsonProperty("lastTsdt")
    private String lastTsdt;

    @JsonProperty("lastTedt")
    private String lastTedt;

    @JsonProperty("nowTsdt")
    private String nowTsdt;

    private String createdAt;
    private String updatedAt;

    private String chargerTypeNm;
    private boolean isFast;

    private String chargerName;

    private String address;


    /**
     * Entity -> DTO 변환
     */
    public static ChargerDto fromEntity(ChargerEntity entity, StationEntity station) {
        if (entity == null) return null;

        String typeCode = entity.getChargerType();
        // 미리 정의된 상수를 사용하여 메모리 사용량 절감
        boolean fast = FAST_TYPES.contains(typeCode);

        return ChargerDto.builder()
                .statId(entity.getStatId())
                .chargerId(entity.getChargerId())
                .chargerType(typeCode)
                .stat(entity.getStat())
                // 상태 코드를 한글명으로 변환해서 저장
                .statNm(STAT_LABELS.getOrDefault(entity.getStat(), "정보없음"))
                .statUpdDt(entity.getStatUpdDt())
                .output(entity.getOutput())
                .method(entity.getMethod())
                .lastTsdt(entity.getLastTsdt())
                .lastTedt(entity.getLastTedt())
                .nowTsdt(entity.getNowTsdt())
                .isFast(fast)
                .chargerTypeNm(fast ? "급속" : "완속")
                .build();
    }

    public static ChargerDto fromEntityWithStation(ChargerEntity entity, StationEntity station) {
        if (entity == null) return null;
        String typeCode = entity.getChargerType();
        boolean fast = FAST_TYPES.contains(typeCode);

        return ChargerDto.builder()
                .statId(entity.getStatId())
                .chargerId(entity.getChargerId())
                .chargerType(typeCode)
                .stat(entity.getStat())
                .statNm(STAT_LABELS.getOrDefault(entity.getStat(), "정보없음"))
                .isFast(fast)
                .chargerTypeNm(fast ? "급속" : "완속")
                .chargerName(station.getStatNm() + " · " + (fast ? "급속 충전기" : "완속 충전기"))
                .address(station.getAddr())
                .build();
    }
}