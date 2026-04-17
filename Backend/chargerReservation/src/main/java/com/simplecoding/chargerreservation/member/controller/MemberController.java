package com.simplecoding.chargerreservation.member.controller;

import com.simplecoding.chargerreservation.common.CommonUtil;
import com.simplecoding.chargerreservation.member.dto.MemberDto;
import com.simplecoding.chargerreservation.member.dto.TokenDto;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "회원 API", description = "회원가입 / 로그인 / 로그아웃 / 회원정보 조회 및 수정 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final CommonUtil commonUtil;

    @Operation(summary = "회원가입", description = "아이디, 비밀번호, 이름, 이메일, 전화번호로 회원가입합니다")
    @PostMapping("/join")
    public ResponseEntity<String> join(@Valid @RequestBody MemberDto memberDto,
                                       BindingResult result) {
        commonUtil.checkBindingResult(result);
        memberService.join(memberDto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "아이디 중복 확인", description = "사용 가능한 아이디면 true, 중복이면 false 를 반환합니다")
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam("loginId") String loginId) {
        boolean isDuplicate = memberService.checkIdDuplicate(loginId);
        return ResponseEntity.ok(!isDuplicate);
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다. AccessToken 은 응답 body, RefreshToken 은 HttpOnly 쿠키로 발급됩니다")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberDto memberDto, HttpServletRequest request) {
        try {
            String clientIp = commonUtil.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            TokenDto tokenDto = memberService.login(
                    memberDto.getLoginId(),
                    memberDto.getLoginPw(),
                    userAgent,
                    clientIp
            );

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60 * 24 * 7)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(tokenDto);

        } catch (RuntimeException e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "토큰 재발급", description = "AccessToken 만료 시 RefreshToken 쿠키로 새 AccessToken 을 발급합니다")
    @PostMapping("/refresh")
    public ResponseEntity<TokenDto> refresh(@CookieValue("refreshToken") String refreshToken) {
        TokenDto newAccessToken = memberService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(newAccessToken);
    }

    @Operation(summary = "로그아웃", description = "DB 에서 RefreshToken 을 삭제하고 로그아웃합니다")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        }
        String loginId = authentication.getName();
        memberService.logout(loginId);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다")
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

    @Operation(summary = "회원 정보 수정", description = "이름, 전화번호, 비밀번호를 수정합니다")
    @PutMapping("/me")
    public ResponseEntity<String> modifyMember(@RequestBody MemberDto memberDto) {
        memberService.modifyMember(memberDto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }
}

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<String> withdraw() {
        Member member = memberService.getCurrentMember();
        memberService.withdrawMember(member.getLoginId());

        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }

}
