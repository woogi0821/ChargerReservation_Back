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
     * [관리자] 공지사항 등록
     */
    @PostMapping("/admin")
    public ApiResponse<NoticeResponseDto> registerNotice(@RequestBody NoticeRequestDto requestDto) {
        String adminId = "admin_user";
        NoticeResponseDto result = noticeService.registerNotice(requestDto, adminId);

        // 직접 객체를 생성하여 반환
        return new ApiResponse<>(true, "등록 성공", result, 0, 1);
    }
}