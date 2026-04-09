package com.simplecoding.chargerreservation.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 어드민 로그인 응답 데이터
// 서버 → 클라이언트 로 전달되는 데이터
@Getter
@AllArgsConstructor
public class AdminLoginResponseDto {

    // 발급된 JWT 액세스 토큰
    private String accessToken;

    // 어드민 역할 — SUPER / MANAGER / VIEWER
    private String adminRole;

    // 어드민 ID
    private Long adminId;

    // 담당 파트 — ALL / MEMBER / RESERVATION / CHARGER / INQUIRY
    private String adminPart;
}