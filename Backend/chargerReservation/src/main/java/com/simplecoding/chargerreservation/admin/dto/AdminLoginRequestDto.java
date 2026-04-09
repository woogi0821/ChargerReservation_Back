package com.simplecoding.chargerreservation.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 어드민 로그인 요청 데이터
// 클라이언트 → 서버 로 전달되는 데이터
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequestDto {

    // 로그인 아이디 — MEMBER 테이블의 LOGIN_ID
    private String loginId;

    // 로그인 비밀번호 — MEMBER 테이블의 LOGIN_PW
    private String loginPw;
}