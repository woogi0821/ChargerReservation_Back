//package com.simplecoding.chargerreservation.notice.service;
//
//
//import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
//import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
//import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class NoticeService {
//
//    private final NoticeRepository noticeRepository;
//
//    /* 목록 조회 */
//    @Transactional(readOnly = true)
//    public List<NoticeResponseDto> getNoticeList() {
//        return noticeRepository.findAllActive()
//                .stream()
//                .map(NoticeResponseDto::from)
//                .collect(Collectors.toList());
//    }
//
//    /* 단건 조회 */
//    @Transactional(readOnly = true)
//    public NoticeResponseDto getNotice(Long noticeId) {
//        Notice notice = noticeRepository
//                .findByNoticeIdAndIsDeletedFalse(noticeId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다. id=" + noticeId));
//        return NoticeResponseDto.from(notice);
//    }
//
//    /* 공지 작성 */
//    @Transactional
//    public NoticeResponseDto createNotice(NoticeRequestDto dto, String author) {
//        String noticeNo = generateNoticeNo();
//
//        Notice notice = Notice.builder()
//                .noticeNo(noticeNo)
//                .title(dto.getTitle())
//                .content(dto.getContent())
//                .author(author)
//                .isPinned(dto.isPinned())
//                .build();
//
//        return NoticeResponseDto.from(noticeRepository.save(notice));
//    }
//
//    /* 공지 수정 */
//    @Transactional
//    public NoticeResponseDto updateNotice(Long noticeId, NoticeRequestDto dto) {
//        Notice notice = noticeRepository
//                .findByNoticeIdAndIsDeletedFalse(noticeId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다. id=" + noticeId));
//
//        notice.setTitle(dto.getTitle());
//        notice.setContent(dto.getContent());
//        notice.setPinned(dto.isPinned());
//        // dirty checking으로 자동 UPDATE (save() 호출 불필요)
//
//        return NoticeResponseDto.from(notice);
//    }
//
//    /* 공지 삭제 (소프트 삭제) */
//    @Transactional
//    public void deleteNotice(Long noticeId) {
//        Notice notice = noticeRepository
//                .findByNoticeIdAndIsDeletedFalse(noticeId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다. id=" + noticeId));
//
//        notice.setDeleted(true);
//        // dirty checking으로 자동 UPDATE
//    }
//
//    /* noticeNo 채번 (n001 ~ n999) */
//    private String generateNoticeNo() {
//        String maxNo = noticeRepository.findMaxNoticeNo();
//        if (maxNo == null) return "n001";
//        int next = Integer.parseInt(maxNo.substring(1)) + 1;
//        return String.format("n%03d", next);
//    }
//}