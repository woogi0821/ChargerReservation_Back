package com.simplecoding.chargerreservation.notice.service;

import com.simplecoding.chargerreservation.common.MapStruct;
import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MapStruct mapStruct;

    /**
     * 공지사항 등록
     * @param requestDto : 관리자가 입력한 제목, 내용, 고정여부
     * @param adminId    : 현재 로그인한 관리자 ID (세션이나 토큰에서 가져옴)
     */
    @Transactional
    public NoticeResponseDto registerNotice(NoticeRequestDto requestDto, String adminId) {
        // 1. DTO -> Entity 변환 (MapStruct 이용)
        NoticeEntity noticeEntity = mapStruct.toEntity(requestDto);

        // 2. 관리자 ID 및 초기값 세팅
        noticeEntity.setWriterId(adminId);
        noticeEntity.setDeleteYn("N"); // 등록 시 기본값 'N'

        // 3. DB 저장 (리포지토리의 save 호출)
        NoticeEntity savedNotice = noticeRepository.save(noticeEntity);

        // 4. 저장된 결과를 다시 DTO로 변환하여 반환
        return mapStruct.toResponseDto(savedNotice);
    }
}