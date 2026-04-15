package com.simplecoding.chargerreservation.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private Long memberId;
    private String memberGrade;
    private Long adminId;       // 관리자 ID
    private String adminRole;   // 관리자 역할 (SUPER / MANAGER)
    private String adminPart;   // 관리자 파트 (ALL / MEMBER / RESERVATION 등)
}