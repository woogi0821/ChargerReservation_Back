package com.simplecoding.chargerreservation.admin.controller;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── 관리자 전체 목록 조회 ────────────────────────────────────────────────────
    // GET /api/admin/list
    @GetMapping("/list")
    public ResponseEntity<List<AdminDto>> getAdminList() {
        return ResponseEntity.ok(adminService.getAdminList());
    }

    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    // POST /api/admin
    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto dto) {
        return ResponseEntity.ok(adminService.createAdmin(dto));
    }

    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    // GET /api/admin/{adminId}
    @GetMapping("/{adminId}")
    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdmin(adminId));
    }

    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    // PATCH /api/admin/{targetId}/role?newRole=MANAGER
    @PatchMapping("/{targetId}/role")
    public ResponseEntity<AdminDto> updateAdminRole(
            @PathVariable Long targetId,
            @RequestParam String newRole) {
        return ResponseEntity.ok(
                adminService.updateAdminRole(targetId, newRole)
        );
    }

    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    // DELETE /api/admin/{targetId}
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long targetId) {
        adminService.deleteAdmin(targetId);
        return ResponseEntity.noContent().build();
    }

    // ── 회원 전체 목록 조회 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────
    // GET /api/admin/members
    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberDto>> getMemberList() {
        return ResponseEntity.ok(adminService.getMemberList());
    }

    // ── 회원 상태 변경 (SUPER 또는 MEMBER 파트만 가능) ──────────────────────────
    // PATCH /api/admin/members/{memberId}?newStatus=SUSPENDED
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<AdminMemberDto> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(
                adminService.updateMemberStatus(memberId, newStatus)
        );
    }

    // ── 패널티 전체 목록 조회 (SUPER 또는 INQUIRY 파트만 가능) ──────────────────
    // GET /api/admin/penalties
    @GetMapping("/penalties")
    public ResponseEntity<List<AdminPenaltyDto>> getPenaltyList() {
        return ResponseEntity.ok(adminService.getPenaltyList());
    }

    // ── 패널티 취소 (SUPER 또는 INQUIRY 파트만 가능) ─────────────────────────────
    // PATCH /api/admin/penalties/{penaltyId}
    @PatchMapping("/penalties/{penaltyId}")
    public ResponseEntity<AdminPenaltyDto> cancelPenalty(
            @PathVariable Long penaltyId) {
        return ResponseEntity.ok(adminService.cancelPenalty(penaltyId));
    }
}