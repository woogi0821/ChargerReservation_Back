package com.simplecoding.chargerreservation.admin.service;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.common.SecurityUtil;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyStatus;
import com.simplecoding.chargerreservation.penalty.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;
    private final PenaltyRepository penaltyRepository;

    // ── 요청자 관리자 조회 헬퍼 ─────────────────────────────────────────────────
    private Admin getRequesterAdmin() {
        String loginId = SecurityUtil.getCurrentLoginId();
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("인증된 회원을 찾을 수 없습니다"));
        return adminRepository.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "관리자 권한이 없습니다"));
    }

    // ── 관리자 전체 목록 조회 ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AdminDto> getAdminList() {
        List<Admin> admins = adminRepository.findAll();

        return admins.stream()
                .map(admin -> {
                    String name = memberRepository.findById(admin.getMemberId())
                            .map(Member::getName)
                            .orElse("알 수 없음");
                    return AdminDto.from(admin, name);
                })
                .collect(Collectors.toList());
    }

    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    public AdminDto createAdmin(AdminDto dto) {
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new RuntimeException("등록 대상 회원을 찾을 수 없습니다"));

        Admin admin = new Admin(dto.getMemberId(), dto.getAdminRole());
        adminRepository.save(admin);

        // 관리자 등록 시 MEMBER_GRADE 'Y' 로 변경
        member.setMemberGrade("Y");
        memberRepository.save(member);

        return AdminDto.from(admin);
    }

    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminDto getAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다"));
        return AdminDto.from(admin);
    }

    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    public AdminDto updateAdminRole(Long targetId, String newRole) {

        Admin requester = getRequesterAdmin();

        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 역할 변경이 가능합니다");
        }

        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));

        target.updateRole(newRole);
        return AdminDto.from(adminRepository.save(target));
    }

    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    public void deleteAdmin(Long targetId) {

        Admin requester = getRequesterAdmin();

        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 관리자 해제가 가능합니다");
        }

        if (requester.getAdminId().equals(targetId)) {
            throw new RuntimeException("자기 자신은 해제할 수 없습니다");
        }

        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));

        adminRepository.delete(target);

        // 관리자 해제 시 MEMBER_GRADE 'N' 으로 복구
        Member member = memberRepository.findById(target.getMemberId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));
        member.setMemberGrade("N");
        memberRepository.save(member);
    }

    // ── 회원 전체 목록 조회 (SUPER / ALL / MEMBER 파트만 가능) ───────────────────
    @Transactional(readOnly = true)
    public List<AdminMemberDto> getMemberList() {

        Admin requester = getRequesterAdmin();

        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        // ALL 파트는 모든 영역 접근 가능 → MEMBER 파트와 동일하게 허용
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER")
                || requester.getAdminPart().equals("ALL");

        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 조회 권한이 없습니다");
        }

        return memberRepository.findAll()
                .stream()
                .map(AdminMemberDto::from)
                .collect(Collectors.toList());
    }

    // ── 회원 상태 변경 (SUPER / ALL / MEMBER 파트만 가능) ────────────────────────
    public AdminMemberDto updateMemberStatus(Long memberId, String newStatus) {

        Admin requester = getRequesterAdmin();

        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        // ALL 파트는 모든 영역 접근 가능 → MEMBER 파트와 동일하게 허용
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER")
                || requester.getAdminPart().equals("ALL");

        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 상태 변경 권한이 없습니다");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));

        member.setStatus(newStatus);

        if (newStatus.equals("SUSPENDED")) {
            member.setSuspendedUntil(LocalDateTime.now().plusHours(24));
        }

        if (newStatus.equals("ACTIVE") || newStatus.equals("WITHDRAWN")) {
            member.setSuspendedUntil(null);
        }

        return AdminMemberDto.from(memberRepository.save(member));
    }

    // ── 패널티 전체 목록 조회 (SUPER / ALL / INQUIRY 파트만 가능) ────────────────
    @Transactional(readOnly = true)
    public List<AdminPenaltyDto> getPenaltyList() {

        Admin requester = getRequesterAdmin();

        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        // ALL 파트는 모든 영역 접근 가능 → INQUIRY 파트와 동일하게 허용
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");

        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "패널티 조회 권한이 없습니다");
        }

        return penaltyRepository.findAll()
                .stream()
                .map(AdminPenaltyDto::from)
                .collect(Collectors.toList());
    }

    // ── 패널티 취소 (SUPER / ALL / INQUIRY 파트만 가능) ──────────────────────────
    public AdminPenaltyDto cancelPenalty(Long penaltyId) {

        Admin requester = getRequesterAdmin();

        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        // ALL 파트는 모든 영역 접근 가능 → INQUIRY 파트와 동일하게 허용
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");

        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "패널티 취소 권한이 없습니다");
        }

        PenaltyHistory penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new RuntimeException("패널티를 찾을 수 없습니다"));

        if (penalty.getStatus().equals(PenaltyStatus.CANCELED)) {
            throw new RuntimeException("이미 취소된 패널티입니다");
        }

        penalty.setStatus(PenaltyStatus.CANCELED);

        return AdminPenaltyDto.from(penaltyRepository.save(penalty));
    }
}