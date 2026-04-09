//package com.simplecoding.chargerreservation.admin.controller;
//
//import com.simplecoding.chargerreservation.admin.dto.AdminDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminLoginRequestDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminLoginResponseDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
//import com.simplecoding.chargerreservation.admin.service.AdminService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/admins")
//@RequiredArgsConstructor
//public class AdminController {
//
//    private final AdminService adminService;
//
//    // ── 어드민 로그인 ────────────────────────────
//    // POST /api/admins/login
//    @PostMapping("/login")
//    public ResponseEntity<AdminLoginResponseDto> login(@RequestBody AdminLoginRequestDto dto) {
//        return ResponseEntity.ok(adminService.login(dto));
//    }
//
//    // ── 관리자 전체 목록 조회 ────────────────────
//    // GET /api/admins/list
//    @GetMapping("/list")
//    public ResponseEntity<List<AdminDto>> getAdminList() {
//        return ResponseEntity.ok(adminService.getAdminList());
//    }
//
//    // ── 관리자 등록 ──────────────────────────────
//    // POST /api/admins
//    @PostMapping
//    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto dto) {
//        return ResponseEntity.ok(adminService.createAdmin(dto));
//    }
//
//    // ── 관리자 단건 조회 ─────────────────────────
//    // GET /api/admins/{adminId}
//    @GetMapping("/{adminId}")
//    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long adminId) {
//        return ResponseEntity.ok(adminService.getAdmin(adminId));
//    }
//
//    // ── 관리자 역할 변경 ─────────────────────────
//    // PATCH /api/admins/{targetId}/role?requesterId=1&newRole=MANAGER
//    @PatchMapping("/{targetId}/role")
//    public ResponseEntity<AdminDto> updateAdminRole(
//            @RequestParam Long requesterId,
//            @PathVariable Long targetId,
//            @RequestParam String newRole) {
//        return ResponseEntity.ok(
//                adminService.updateAdminRole(requesterId, targetId, newRole)
//        );
//    }
//
//    // ── 관리자 해제 (SUPER 만 가능) ──────────────
//    // DELETE /api/admins/{targetId}?requesterId=1
//    @DeleteMapping("/{targetId}")
//    public ResponseEntity<Void> deleteAdmin(
//            @RequestParam Long requesterId,
//            @PathVariable Long targetId) {
//        adminService.deleteAdmin(requesterId, targetId);
//        return ResponseEntity.noContent().build();
//    }
//
//    // ── 회원 전체 목록 조회 (SUPER + MEMBER 파트만 가능) ──
//    // GET /api/admins/members?requesterId=1
//    @GetMapping("/members")
//    public ResponseEntity<List<AdminMemberDto>> getMemberList(
//            @RequestParam Long requesterId) {
//        return ResponseEntity.ok(adminService.getMemberList(requesterId));
//    }
//
//    // ── 회원 상태 변경 (SUPER + MEMBER 파트만 가능) ──
//    // PATCH /api/admins/members/{memberId}?requesterId=1&newStatus=SUSPENDED
//    @PatchMapping("/members/{memberId}")
//    public ResponseEntity<AdminMemberDto> updateMemberStatus(
//            @RequestParam Long requesterId,
//            @PathVariable Long memberId,
//            @RequestParam String newStatus) {
//        return ResponseEntity.ok(
//                adminService.updateMemberStatus(requesterId, memberId, newStatus)
//        );
//    }
//}