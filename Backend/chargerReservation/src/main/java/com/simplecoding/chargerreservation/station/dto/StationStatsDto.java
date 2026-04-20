package com.simplecoding.chargerreservation.station.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StationStatsDto {
    private long totalStations;
    private long totalChargers;
    private long availableChargers;
    private long chargingCount;   // ✅ 추가 — 충전 중 수
    private long reservedCount;   // ✅ 추가 — 예약 중 수
}