package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 요청 / 응답 공용 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor  // (adminId, memberId, adminRole, adminPart, name) 전체 생성자 자동 생성
public class AdminDto {

    private Long adminId;
    private Long memberId;
    private String adminRole;   // 관리자 역할 — SUPER / MANAGER / VIEWER
    private String adminPart;   // 담당 파트 — ALL / MEMBER / RESERVATION / CHARGER / INQUIRY
    private String name;        // 관리자 이름 — MEMBER 테이블에서 조회


    // ── 기존 코드 호환용 생성자 (name / adminPart 없는 버전) ────────────────────
    public AdminDto(Long adminId, Long memberId, String adminRole) {
        this.adminId = adminId;
        this.memberId = memberId;
        this.adminRole = adminRole;
    }

    // ── adminPart 포함, name 없는 생성자 (단건 조회 / 역할 변경 / 해제 응답용) ──
    public AdminDto(Long adminId, Long memberId, String adminRole, String adminPart) {
        this.adminId   = adminId;
        this.memberId  = memberId;
        this.adminRole = adminRole;
        this.adminPart = adminPart;
    }


    // ── Admin Entity → DTO 변환 (adminPart 포함, name 없는 버전) ───────────────
    public static AdminDto from(Admin admin) {
        return new AdminDto(
                admin.getAdminId(),
                admin.getMemberId(),
                admin.getAdminRole(),
                admin.getAdminPart()   // adminPart 추가
        );
    }

    // ── Admin Entity + 이름 → DTO 변환 (목록 조회용) ───────────────────────────
    public static AdminDto from(Admin admin, String name) {
        return new AdminDto(
                admin.getAdminId(),
                admin.getMemberId(),
                admin.getAdminRole(),
                admin.getAdminPart(),  // adminPart 추가
                name
        );
    }
}