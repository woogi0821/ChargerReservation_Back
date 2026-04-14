package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.station.entity.StationEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 어드민 페이지에서 충전소 목록 조회 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminStationDto {

    private String statId;
    private String statNm;
    private String addr;
    private String location;
    private String useTime;
    private String bnm;
    private String parkingFree;
    private String limitYn;
    private String limitDetail;

    // StationEntity → AdminStationDto 변환
    public static AdminStationDto from(StationEntity station) {
        return new AdminStationDto(
                station.getStatId(),
                station.getStatNm(),
                station.getAddr(),
                // "null" 문자열로 저장된 경우 null 로 변환
                "null".equals(station.getLocation()) ? null : station.getLocation(),
                station.getUseTime(),
                station.getBnm(),
                station.getParkingFree(),
                station.getLimitYn(),
                station.getLimitDetail()
        );
    }
}