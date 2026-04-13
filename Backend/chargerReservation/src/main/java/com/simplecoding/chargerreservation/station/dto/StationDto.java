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

    // 1. [기본 정보]
    private String statId;
    private String statNm;          // 충전소명
    private String addr;            // 주소
    private String bnm;             // 기관명
    private String location;        // 상세위치
    private String useTime;         // 이용가능 시간
    private Double lat;
    private Double lng;
    private Double distance;        // 거리

    // 2. [상태 및 마커 정보]
    private Integer availableCount;
    private Integer totalCount;
    private Integer brokenCount;
    private String occupancy;
    private String lastUpdated;
    private String statSummary;
    private String markerColor;
    private String warningLevel;
    private String fastChargerStatus; // 급속 현황 텍스트
    private String slowChargerStatus; // 완속 현황 텍스트

    // 3. [제약 및 주차 정보]
    private String limitYn;
    private String parkingFree;
    private String limitDetail;
    private String parkingInfo;
    private String openStatus;

    // 4. [요금 정보]
    // 급속 관련 (기존 필드 유지)
    private Double currentPrice;    // 급속 현재 요금
    private Double lastYearPrice;   // 급속 작년 요금
    private Double priceDiff;       // 급속 요금 차이

    // 완속 관련 (추가된 필드)
    private Double slowPrice;          // 완속 현재 요금
    private Double slowLastYearPrice;  // 완속 작년 요금
    private Double slowPriceDiff;      // 완속 요금 차이

    private String season;
    private List<ChargerDto> chargers;
    private ChargerPriceDto priceDetail;

    /**
     * 상태 및 마커 정보 세팅
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
        this.occupancy = this.statSummary;
    }

    /**
     * 급속/완속 개별 상태 세팅
     */
    public void setTypeDetailStatus(String type, int available, int total, int broken) {
        String statusText = (broken > 0)
                ? String.format("%s %d/%d (고장%d)", type, available, total, broken)
                : String.format("%s %d/%d", type, available, total);

        if ("급속".equals(type)) this.fastChargerStatus = statusText;
        else if ("완속".equals(type)) this.slowChargerStatus = statusText;
    }

    /**
     * [수정] 요금 비교 및 계절 정보 세팅 로직
     * 급속과 완속 데이터를 모두 받아 각각의 차이를 저장합니다.
     */
    public void setPriceComparison(Double fastCurr, Double fastLast, Double slowCurr, Double slowLast, String currentMonth) {
        // 1. 급속 세팅
        this.currentPrice = fastCurr;
        this.lastYearPrice = fastLast;
        if (fastCurr != null && fastLast != null) {
            this.priceDiff = fastCurr - fastLast;
        }

        // 2. 완속 세팅
        this.slowPrice = slowCurr;
        this.slowLastYearPrice = slowLast;
        if (slowCurr != null && slowLast != null) {
            this.slowPriceDiff = slowCurr - slowLast;
        }

        // 3. 계절 판별
        try {
            int month = Integer.parseInt(currentMonth);
            if ((month >= 3 && month <= 5) || (month >= 9 && month <= 11)) this.season = "봄/가을";
            else if (month >= 6 && month <= 8) this.season = "여름";
            else this.season = "겨울";
        } catch (Exception e) {
            this.season = "정보없음";
        }
    }

    public String getPriceDisplayText() {
        if (this.currentPrice == null || this.currentPrice <= 0) {
            return "현장확인";
        }
        return String.format("%.1f원/kWh", this.currentPrice);
    }
}