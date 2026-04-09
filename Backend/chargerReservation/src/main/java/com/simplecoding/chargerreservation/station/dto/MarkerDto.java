package com.simplecoding.chargerreservation.station.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkerDto {
    private String statId;          // 충전소 ID
    private String statNm;          // ✅ 충전소명 (유지)
    private double lat;             // 위도
    private double lng;             // 경도

    private String occupancy;        // ✅ [3단계] 하단 텍스트 (예: "3 / 4 (고장 1)")
    private String markerColor;      // ✅ [1단계] 서버에서 결정한 색상 (green, amber, red, gray, black)
    private String warningLevel;     // ✅ [2단계] 고장 상태 (NONE, PARTIAL, TOTAL)

    private double availabilityRate; // 가용 비율 (필요 시 내부 계산용)

    /**
     * 마커 색상을 반환하는 로직
     * getMarkerColor()를 호출할 때 필드에 세팅된 색상을 최우선으로 보장합니다.
     */
    public String getMarkerColor() {
        // 1순위: 전체 고장(TOTAL) 상태라면 무조건 검정색(black)
        if ("TOTAL".equals(this.warningLevel)) {
            return "black";
        }

        // 2순위: 서버(Service/Dto)에서 계산되어 저장된 markerColor를 그대로 사용
        // StationDto의 setStatusInfo에서 계산된 결과가 여기 담깁니다.
        if (this.markerColor != null && !this.markerColor.isEmpty()) {
            return this.markerColor;
        }

        // 3순위: 백업 로직 (예외 상황 대비)
        if (availabilityRate >= 70) return "green";
        else if (availabilityRate >= 30) return "amber";
        else if (availabilityRate > 0) return "red";

        return "gray";
    }
}