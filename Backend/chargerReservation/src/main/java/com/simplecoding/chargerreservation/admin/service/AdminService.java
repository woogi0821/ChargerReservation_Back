package com.simplecoding.chargerreservation.admin.service;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.common.SecurityUtil;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
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

    // ── 의존성 주입 ──────────────────────────────────────────────────────────────
    // 로그인/JWT 발급은 MemberService + JwtTokenProvider 에서 처리하므로
    // AdminService 에서는 관리자 관련 비즈니스 로직만 담당
    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    private Admin getRequesterAdmin() {
        String loginId = SecurityUtil.getCurrentLoginId();
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("인증된 회원을 찾을 수 없습니다"));
        return adminRepository.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "관리자 권한이 없습니다"));
    }

    // ── 관리자 전체 목록 조회 ────────────────────────────────────────────────────
    // 등록된 관리자 전체 목록을 반환
    // Admin 테이블과 Member 테이블을 조합해서 관리자 이름까지 포함하여 응답
    @Transactional(readOnly = true)
    public List<AdminDto> getAdminList() {
        List<Admin> admins = adminRepository.findAll();

        return admins.stream()
                .map(admin -> {
                    // Admin 테이블에는 이름이 없으므로 memberId로 Member 테이블에서 이름 조회
                    String name = memberRepository.findById(admin.getMemberId())
                            .map(Member::getName)
                            .orElse("알 수 없음"); // 매핑된 회원이 없을 경우 기본값
                    return AdminDto.from(admin, name);
                })
                .collect(Collectors.toList());
    }


    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    // 특정 회원(memberId)을 관리자로 등록
    // 등록 완료 후 해당 회원의 MEMBER_GRADE 를 'Y' (관리자) 로 변경
    public AdminDto createAdmin(AdminDto dto) {
        // 등록 대상 회원이 실제로 존재하는지 검증
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new RuntimeException("등록 대상 회원을 찾을 수 없습니다"));

        // Admin 엔티티 생성 후 저장
        Admin admin = new Admin(dto.getMemberId(), dto.getAdminRole());
        adminRepository.save(admin);

        // 관리자 등록 시 MEMBER_GRADE 'Y' 로 변경 → JWT 권한 체크 기준값
        member.setMemberGrade("Y");
        memberRepository.save(member);

        return AdminDto.from(admin);
    }


    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    // adminId 로 특정 관리자 정보 조회
    @Transactional(readOnly = true)
    public AdminDto getAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다"));
        return AdminDto.from(admin);
    }


    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    // targetId    : 역할을 변경할 대상 관리자의 adminId
    // newRole     : 변경할 역할 값 (SUPER / MANAGER / VIEWER)
    public AdminDto updateAdminRole(Long targetId, String newRole) {

        // 요청자 조회 및 SUPER 권한 검증
        Admin requester = getRequesterAdmin();

        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 역할 변경이 가능합니다");
        }

        // 변경 대상 관리자 조회 후 역할 업데이트
        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));

        target.updateRole(newRole);
        return AdminDto.from(adminRepository.save(target));
    }


    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    // targetId    : 해제할 대상 관리자의 adminId
    // 해제 완료 후 해당 회원의 MEMBER_GRADE 를 'N' (일반회원) 으로 복구
    public void deleteAdmin(Long targetId) {

        // 요청자 조회 및 SUPER 권한 검증
        Admin requester = getRequesterAdmin();

        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 관리자 해제가 가능합니다");
        }

        // 자기 자신 해제 방지
        if (requester.getAdminId().equals(targetId)) {
            throw new RuntimeException("자기 자신은 해제할 수 없습니다");
        }

        // 대상 관리자 조회 후 삭제
        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));

        adminRepository.delete(target);

        // 관리자 해제 시 MEMBER_GRADE 'N' 으로 복구 → 일반 회원 등급으로 되돌림
        Member member = memberRepository.findById(target.getMemberId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));
        member.setMemberGrade("N");
        memberRepository.save(member);
    }


    // ── 회원 전체 목록 조회 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────
    // 회원관리 파트(adminPart = MEMBER) 또는 최고관리자(SUPER) 만 접근 가능
    // 다른 파트 관리자(RESERVATION, CHARGER 등)는 접근 불가 → 403 반환
    @Transactional(readOnly = true)
    public List<AdminMemberDto> getMemberList() {

        Admin requester = getRequesterAdmin();

        // SUPER 역할이거나 담당 파트가 MEMBER 인 경우에만 허용
        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER");

        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 조회 권한이 없습니다");
        }

        // 전체 회원 목록을 AdminMemberDto 로 변환하여 반환
        return memberRepository.findAll()
                .stream()
                .map(AdminMemberDto::from)
                .collect(Collectors.toList());
    }


    // ── 회원 상태 변경 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────────
    // newStatus 값 : ACTIVE(정상) / SUSPENDED(정지) / WITHDRAWN(탈퇴)
    // SUSPENDED 설정 시 suspendedUntil 을 현재 시각 + 24시간 으로 자동 세팅
    // ACTIVE / WITHDRAWN 설정 시 suspendedUntil 초기화
    public AdminMemberDto updateMemberStatus(Long memberId, String newStatus) {

        Admin requester = getRequesterAdmin();

        // SUPER 역할이거나 담당 파트가 MEMBER 인 경우에만 허용
        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER");

        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 상태 변경 권한이 없습니다");
        }

        // 대상 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));

        // 상태값 변경
        member.setStatus(newStatus);

        // SUSPENDED 상태일 경우 정지 만료 시각을 24시간 후로 설정
        if (newStatus.equals("SUSPENDED")) {
            member.setSuspendedUntil(LocalDateTime.now().plusHours(24));
        }

        // ACTIVE 또는 WITHDRAWN 상태로 변경 시 정지 만료 시각 초기화
        if (newStatus.equals("ACTIVE") || newStatus.equals("WITHDRAWN")) {
            member.setSuspendedUntil(null);
        }

        return AdminMemberDto.from(memberRepository.save(member));
    }
}