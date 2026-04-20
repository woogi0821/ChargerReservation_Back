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
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ReservationRepository reservationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;
    private final EmailService emailService;

    // 최신 리프레시 요청 저장소(중복 요청 방어용)
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, TokenDto> refreshCache = new ConcurrentHashMap<>();
    private final Map<String, TokenDto> lastSuccessMap = new ConcurrentHashMap<>();

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

    // 아이디 찾기
    public String findLoginId(String name, String email) {
        Member member = memberRepository.findByNameAndEmail(name, email)
            .orElseThrow(() -> new IllegalArgumentException("일치하는 회원이 없습니다."));

        String loginId = member.getLoginId();

        if (loginId.length() <= 4) {
            return loginId.substring(0, 1) + "*".repeat(loginId.length() - 1);
        }

        return loginId.substring(0, 4) + "*".repeat(loginId.length() - 4);
    }

    // 비밀번호 찾기
    @Transactional
    public void sendTempPassword(Map<String, String> requestData) {
        String loginId = requestData.get("loginId");
        String phone = requestData.get("phone");
        String email = requestData.get("email");

        Member member = memberRepository.findByLoginIdAndPhoneAndEmail(loginId, phone, email)
            .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원이 없습니다."));

        String tempPw = UUID.randomUUID().toString().substring(0, 8);
        String encodedPw = passwordEncoder.encode(tempPw);
        member.updatePassword(encodedPw);

        String subject = "[ChargeNow] 임시 비밀번호 발송 안내";
        String content = member.getName() + "님, 안녕하세요.\n\n" +
            "요청하신 임시 비밀번호는 [ " + tempPw + " ] 입니다.\n" +
            "로그인 후 마이페이지에서 반드시 비밀번호를 변경해 주세요.";

        emailService.sendMail(email, subject, content);
    }

    // 회원 탈퇴
    @Transactional
    public void withdrawMember(String loginId) {
        Member member = getCurrentMember();
        reservationRepository.cancelAllByMemberId(member.getMemberId());     // 예약 취소
        member.setStatus("WITHDRAWN");                                       // 회원 상태 변경
        memberTokenRepository.deleteByMember(member);
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
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new RuntimeException("해당 계정은 사용할 수 없는 상태입니다.");
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
                .name(member.getName()) // ✅ 추가 — 회원 이름
                .adminId(adminId)
                .adminRole(adminRole)
                .adminPart(adminPart)
                .build();
    }

    // 토큰 재발급
    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        if (refreshCache.containsKey(refreshToken)) {
            return refreshCache.get(refreshToken);
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
        }

        String loginId = jwtTokenProvider.getLoginId(refreshToken);

        Optional<MemberToken> memberTokenOpt = memberTokenRepository.findByRefreshToken(refreshToken);

        if (memberTokenOpt.isPresent()) {
            MemberToken memberToken = memberTokenOpt.get();
            Member member = memberToken.getMember();

            String newAt = jwtTokenProvider.createAccessToken(member);
            String newRt = jwtTokenProvider.createRefreshToken(member.getLoginId());

            TokenDto response = TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAt)
                .refreshToken(newRt)
                .build();

            refreshCache.put(refreshToken, response);
            lastSuccessMap.put(loginId, response);

            scheduler.schedule(() -> refreshCache.remove(refreshToken), 3, TimeUnit.SECONDS);
            scheduler.schedule(() -> lastSuccessMap.remove(loginId), 1, TimeUnit.MINUTES);

            // DB 업데이트
            memberToken.setRefreshToken(newRt);
            memberToken.setExpiresAt(LocalDateTime.now().plusDays(7));

            return response;
        }

        if (lastSuccessMap.containsKey(loginId)) {
            return lastSuccessMap.get(loginId);
        }

        throw new RuntimeException("유효하지 않은 인증 정보입니다.");
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

    public boolean checkIdDuplicate(String loginId) {
        return memberRepository.findByLoginId(loginId).isPresent();
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
    }
}