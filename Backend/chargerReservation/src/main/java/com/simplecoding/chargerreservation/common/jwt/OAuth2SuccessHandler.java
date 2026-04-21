package com.simplecoding.chargerreservation.common.jwt;

import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.entity.MemberToken;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.member.repository.MemberTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final MemberTokenRepository memberTokenRepository;
    @Value("${spring.react.ip}")
    private  String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Principal에서 사용자 정보 추출
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = extractEmail(oAuth2User);

        // 회원 정보 조회
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다."));

        // 토큰 생성 (JwtTokenProvider 사용)
        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member.getLoginId());

        // 접속 정보 및 만료 시간 설정
        String userAgent = request.getHeader("User-Agent");
        String clientIp = request.getRemoteAddr();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 4. DB 저장 또는 업데이트
        MemberToken memberToken = memberTokenRepository.findByMember(member)
            .orElse(new MemberToken());

        memberToken.setMember(member);
        memberToken.setRefreshToken(refreshToken);
        memberToken.setUserAgent(userAgent);
        memberToken.setClientIp(clientIp);
        memberToken.setExpiresAt(expiresAt);

        memberTokenRepository.save(memberToken);

// 수정
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) //aws배포시 true로 수정 로컬테스트 환경에서 false로
                .path("/")
//                .maxAge(60 * 60 * 24 * 7)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }

    private String extractEmail(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if (attributes.get("kakao_account") != null) {
            return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
        }
        if (attributes.get("response") != null) {
            return (String) ((Map<String, Object>) attributes.get("response")).get("email");
        }
        return (String) attributes.get("email");
    }

}
