package com.simplecoding.chargerreservation.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


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
}
