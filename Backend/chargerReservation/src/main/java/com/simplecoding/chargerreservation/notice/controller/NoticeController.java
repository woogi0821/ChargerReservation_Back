package com.simplecoding.chargerreservation.notice.controller;

import com.simplecoding.chargerreservation.common.ApiResponse;
import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공지사항 API", description = "공지사항 조회 / 등록 / 수정 / 삭제 / 복구 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "고객용 공지사항 목록 조회", description = "삭제되지 않은 공지사항 목록을 반환합니다 (deleteYn = N)")
    @GetMapping
    public ApiResponse<List<NoticeResponseDto>> getCustomerNoticeList(
            @RequestParam(defaultValue = "1") int page) {
        return noticeService.getCustomerNoticeList(page);
    }

    @Operation(summary = "관리자용 공지사항 전체 목록 조회", description = "삭제된 공지사항 포함 전체 목록을 반환합니다 (deleteYn = N + Y)")
    @GetMapping("/admin")
    public ApiResponse<List<NoticeResponseDto>> getAdminNoticeList() {
        List<NoticeResponseDto> list = noticeService.getAdminNoticeList();
        return new ApiResponse<>(true, "관리자용 전체 공지사항 조회 성공", list, list.size(), 1);
    }

    @Operation(summary = "공지사항 등록", description = "새 공지사항을 등록합니다")
    @PostMapping("/admin")
    public ApiResponse<NoticeResponseDto> registerNotice(@RequestBody NoticeRequestDto requestDto) {
        String adminId = "admin_user";
        NoticeResponseDto result = noticeService.registerNotice(requestDto, adminId);
        return new ApiResponse<>(true, "등록 성공", result, 0, 1);
    }

    @Operation(summary = "공지사항 수정", description = "noticeId 로 공지사항을 수정합니다")
    @PutMapping("/admin/{noticeId}")
    public ApiResponse<NoticeResponseDto> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeRequestDto requestDto) {
        NoticeResponseDto result = noticeService.updateNotice(noticeId, requestDto);
        return new ApiResponse<>(true, "공지사항이 수정되었습니다.", result, 0, 0);
    }

    @Operation(summary = "공지사항 삭제", description = "공지사항을 소프트 삭제합니다 (deleteYn N → Y)")
    @DeleteMapping("/admin/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return new ApiResponse<>(true, noticeId + "번 공지사항이 삭제(비활성화) 되었습니다.", null, 0, 0);
    }

    @Operation(summary = "공지사항 복구", description = "삭제된 공지사항을 복구합니다 (deleteYn Y → N)")
    @PatchMapping("/admin/restore/{noticeId}")
    public ApiResponse<Void> restoreNotice(@PathVariable Long noticeId) {
        noticeService.restoreNotice(noticeId);
        return new ApiResponse<>(true, noticeId + "번 공지사항이 복구되었습니다.", null, 0, 0);
    }
}