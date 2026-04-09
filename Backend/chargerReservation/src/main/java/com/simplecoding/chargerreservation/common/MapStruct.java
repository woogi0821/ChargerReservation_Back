package com.simplecoding.chargerreservation.common;

import com.simplecoding.chargerreservation.charger.dto.ChargerDto;
import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.chargerPrice.dto.ChargerPriceDto;
import com.simplecoding.chargerreservation.chargerPrice.entity.ChargerPriceEntity;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.entity.StationEntity;
import com.simplecoding.chargerreservation.station.repository.MarkerProjection;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MapStruct {

    // --------------------------------------------------
    // 1) 충전소(Station) 변환
    // --------------------------------------------------
    @Mapping(target = "parkingInfo", expression = "java(convertParking(entity.getParkingFree()))")
    @Mapping(target = "openStatus", expression = "java(convertOpenStatus(entity.getLimitYn(), entity.getLimitDetail()))")
    @Mapping(target = "chargers", ignore = true)
    @Mapping(target = "currentPrice", ignore = true) // 서비스에서 수동으로 넣을 거라 무시
    @Mapping(target = "priceDetail", ignore = true)  // 상세조회 시 채울 거라 무시
    StationDto toDto(StationEntity entity);

    @Mapping(target = "parkingInfo", expression = "java(convertParking(p.getParkingFree()))")
    @Mapping(target = "openStatus", expression = "java(convertOpenStatus(p.getLimitYn(), p.getLimitDetail()))")
    @Mapping(target = "chargers", ignore = true)
    @Mapping(target = "currentPrice", source = "currentPrice") // ✨ 프로젝션의 값을 DTO로 바로 매핑
    @Mapping(target = "priceDetail", ignore = true)
    StationDto toDto(MarkerProjection p);

    StationEntity toEntity(StationDto stationDto);

    void updateFromDto(StationDto dto, @MappingTarget StationEntity entity);

    // --------------------------------------------------
    // 2) 충전기(Charger) 변환
    // --------------------------------------------------
    ChargerDto toDto(ChargerEntity chargerEntity);
    ChargerEntity toEntity(ChargerDto chargerDto);
    void updateFromDto(ChargerDto dto, @MappingTarget ChargerEntity entity);

    // --------------------------------------------------
    // 3) 요금(ChargerPrice) 변환
    // --------------------------------------------------
    ChargerPriceDto toDto(ChargerPriceEntity entity);

    @Mapping(target = "updateDt", ignore = true)
    ChargerPriceEntity toEntity(ChargerPriceDto dto);

    // --------------------------------------------------
    // 4) 공통 변환 로직 (Default Methods)
    // --------------------------------------------------
    default String convertParking(String parkingFree) {
        if ("Y".equals(parkingFree)) return "무료주차";
        if ("N".equals(parkingFree)) return "유료주차";
        return "정보없음";
    }

    default String convertOpenStatus(String limitYn, String limitDetail) {
        if ("Y".equals(limitYn)) {
            return (limitDetail != null && !limitDetail.isEmpty()) ? "미개방(" + limitDetail + ")" : "미개방";
        }
        return "개방";
    }
}
