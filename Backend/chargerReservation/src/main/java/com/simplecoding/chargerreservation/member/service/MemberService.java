package com.simplecoding.chargerreservation.member.service;

import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.common.jwt.JwtTokenProvider;
import com.simplecoding.chargerreservation.common.SecurityUtil;
import com.simplecoding.chargerreservation.member.dto.MemberDto;
import com.simplecoding.chargerreservation.member.dto.TokenDto;
import com.simplecoding.chargerreservation.member.entity.EmailVerification;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.entity.MemberToken;
import com.simplecoding.chargerreservation.member.repository.EmailVerificationRepository;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.member.repository.MemberTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;

    /**==========================================
     * 회원 관리 (회원가입, 수정, 탈퇴)
     ========================================== */
    public Long join(MemberDto dto) {
        validateDuplicateMember(dto.getLoginId());
        validateDuplicateEmail(dto.getEmail());

        EmailVerification verification = emailVerificationRepository.findById(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일에 대한 인증 정보가 존재하지 않습니다."));

        if (!"Y".equals(verification.getIsVerified())) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요.");
        }

        Member member = Member.builder()
            .loginId(dto.getLoginId())
            .loginPw(passwordEncoder.encode(dto.getLoginPw()))
            .email(dto.getEmail())
            .name(dto.getName())
            .phone(dto.getPhone())
            .build();

        // 저장 및 인증 데이터 삭제
        Member savedMember = memberRepository.save(member);
        emailVerificationRepository.delete(verification);

        return savedMember.getMemberId();
    }

    // 회원 수정
    @Transactional
    public void modifyMember(MemberDto memberDto) {
        Member member = getCurrentMember();

        member.updateMember(memberDto.getName(), memberDto.getPhone());

        if (memberDto.getLoginPw() != null && !memberDto.getLoginPw().trim().isEmpty()) {
            String encodedPw = passwordEncoder.encode(memberDto.getLoginPw());
            member.updatePassword(encodedPw);
        }
    }

    // 회원 탈퇴
    @Transactional
    public void withdrawMember(String loginId) {
        Member member = getCurrentMember();
        member.setStatus("WITHDRAWN");

        // TODO: 예약 관련 테이블 및 데이터 처리 로직 설계 예정
        memberTokenRepository.deleteByMember(member);

        log.info("회원 탈퇴 처리 완료: {}", loginId);
    }

    /**==========================================
     * 로그인 인증 및 보안 (Auth)
     ========================================== */
    public TokenDto login(String loginId, String password, String userAgent, String clientIp) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(password, member.getLoginPw())) {
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getLoginId());

        MemberToken memberToken = memberTokenRepository.findByMember(member)
                .orElseGet(() -> MemberToken.builder().member(member).build());

        memberToken.setRefreshToken(refreshToken);
        memberToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        memberToken.setUserAgent(userAgent);
        memberToken.setClientIp(clientIp);
        memberTokenRepository.save(memberToken);

        // ── 관리자 여부 확인 후 adminRole / adminPart / adminId 세팅 ──
        Long adminId = null;
        String adminRole = null;
        String adminPart = null;

        if ("Y".equals(member.getMemberGrade())) {
            Admin admin = adminRepository.findByMemberId(member.getMemberId()).orElse(null);
            if (admin != null) {
                adminId   = admin.getAdminId();
                adminRole = admin.getAdminRole();
                adminPart = admin.getAdminPart();
            }
        }

        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getMemberId())
                .memberGrade(member.getMemberGrade())
                .adminId(adminId)
                .adminRole(adminRole)
                .adminPart(adminPart)
                .build();
    }

    // 토큰 재발급
    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
        }

        MemberToken memberToken = memberTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("DB에 존재하지 않는 토큰입니다."));

        Member member = memberToken.getMember();
        String newAt = jwtTokenProvider.createAccessToken(member);
        String newRt = jwtTokenProvider.createRefreshToken(member.getLoginId());

        memberToken.setRefreshToken(newRt);
        memberToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        // 새로운 AccessToken만 만들어서 반환(로그인 상태 연장)type = "button",
        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAt)
                .refreshToken(newRt)
                .build();
    }

    // 로그아웃
    @Transactional
    public void logout(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new IllegalArgumentException("해당 회원이 존재하지 않습니다."));

        memberTokenRepository.deleteByMember(member);
    }

    /**==========================================
     * 공통 모듈
     ==========================================*/
    // 현재 로그인 한 유저 엔티티 가져오기
    public Member getCurrentMember() {
        String currentId = SecurityUtil.getCurrentLoginId();
        return memberRepository.findByLoginId(currentId)
                .orElseThrow(() -> new RuntimeException("로그인한 사용자 정보를 찾을 수 없습니다."));
    }

    private void validateDuplicateMember(String loginId) {
        if (memberRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }

    // 아이디 존재 여부 확인 (중복 확인 버튼용)
    public boolean checkIdDuplicate(String loginId) {
        return memberRepository.findByLoginId(loginId).isPresent();
    }

    // 이메일 중복 검증 함수
    private void validateDuplicateEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
    }
}