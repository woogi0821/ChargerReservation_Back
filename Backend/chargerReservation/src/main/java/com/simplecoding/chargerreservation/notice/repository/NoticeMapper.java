package com.simplecoding.chargerreservation.notice.repository;

import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

// componentModel = "spring"을 넣어야 서비스에서 @Autowired(주입)가 가능합니다.
@Mapper(componentModel = "spring")
public interface NoticeMapper {

    // Entity -> ResponseDto 변환 규칙
    NoticeResponseDto toResponseDto(NoticeEntity entity);
}