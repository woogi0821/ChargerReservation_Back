package com.simplecoding.chargerreservation.admin.controller;

import com.simplecoding.chargerreservation.admin.dto.AdminChargerDto;
import com.simplecoding.chargerreservation.admin.dto.AdminDashboardDto;
import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminInquiryDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminNoticeDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.dto.AdminReservationDto;
import com.simplecoding.chargerreservation.admin.dto.AdminStationDto;
import com.simplecoding.chargerreservation.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 API", description = "관리자 페이지 전용 API (JWT 인증 필요 / MEMBER_GRADE = Y)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── 대시보드 ──────────────────────────────────────────────────────────────────
    @Operation(summary = "대시보드 통계 조회", description = "총 회원 수, 오늘 예약 수, 충전소 수, 고장 충전기 수, 미답변 문의 수를 반환합니다")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ── 관리자 ────────────────────────────────────────────────────────────────────
    @Operation(summary = "관리자 전체 목록 조회", description = "등록된 모든 관리자 목록을 반환합니다")
    @GetMapping("/list")
    public ResponseEntity<List<AdminDto>> getAdminList() {
        return ResponseEntity.ok(adminService.getAdminList());
    }

    @Operation(summary = "관리자 등록", description = "SUPER 권한만 가능. 회원을 관리자로 등록합니다")
    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto dto) {
        return ResponseEntity.ok(adminService.createAdmin(dto));
    }

    @Operation(summary = "관리자 단건 조회", description = "adminId 로 특정 관리자 정보를 조회합니다")
    @GetMapping("/{adminId}")
    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdmin(adminId));
    }

    @Operation(summary = "관리자 역할 변경", description = "SUPER 권한만 가능. 관리자의 역할을 변경합니다")
    @PatchMapping("/{targetId}/role")
    public ResponseEntity<AdminDto> updateAdminRole(
            @PathVariable Long targetId,
            @RequestParam String newRole) {
        return ResponseEntity.ok(adminService.updateAdminRole(targetId, newRole));
    }

    // ✅ 추가 — 관리자 파트 변경
    @Operation(summary = "관리자 파트 변경", description = "SUPER 권한만 가능. 관리자의 담당 파트를 변경합니다")
    @PatchMapping("/{targetId}/part")
    public ResponseEntity<AdminDto> updateAdminPart(
            @PathVariable Long targetId,
            @RequestParam String newPart) {
        return ResponseEntity.ok(adminService.updateAdminPart(targetId, newPart));
    }

    @Operation(summary = "관리자 해제", description = "SUPER 권한만 가능. 관리자를 해제하고 MEMBER_GRADE 를 N 으로 변경합니다")
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long targetId) {
        adminService.deleteAdmin(targetId);
        return ResponseEntity.noContent().build();
    }

    // ── 회원 ──────────────────────────────────────────────────────────────────────
    @Operation(summary = "회원 전체 목록 조회", description = "SUPER / MEMBER 파트만 가능. 전체 회원 목록을 반환합니다")
    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberDto>> getMemberList() {
        return ResponseEntity.ok(adminService.getMemberList());
    }

    @Operation(summary = "회원 상태 변경", description = "SUPER / MEMBER 파트만 가능. 회원 상태를 ACTIVE / SUSPENDED / WITHDRAWN 으로 변경합니다")
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<AdminMemberDto> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(adminService.updateMemberStatus(memberId, newStatus));
    }

    // ── 패널티 ────────────────────────────────────────────────────────────────────
    @Operation(summary = "패널티 전체 목록 조회", description = "관리자면 누구나 조회 가능합니다")
    @GetMapping("/penalties")
    public ResponseEntity<List<AdminPenaltyDto>> getPenaltyList() {
        return ResponseEntity.ok(adminService.getPenaltyList());
    }

    @Operation(summary = "패널티 취소", description = "SUPER / PENALTY 파트만 가능. 패널티를 취소 처리합니다")
    @PatchMapping("/penalties/{penaltyId}")
    public ResponseEntity<AdminPenaltyDto> cancelPenalty(@PathVariable Long penaltyId) {
        return ResponseEntity.ok(adminService.cancelPenalty(penaltyId));
    }

    @Operation(summary = "패널티 삭제", description = "SUPER / PENALTY 파트만 가능. 패널티를 삭제합니다")
    @DeleteMapping("/penalties/{penaltyId}")
    public ResponseEntity<Void> deletePenalty(@PathVariable Long penaltyId) {
        adminService.deletePenalty(penaltyId);
        return ResponseEntity.noContent().build();
    }

    // ── 예약 ──────────────────────────────────────────────────────────────────────
    @Operation(summary = "예약 전체 목록 조회", description = "관리자면 누구나 조회 가능합니다")
    @GetMapping("/reservations")
    public ResponseEntity<List<AdminReservationDto>> getReservationList() {
        return ResponseEntity.ok(adminService.getReservationList());
    }

    @Operation(summary = "예약 강제 취소", description = "SUPER / RESERVATION 파트만 가능. 예약을 강제 취소합니다")
    @PatchMapping("/reservations/{reservationId}")
    public ResponseEntity<AdminReservationDto> cancelReservation(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(adminService.cancelReservation(reservationId));
    }

    @Operation(summary = "예약 삭제", description = "SUPER / RESERVATION 파트만 가능. 예약을 삭제합니다")
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long reservationId) {
        adminService.deleteReservation(reservationId);
        return ResponseEntity.noContent().build();
    }

    // ── 공지사항 ──────────────────────────────────────────────────────────────────
    @Operation(summary = "공지사항 목록 조회", description = "관리자면 누구나 조회 가능합니다. 삭제된 공지는 제외됩니다")
    @GetMapping("/notices")
    public ResponseEntity<Page<AdminNoticeDto>> getNoticeList(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminService.getNoticeList(page));
    }

    @Operation(summary = "공지사항 등록", description = "SUPER / INQUIRY 파트만 가능. 새 공지사항을 등록합니다")
    @PostMapping("/notices")
    public ResponseEntity<AdminNoticeDto> createNotice(@RequestBody AdminNoticeDto dto) {
        return ResponseEntity.ok(adminService.createNotice(dto));
    }

    @Operation(summary = "공지사항 수정", description = "SUPER / INQUIRY 파트만 가능. 공지사항을 수정합니다")
    @PatchMapping("/notices/{noticeId}")
    public ResponseEntity<AdminNoticeDto> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody AdminNoticeDto dto) {
        return ResponseEntity.ok(adminService.updateNotice(noticeId, dto));
    }

    @Operation(summary = "공지사항 삭제", description = "SUPER / INQUIRY 파트만 가능. 공지사항을 소프트 삭제합니다")
    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    // ── 충전소 / 충전기 ───────────────────────────────────────────────────────────
    @Operation(summary = "충전소 목록 조회", description = "관리자면 누구나 조회 가능합니다. keyword 파라미터로 검색 가능합니다")
    @GetMapping("/stations")
    public ResponseEntity<List<AdminStationDto>> getStationList(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminService.getStationList(keyword));
    }

    @Operation(summary = "충전기 목록 조회", description = "관리자면 누구나 조회 가능합니다. statId 파라미터로 특정 충전소 충전기만 조회 가능합니다")
    @GetMapping("/chargers")
    public ResponseEntity<List<AdminChargerDto>> getChargerList(
            @RequestParam(required = false) String statId) {
        return ResponseEntity.ok(adminService.getChargerList(statId));
    }

    @Operation(summary = "충전기 상태 변경", description = "SUPER / CHARGER 파트만 가능. 충전기 상태를 변경합니다")
    @PatchMapping("/chargers/{statId}/{chargerId}")
    public ResponseEntity<AdminChargerDto> updateChargerStat(
            @PathVariable String statId,
            @PathVariable String chargerId,
            @RequestParam String newStat) {
        return ResponseEntity.ok(adminService.updateChargerStat(statId, chargerId, newStat));
    }

    // ── 문의 ──────────────────────────────────────────────────────────────────────
    @Operation(summary = "문의 전체 목록 조회", description = "관리자면 누구나 조회 가능합니다")
    @GetMapping("/inquiries")
    public ResponseEntity<List<AdminInquiryDto>> getInquiryList() {
        return ResponseEntity.ok(adminService.getInquiryList());
    }

    @Operation(summary = "문의 답변 등록", description = "SUPER / INQUIRY 파트만 가능. 문의에 답변을 등록합니다")
    @PostMapping("/inquiries/{inquiryId}/answer")
    public ResponseEntity<AdminInquiryDto> answerInquiry(
            @PathVariable Long inquiryId,
            @RequestBody AdminInquiryDto dto) {
        return ResponseEntity.ok(adminService.answerInquiry(inquiryId, dto));
    }
}