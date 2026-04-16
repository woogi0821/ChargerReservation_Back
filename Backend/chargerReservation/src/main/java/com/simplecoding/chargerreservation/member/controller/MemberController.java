package com.simplecoding.chargerreservation.member.controller;

import com.simplecoding.chargerreservation.common.CommonUtil;
import com.simplecoding.chargerreservation.member.dto.MemberDto;
import com.simplecoding.chargerreservation.member.dto.TokenDto;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final CommonUtil commonUtil;

    // 회원가입
    @PostMapping("/join")
    public ResponseEntity<String> join(@Valid @RequestBody MemberDto memberDto,
                                       BindingResult result) {
        commonUtil.checkBindingResult(result);
        memberService.join(memberDto);

        return ResponseEntity.ok().build();
    }

    // 아이디 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam("loginId") String loginId) {
        boolean isDuplicate = memberService.checkIdDuplicate(loginId);

        return ResponseEntity.ok(!isDuplicate);
    }

    // 로그인 (성공 시 AT, RT 발급)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberDto memberDto, HttpServletRequest request) {
        try {
            // IP 추출 (작성하신 메서드 활용) 및 User-Agent 추출
            String clientIp = commonUtil.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            // DB 저장 및 토큰 발급
            TokenDto tokenDto = memberService.login(
                memberDto.getLoginId(),
                memberDto.getLoginPw(),
                userAgent,
                clientIp
            );

            // Refresh Token 전용 쿠키 생성
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
                .httpOnly(true)    // JavaScript 접근 차단 (XSS 방어)
                .secure(false)     // TODO: 로컬 테스트 시 false, HTTPS 배포 시 true
                .path("/")
                .maxAge(60 * 60 * 24 * 7) // 7일 (DB 만료일과 동기화)
                .sameSite("Lax")   // CSRF 방어
                .build();

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(tokenDto); // 프론트에서 accessToken을 꺼내 쓸 수 있도록 함

        } catch (RuntimeException e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 토큰 재발급 (AccessToken 만료 시 리액트에서 호출)
    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> refresh(@RequestBody String refreshToken) {
        // 클라이언트가 보낸 RT로 새 AT를 발급
        TokenDto newAccessToken = memberService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(newAccessToken);
    }

    // 로그아웃 (DB에서 RT 삭제)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        }

        String loginId = authentication.getName();
        memberService.logout(loginId);

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    // 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MemberDto> getCurrentMember() {
        Member member = memberService.getCurrentMember();

        MemberDto responseDto = MemberDto.builder()
            .loginId(member.getLoginId())
            .name(member.getName())
            .email(member.getEmail())
            .phone(member.getPhone())
            .memberGrade(member.getMemberGrade())
            .build();

        return ResponseEntity.ok(responseDto);
    }

    // 회원 정보 수정
    @PutMapping("/me")
    public ResponseEntity<String> modifyMember(@RequestBody MemberDto memberDto) {
        memberService.modifyMember(memberDto);

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

}
