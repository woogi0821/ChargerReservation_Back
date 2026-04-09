package com.simplecoding.chargerreservation.admin.controller;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    // AdminService 주입 (로그인/JWT 는 MemberController + JwtTokenProvider 에서 처리)
    private final AdminService adminService;


    // ── 관리자 전체 목록 조회 ────────────────────────────────────────────────────
    // GET /api/admins/list
    // 등록된 관리자 전체 목록 반환 (이름 포함)
    @GetMapping("/list")
    public ResponseEntity<List<AdminDto>> getAdminList() {
        return ResponseEntity.ok(adminService.getAdminList());
    }


    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    // POST /api/admins
    // Body : { "memberId": 1, "adminRole": "MANAGER" }
    // 특정 회원을 관리자로 등록 (adminPart 기본값 ALL)
    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto dto) {
        return ResponseEntity.ok(adminService.createAdmin(dto));
    }


    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    // GET /api/admins/{adminId}
    // adminId 로 특정 관리자 정보 조회
    @GetMapping("/{adminId}")
    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdmin(adminId));
    }


    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    // PATCH /api/admins/{targetId}/role?requesterId=1&newRole=MANAGER
    // requesterId : 요청자 adminId (SUPER 여부 검증용)
    // targetId    : 역할을 변경할 대상 adminId
    // newRole     : 변경할 역할 (SUPER / MANAGER / VIEWER)
    @PatchMapping("/{targetId}/role")
    public ResponseEntity<AdminDto> updateAdminRole(
            @PathVariable Long targetId,
            @RequestParam String newRole) {
        return ResponseEntity.ok(
                adminService.updateAdminRole(targetId, newRole)
        );
    }


    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    // DELETE /api/admins/{targetId}?requesterId=1
    // requesterId : 요청자 adminId (SUPER 여부 검증용)
    // targetId    : 해제할 대상 adminId
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long targetId) {
        adminService.deleteAdmin(targetId);
        return ResponseEntity.noContent().build();
    }


    // ── 회원 전체 목록 조회 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────
    // GET /api/admins/members?requesterId=1
    // MEMBER 파트 관리자 또는 SUPER 만 접근 가능
    // 다른 파트 관리자 접근 시 403 반환
    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberDto>> getMemberList() {
        return ResponseEntity.ok(adminService.getMemberList());
    }


    // ── 회원 상태 변경 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────────
    // PATCH /api/admins/members/{memberId}?requesterId=1&newStatus=SUSPENDED
    // newStatus 값 : ACTIVE / SUSPENDED / WITHDRAWN
    // SUSPENDED 시 자동으로 24시간 정지 처리
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<AdminMemberDto> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(
                adminService.updateMemberStatus(memberId, newStatus)
        );
    }
}