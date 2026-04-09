//package com.simplecoding.chargerreservation.notice.controller;
//
//import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
//import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
//import com.simplecoding.chargerreservation.notice.service.NoticeService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/admin/notices")
//@RequiredArgsConstructor
//public class NoticeController {
//
//    private final NoticeService noticeService;
//
//    // 목록 조회
//    @GetMapping
//    public ResponseEntity<List<NoticeResponseDto>> getList() {
//        return ResponseEntity.ok(noticeService.getNoticeList());
//    }
//
//    // 단건 조회
//    @GetMapping("/{noticeId}")
//    public ResponseEntity<NoticeResponseDto> getOne(@PathVariable Long noticeId) {
//        return ResponseEntity.ok(noticeService.getNotice(noticeId));
//    }
//
//    // 작성
//    // ⚠️ author는 추후 Spring Security 세션에서 꺼내는 걸로 교체 필요
//    @PostMapping
//    public ResponseEntity<NoticeResponseDto> create(
//            @RequestBody NoticeRequestDto dto,
//            @RequestParam(defaultValue = "홍길동") String author
//    ) {
//        return ResponseEntity.ok(noticeService.createNotice(dto, author));
//    }
//
//    // 수정
//    @PutMapping("/{noticeId}")
//    public ResponseEntity<NoticeResponseDto> update(
//            @PathVariable Long noticeId,
//            @RequestBody NoticeRequestDto dto
//    ) {
//        return ResponseEntity.ok(noticeService.updateNotice(noticeId, dto));
//    }
//
//    // 삭제
//    @DeleteMapping("/{noticeId}")
//    public ResponseEntity<Void> delete(@PathVariable Long noticeId) {
//        noticeService.deleteNotice(noticeId);
//        return ResponseEntity.noContent().build();
//    }
//}