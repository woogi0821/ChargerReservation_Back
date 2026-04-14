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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MapStruct mapStruct;
    private final NoticeMapper noticeMapper;

    /**
     * 공지사항 등록
     */
    @Transactional
    public NoticeResponseDto registerNotice(NoticeRequestDto requestDto, String adminId) {
        NoticeEntity noticeEntity = mapStruct.toEntity(requestDto);
        noticeEntity.setWriterId(adminId);
        noticeEntity.setDeleteYn("N");

        NoticeEntity savedNotice = noticeRepository.save(noticeEntity);
        return mapStruct.toResponseDto(savedNotice);
    }

    /**
     * 공지사항 수정
     */
    @Transactional
    public NoticeResponseDto updateNotice(Long noticeId, NoticeRequestDto requestDto) {
        NoticeEntity entity = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항을 찾을 수 없습니다. ID: " + noticeId));

        entity.setTitle(requestDto.getTitle());
        entity.setContent(requestDto.getContent());
        entity.setFixYn(requestDto.getFixYn());

        return noticeMapper.toResponseDto(entity);
    }

    /**
     * 소프트 삭제 (N -> Y)
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        NoticeEntity entity = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 공지사항이 없습니다: " + noticeId));

        entity.setDeleteYn("Y");
    }

    /**
     * [추가] 공지사항 복구 (Y -> N)
     * 관리자 페이지에서 '삭제됨' 상태인 글을 다시 살릴 때 사용합니다.
     */
    @Transactional
    public void restoreNotice(Long noticeId) {
        NoticeEntity entity = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("복구할 공지사항이 없습니다: " + noticeId));

        entity.setDeleteYn("N");
    }

    /**
     * [고객용] 목록 조회 (N만 최신순)
     */
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getCustomerNoticeList() {
        List<NoticeEntity> entities = noticeRepository.findByDeleteYnOrderByInsertTimeDesc("N");
        return entities.stream()
                .map(mapStruct::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * [추가] [관리자용] 목록 조회 (N + Y 전체 최신순)
     */
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getAdminNoticeList() {
        // 리포지토리에 추가한 findAllByOrderByInsertTimeDesc() 사용
        List<NoticeEntity> entities = noticeRepository.findAllByOrderByInsertTimeDesc();
        return entities.stream()
                .map(mapStruct::toResponseDto)
                .collect(Collectors.toList());
    }
}