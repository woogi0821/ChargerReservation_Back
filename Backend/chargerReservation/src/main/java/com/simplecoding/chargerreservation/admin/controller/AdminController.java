package com.simplecoding.chargerreservation.admin.controller;

import com.simplecoding.chargerreservation.admin.dto.AdminChargerDto;
import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminNoticeDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.dto.AdminReservationDto;
import com.simplecoding.chargerreservation.admin.dto.AdminStationDto;
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
    @GetMapping("/list")
    public ResponseEntity<List<AdminDto>> getAdminList() {
        return ResponseEntity.ok(adminService.getAdminList());
    }

    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto dto) {
        return ResponseEntity.ok(adminService.createAdmin(dto));
    }

    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    @GetMapping("/{adminId}")
    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdmin(adminId));
    }

    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    @PatchMapping("/{targetId}/role")
    public ResponseEntity<AdminDto> updateAdminRole(
            @PathVariable Long targetId,
            @RequestParam String newRole) {
        return ResponseEntity.ok(adminService.updateAdminRole(targetId, newRole));
    }

    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long targetId) {
        adminService.deleteAdmin(targetId);
        return ResponseEntity.noContent().build();
    }

    // ── 회원 전체 목록 조회 (SUPER / ALL / MEMBER 파트만 가능) ───────────────────
    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberDto>> getMemberList() {
        return ResponseEntity.ok(adminService.getMemberList());
    }

    // ── 회원 상태 변경 (SUPER / ALL / MEMBER 파트만 가능) ────────────────────────
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<AdminMemberDto> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(adminService.updateMemberStatus(memberId, newStatus));
    }

    // ── 패널티 전체 목록 조회 (SUPER / ALL / INQUIRY 파트만 가능) ────────────────
    @GetMapping("/penalties")
    public ResponseEntity<List<AdminPenaltyDto>> getPenaltyList() {
        return ResponseEntity.ok(adminService.getPenaltyList());
    }

    // ── 패널티 취소 (SUPER / ALL / INQUIRY 파트만 가능) ──────────────────────────
    @PatchMapping("/penalties/{penaltyId}")
    public ResponseEntity<AdminPenaltyDto> cancelPenalty(@PathVariable Long penaltyId) {
        return ResponseEntity.ok(adminService.cancelPenalty(penaltyId));
    }

    // ── 예약 전체 목록 조회 (SUPER / ALL / RESERVATION 파트만 가능) ──────────────
    @GetMapping("/reservations")
    public ResponseEntity<List<AdminReservationDto>> getReservationList() {
        return ResponseEntity.ok(adminService.getReservationList());
    }

    // ── 예약 강제 취소 (SUPER / ALL / RESERVATION 파트만 가능) ───────────────────
    @PatchMapping("/reservations/{reservationId}")
    public ResponseEntity<AdminReservationDto> cancelReservation(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(adminService.cancelReservation(reservationId));
    }

    // ── 공지사항 목록 조회 ────────────────────────────────────────────────────────
    @GetMapping("/notices")
    public ResponseEntity<List<AdminNoticeDto>> getNoticeList() {
        return ResponseEntity.ok(adminService.getNoticeList());
    }

    // ── 공지사항 등록 (SUPER 만 가능) ────────────────────────────────────────────
    @PostMapping("/notices")
    public ResponseEntity<AdminNoticeDto> createNotice(@RequestBody AdminNoticeDto dto) {
        return ResponseEntity.ok(adminService.createNotice(dto));
    }

    // ── 공지사항 수정 (SUPER 만 가능) ────────────────────────────────────────────
    @PatchMapping("/notices/{noticeId}")
    public ResponseEntity<AdminNoticeDto> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody AdminNoticeDto dto) {
        return ResponseEntity.ok(adminService.updateNotice(noticeId, dto));
    }

    // ── 공지사항 삭제 (SUPER 만 가능) ────────────────────────────────────────────
    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    // ── 충전소 전체 목록 조회 (SUPER / ALL / CHARGER 파트만 가능) ────────────────
    // GET /api/admin/stations
    @GetMapping("/stations")
    public ResponseEntity<List<AdminStationDto>> getStationList() {
        return ResponseEntity.ok(adminService.getStationList());
    }

    // ── 충전기 목록 조회 (SUPER / ALL / CHARGER 파트만 가능) ─────────────────────
    // GET /api/admin/chargers?statId=충전소ID (statId 없으면 전체 조회)
    @GetMapping("/chargers")
    public ResponseEntity<List<AdminChargerDto>> getChargerList(
            @RequestParam(required = false) String statId) {
        return ResponseEntity.ok(adminService.getChargerList(statId));
    }

    // ── 충전기 상태 변경 (SUPER / ALL / CHARGER 파트만 가능) ─────────────────────
    // PATCH /api/admin/chargers/{statId}/{chargerId}?newStat=4
    @PatchMapping("/chargers/{statId}/{chargerId}")
    public ResponseEntity<AdminChargerDto> updateChargerStat(
            @PathVariable String statId,
            @PathVariable String chargerId,
            @RequestParam String newStat) {
        return ResponseEntity.ok(adminService.updateChargerStat(statId, chargerId, newStat));
    }
}