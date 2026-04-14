package com.simplecoding.chargerreservation.member.service;

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

    /**==========================================
     * 회원 관리 (회원가입)
     ========================================== */
    public Long join(MemberDto dto) {
        // 아이디 및 이메일 중복확인
        validateDuplicateMember(dto.getLoginId());
        validateDuplicateEmail(dto.getEmail());

        // 이메일 인증 여부 확인
        EmailVerification verification = emailVerificationRepository.findById(dto.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("해당 이메일에 대한 인증 정보가 존재하지 않습니다."));

        if (!"Y".equals(verification.getIsVerified())) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요.");
        }

        // 엔티티 생성 및 비밀번호 암호화
        Member member = Member.builder()
            .loginId(dto.getLoginId())
            .loginPw(passwordEncoder.encode(dto.getLoginPw()))
            .email(dto.getEmail())
            .name(dto.getName())
            .phone(dto.getPhone())
//            .status("ACTIVE")
//            .memberGrade("N")
//            .provider("LOCAL")
            .build();

        // 저장 및 인증 데이터 삭제
        Member savedMember = memberRepository.save(member);
        emailVerificationRepository.delete(verification);

        return savedMember.getMemberId();
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

        // 로그인 성공시 토큰 발행
        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getLoginId());

        // Refresh Token DB 저장 (기존 토큰 있으면 삭제 후 갱신 또는 업데이트)
        MemberToken memberToken = memberTokenRepository.findByMember(member)
            .orElseGet(() -> MemberToken.builder().member(member).build());

        memberToken.setRefreshToken(refreshToken);
        memberToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        memberToken.setUserAgent(userAgent);
        memberToken.setClientIp(clientIp);

        memberTokenRepository.save(memberToken);

        return TokenDto.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .memberGrade(member.getMemberGrade())
            .build();
    }

    // 토큰 재발급: 유효한 RefreshToken을 받아서 새로운 AccessToken을 생성
    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        // 전달받은 RefreshToken이 유효한지 체크 (유효기간 등)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
        }

        // DB에 이 토큰이 저장되어 있는지 확인
        MemberToken memberToken = memberTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("DB에 존재하지 않는 토큰입니다."));

        // 새로운 AT와 새로운 RT를 모두 생성
        Member member = memberToken.getMember();
        String newAt = jwtTokenProvider.createAccessToken(member);
        String newRt = jwtTokenProvider.createRefreshToken(member.getLoginId());

        // DB의 기존 토큰 정보를 새로운 RT로 업데이트 (Dirty Checking으로 자동 반영)
        memberToken.setRefreshToken(newRt);
        memberToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // 만료일 갱신

        // 새로운 AccessToken만 만들어서 반환(로그인 상태 연장)
        return TokenDto.builder()
            .grantType("Bearer")
            .accessToken(newAt)
            .refreshToken(newRt)
            .build();
    }

    // 로그아웃: DB에서 리프레시 토큰을 삭제하여 재발급을 막음
    @Transactional
    public void logout(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new IllegalArgumentException("해당 회원이 존재하지 않습니다."));

        // DB에서 해당 멤버의 토큰 삭제
        memberTokenRepository.deleteByMember(member);
    }

    /**==========================================
     * 공통 모듈
     ==========================================*/
    // 현재 로그인 한 유저 엔티티 가져오기
    // 사용 방법: Member writer = memberService.getCurrentMember();
    public Member getCurrentMember() {
        String currentId = SecurityUtil.getCurrentLoginId();

        return memberRepository.findByLoginId(currentId)
            .orElseThrow(() -> new RuntimeException("로그인한 사용자 정보를 찾을 수 없습니다."));
    }

    //  아이디 중복 검증 함수
    private void validateDuplicateMember(String loginId) {
        if (memberRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }
    // 이메일 중복 검증 함수
    private void validateDuplicateEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
    }


}
