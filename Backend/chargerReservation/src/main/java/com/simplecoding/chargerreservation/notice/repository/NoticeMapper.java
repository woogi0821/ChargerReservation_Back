package com.simplecoding.chargerreservation.notice.repository;

import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", imports = {java.time.format.DateTimeFormatter.class, java.time.LocalDateTime.class})
public interface NoticeMapper {
    NoticeMapper INSTANCE = Mappers.getMapper(NoticeMapper.class);

    @Mapping(target = "formattedDate", expression = "java(entity.getInsertTime().format(DateTimeFormatter.ofPattern(\"yyyy-MM-dd\")))")
    @Mapping(target = "isNew", expression = "java(entity.getInsertTime().isAfter(LocalDateTime.now().minusDays(7)))")
    NoticeResponseDto toResponseDto(NoticeEntity entity);
}