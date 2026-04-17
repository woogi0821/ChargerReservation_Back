package com.simplecoding.chargerreservation.notification.controller;

import com.simplecoding.chargerreservation.notification.dto.NotificationResponseDto;
import com.simplecoding.chargerreservation.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알림 API", description = "사용자 알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 반환합니다")
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userDetails.getUsername()));
    }

    @Operation(summary = "알림 읽음 처리", description = "notiId 로 특정 알림을 읽음 처리합니다 (isRead N → Y)")
    @PatchMapping("/{notiId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notiId) {
        notificationService.markAsRead(notiId);
        return ResponseEntity.ok().build();
    }
}