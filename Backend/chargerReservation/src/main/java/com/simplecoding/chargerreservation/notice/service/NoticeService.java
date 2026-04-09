package com.simplecoding.chargerreservation.notice.service;

import com.simplecoding.chargerreservation.common.MapStruct;
import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.notice.repository.NoticeMapper;
import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MapStruct mapStruct;
    private final NoticeMapper noticeMapper;


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

    /**
     * 공지사항 수정
     * @param noticeId 수정할 글 번호
     * @param requestDto 수정할 내용이 담긴 DTO
     */
    @Transactional
    public NoticeResponseDto updateNotice(Long noticeId, NoticeRequestDto requestDto) {
        // 1. 기존 데이터 조회
        NoticeEntity entity = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 2. 데이터 업데이트 (Dirty Checking)
        entity.setTitle(requestDto.getTitle());
        entity.setContent(requestDto.getContent());
        entity.setFixYn(requestDto.getFixYn());

        // 3. MapStruct를 사용하여 Entity -> ResponseDto 변환
        return noticeMapper.toResponseDto(entity);
    }
}