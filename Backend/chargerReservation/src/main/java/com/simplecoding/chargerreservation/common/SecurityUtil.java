package com.simplecoding.chargerreservation.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 SecurityContext에 저장된 로그인 유저의 ID를 반환합니다.
 */
public class SecurityUtil {
    private SecurityUtil() {}

    public static String getCurrentLoginId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인이 필요합니다.");
        }

        return authentication.getName();
    }
}
