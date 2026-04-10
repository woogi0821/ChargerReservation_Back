package com.simplecoding.chargerreservation.station.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.simplecoding.chargerreservation.charger.dto.ChargerDto;
import com.simplecoding.chargerreservation.chargerPrice.dto.ChargerPriceDto;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StationDto {

    // 1. [기본 정보] - 명칭, 주소, 위치 관련 (상세 항목 1, 2, 9, 10, 11)
    private String statId;
    private String statNm;          // 1. 충전소명
    private String addr;            // 2. 주소
    private String bnm;             // 9. 기관명
    private String location;        // 10. 상세위치
    private String useTime;         // 11. 이용가능 시간
    private Double lat;
    private Double lng;
    private Double distance;        // 3. 거리

    // 2. [상태 및 마커 정보] - 실시간 현황 (상세 항목 5, 12)
    private Integer availableCount;
    private Integer totalCount;
    private Integer brokenCount;
    private String occupancy;       // 5. 현황 (예: 2/4 (고장2))
    private String lastUpdated;     // 12. 업데이트 날짜
    private String statSummary;     // 요약 텍스트
    private String markerColor;     // 마커 색상
    private String warningLevel;    // 경고 수준
    private String fastChargerStatus;
    private String slowChargerStatus;

    // 3. [제약 및 주차 정보] - 이용 조건 (상세 항목 6, 7, 8)
    private String limitYn;         // 6. 주차가능유무 (이용자제한 여부)
    private String parkingFree;     // 7. 주차요금 (무료/유료 여부)
    private String limitDetail;     // 8. 이용자제한 상세내용
    private String parkingInfo;     // 변환용 ("무료주차" 등)
    private String openStatus;      // 변환용 ("개방" 등)

    // 4. [요금 정보] - 가격 비교 및 계절 (상세 항목 4)
    private Double currentPrice;    // 현재 요금
    private Double lastYearPrice;   // 작년 요금
    private Double priceDiff;       // 요금 차이
    private String season;          // 현재 계절 (봄/가을, 여름, 겨울)
    private List<ChargerDto> chargers;
    private ChargerPriceDto priceDetail;

    /**
     * 상태 및 마커 정보 세팅 (계산 로직만 유지)
     */
    public void setStatusInfo(int available, int total, int broken) {
        this.availableCount = Math.max(0, (available + broken > total) ? total - broken : available);
        this.totalCount = total;
        this.brokenCount = broken;

        int activeTotal = Math.max(0, total - broken);
        if (total > 0 && (total == broken || activeTotal == 0)) {
            this.markerColor = "black";
            this.warningLevel = "TOTAL";
            this.statSummary = "점검 중";
        } else if (total > 0) {
            this.warningLevel = (broken > 0) ? "PARTIAL" : "NONE";
            double rate = (activeTotal > 0) ? ((double) this.availableCount / activeTotal) * 100 : 0;

            if (this.availableCount == 0) this.markerColor = "gray";
            else if (rate >= 70) this.markerColor = "green";
            else if (rate >= 30) this.markerColor = "amber";
            else this.markerColor = "red";

            this.statSummary = (broken > 0)
                    ? String.format("%d/%d (고장%d)", this.availableCount, total, broken)
                    : String.format("%d/%d", this.availableCount, total);
        } else {
            this.markerColor = "gray";
            this.statSummary = "확인불가";
            this.warningLevel = "NONE";
        }

        // ⭐ [추가] 테스트 코드에서 검증하는 occupancy 필드에 값을 채워줍니다.
        this.occupancy = this.statSummary;
    }

    public void setTypeDetailStatus(String type, int available, int total, int broken) {
        String statusText = (broken > 0) ? String.format("%s %d/%d (고장%d)", type, available, total, broken) : String.format("%s %d/%d", type, available, total);
        if ("급속".equals(type)) this.fastChargerStatus = statusText;
        else if ("완속".equals(type)) this.slowChargerStatus = statusText;
    }

    /**
     * 프론트엔드에서 호출할 요금 표시용 메서드
     */
    public String getPriceDisplayText() {
        if (this.currentPrice == null || this.currentPrice <= 0) {
            return "현장확인";
        }
        return String.format("%.1f원/kWh", this.currentPrice);
    }

    /**
     * [추가] 요금 비교 및 계절 정보 세팅 로직
     * 서비스 단에서 호출하여 데이터를 완성합니다.
     */
    public void setPriceComparison(Double current, Double lastYear, String currentMonth) {
        this.currentPrice = current;
        this.lastYearPrice = lastYear;
        if (current != null && lastYear != null) {
            this.priceDiff = current - lastYear;
        }

        // 계절 판별 (간단 로직)
        int month = Integer.parseInt(currentMonth);
        if (month >= 3 && month <= 5 || month >= 9 && month <= 11) this.season = "봄/가을";
        else if (month >= 6 && month <= 8) this.season = "여름";
        else this.season = "겨울";
    }


}