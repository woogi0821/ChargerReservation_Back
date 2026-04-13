package com.simplecoding.chargerreservation.notice.controller;

import com.simplecoding.chargerreservation.common.ApiResponse;
import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * [고객용] 공지사항 목록 조회 (N만 반환)
     * GET /api/notices
     */
    @GetMapping
    public ApiResponse<List<NoticeResponseDto>> getCustomerNoticeList() {
        List<NoticeResponseDto> list = noticeService.getCustomerNoticeList();
        return new ApiResponse<>(true, "고객용 공지사항 조회 성공", list, list.size(), 1);
    }

    /**
     * [관리자용] 모든 공지사항 목록 조회 (N + Y 전체)
     * GET /api/notices/admin
     */
    @GetMapping("/admin")
    public ApiResponse<List<NoticeResponseDto>> getAdminNoticeList() {
        List<NoticeResponseDto> list = noticeService.getAdminNoticeList();
        return new ApiResponse<>(true, "관리자용 전체 공지사항 조회 성공", list, list.size(), 1);
    }

    /**
     * [관리자] 공지사항 등록
     */
    @PostMapping("/admin")
    public ApiResponse<NoticeResponseDto> registerNotice(@RequestBody NoticeRequestDto requestDto) {
        String adminId = "admin_user"; // 실제 환경에서는 시큐리티 세션 등에서 추출
        NoticeResponseDto result = noticeService.registerNotice(requestDto, adminId);
        return new ApiResponse<>(true, "등록 성공", result, 0, 1);
    }

    /**
     * [관리자] 공지사항 수정
     */
    @PutMapping("/admin/{noticeId}")
    public ApiResponse<NoticeResponseDto> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeRequestDto requestDto) {
        NoticeResponseDto result = noticeService.updateNotice(noticeId, requestDto);
        return new ApiResponse<>(true, "공지사항이 수정되었습니다.", result, 0, 0);
    }

    /**
     * [관리자] 공지사항 소프트 삭제 (N -> Y)
     * DELETE /api/notices/admin/{noticeId}
     */
    @DeleteMapping("/admin/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return new ApiResponse<>(true, noticeId + "번 공지사항이 삭제(비활성화) 되었습니다.", null, 0, 0);
    }

    /**
     * [관리자] 공지사항 복구 (Y -> N)
     * PATCH /api/notices/admin/restore/{noticeId}
     */
    @PatchMapping("/admin/restore/{noticeId}")
    public ApiResponse<Void> restoreNotice(@PathVariable Long noticeId) {
        noticeService.restoreNotice(noticeId);
        return new ApiResponse<>(true, noticeId + "번 공지사항이 복구되었습니다.", null, 0, 0);
    }
}