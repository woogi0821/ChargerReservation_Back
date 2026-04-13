package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 어드민 페이지에서 충전기 목록 조회 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminChargerDto {

    private String statId;
    private String chargerId;
    private String chargerType;
    private String stat;
    private Integer output;
    private String method;
    private String statUpdDt;

    // 충전기 상태값 한글 변환
    // 1 → 통신이상 / 2 → 충전가능 / 3 → 충전중 / 4 → 운영중지 / 5 → 점검중
    public String getStatLabel() {
        return switch (stat) {
            case "1" -> "통신이상";
            case "2" -> "충전가능";
            case "3" -> "충전중";
            case "4" -> "운영중지";
            case "5" -> "점검중";
            default  -> "알 수 없음";
        };
    }

    // ChargerEntity → AdminChargerDto 변환
    public static AdminChargerDto from(ChargerEntity charger) {
        return new AdminChargerDto(
                charger.getStatId(),
                charger.getChargerId(),
                charger.getChargerType(),
                charger.getStat(),
                charger.getOutput(),
                charger.getMethod(),
                charger.getStatUpdDt()
        );
    }
}