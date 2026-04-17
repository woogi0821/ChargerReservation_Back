package com.simplecoding.chargerreservation.station.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StationStatsDto {
    private long totalStations;
    private long totalChargers;
    private long availableChargers;
}