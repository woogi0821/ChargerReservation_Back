package com.simplecoding.chargerreservation.common;

import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.entity.StationEntity;
import com.simplecoding.chargerreservation.station.repository.MarkerProjection;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MapStruct {

    // --- [1] Station: @Named로 특정 메서드만 사용하도록 강제 ---
    @Mapping(target = "parkingInfo", source = "parkingFree", qualifiedByName = "toParkingName")
    @Mapping(target = "openStatus", source = "entity", qualifiedByName = "toOpenStatusName")
    StationDto toDto(StationEntity entity);

    @Mapping(target = "parkingInfo", source = "parkingFree", qualifiedByName = "toParkingName")
    @Mapping(target = "openStatus", source = "p", qualifiedByName = "toOpenStatusNameProj")
    StationDto toDto(MarkerProjection p);

    // --- [2] Notice: source를 직접 지정하여 자동 매핑(오염) 방지 ---
    @Mapping(target = "noticeId", ignore = true)
    @Mapping(target = "writerId", ignore = true)
    @Mapping(target = "deleteYn", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "content", source = "dto.content")
// fixYn이 Dto에 없다면 기본값 'N'을 보장하도록 설정하는 것이 좋습니다.
    @Mapping(target = "fixYn", source = "dto.fixYn", defaultValue = "N")
    NoticeEntity toEntity(NoticeRequestDto dto);

    // 2. Entity -> ResponseDto 변환 (가장 중요한 부분)
    @Mapping(target = "title", source = "entity.title")
    @Mapping(target = "content", source = "entity.content")
// ✅ 수정: insertTime이 null일 경우를 대비해 삼항 연산자(null 체크) 추가
    @Mapping(target = "formattedDate",
            expression = "java(entity.getInsertTime() != null ? entity.getInsertTime().format(java.time.format.DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm\")) : \"\")")
// ✅ 수정: 기준을 1일로 잡으셨는데, 보통 공지는 7일 정도로 넉넉하게 잡거나 null 체크를 추가합니다.
    @Mapping(target = "isNew",
            expression = "java(entity.getInsertTime() != null ? entity.getInsertTime().isAfter(java.time.LocalDateTime.now().minusDays(1)) : false)")
    NoticeResponseDto toResponseDto(NoticeEntity entity);

    // --- [3] 변환 로직: @Named로 이름표를 붙여서 자동 호출을 막음 ---
    @Named("toParkingName")
    default String toParkingName(String parkingFree) {
        if ("Y".equals(parkingFree)) return "무료주차";
        if ("N".equals(parkingFree)) return "유료주차";
        return "정보없음";
    }

    @Named("toOpenStatusName")
    default String toOpenStatusName(StationEntity entity) {
        if ("Y".equals(entity.getLimitYn())) {
            return (entity.getLimitDetail() != null && !entity.getLimitDetail().isEmpty())
                    ? "미개방(" + entity.getLimitDetail() + ")" : "미개방";
        }
        return "개방";
    }

    @Named("toOpenStatusNameProj")
    default String toOpenStatusNameProj(MarkerProjection p) {
        if ("Y".equals(p.getLimitYn())) {
            return (p.getLimitDetail() != null && !p.getLimitDetail().isEmpty())
                    ? "미개방(" + p.getLimitDetail() + ")" : "미개방";
        }
        return "개방";
    }

    // 기존의 다른 매핑들 (toEntity, updateFromDto 등)은 이 아래에 그대로 두시면 됩니다.
}