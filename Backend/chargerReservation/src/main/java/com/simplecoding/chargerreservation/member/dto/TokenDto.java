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
    private String name;        // ✅ 추가 — 회원 이름
    private Long adminId;
    private String adminRole;
    private String adminPart;
}