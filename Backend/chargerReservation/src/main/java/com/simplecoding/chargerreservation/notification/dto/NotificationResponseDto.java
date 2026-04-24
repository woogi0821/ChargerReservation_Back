package com.simplecoding.chargerreservation.notification.dto;

import com.simplecoding.chargerreservation.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;


@Getter
@Builder
@AllArgsConstructor
public class NotificationResponseDto {
    private Long notiId;
    private String title;
    private String message;
    private String notiType;  // Enum을 String으로 변환해서 전달
    private String targetUrl;
    private String isRead;
    private String createdAt;

    // 엔티티를 DTO로 변환하는 생성자 추가
    public NotificationResponseDto(Notification notification) {
        this.notiId = notification.getNotiId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        // Enum이 null일 경우를 대비해 안전하게 처리
        this.notiType = notification.getNotiType() != null ? notification.getNotiType().name() : null;
        this.targetUrl = notification.getTargetUrl();
        this.isRead = notification.getIsRead();

        // LocalDateTime을 리액트에서 보기 편하게 String으로 변환 (예: 2026-04-22 16:10)
        if (notification.getCreatedAt() != null) {
            this.createdAt = notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }
}
