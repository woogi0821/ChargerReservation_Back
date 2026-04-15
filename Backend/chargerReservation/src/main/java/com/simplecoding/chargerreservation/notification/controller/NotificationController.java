package com.simplecoding.chargerreservation.notification.controller;

import com.simplecoding.chargerreservation.notification.dto.NotificationResponseDto;
import com.simplecoding.chargerreservation.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    // 1. 현재 로그인한 사용자의 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(@AuthenticationPrincipal UserDetails userDetails) { // 현재 로그인한 사용자의 정보를 스프링 시큐리티에서 바로 가로채서 가져옴
        // userDetails를 통해 사용자 ID를 가져와 알림 목록 반환
        return ResponseEntity.ok(notificationService.getMyNotifications(userDetails.getUsername()));
    }

    // 2. 알림 읽음 처리 (종 클릭 시 'N' -> 'Y'로 변경)
    @PatchMapping("/{notiId}/read") // 전체가 아니라 일부만 수정
    public ResponseEntity<Void> markAsRead(@PathVariable Long notiId) {
        notificationService.markAsRead(notiId);
        return ResponseEntity.ok().build();
    }
}
