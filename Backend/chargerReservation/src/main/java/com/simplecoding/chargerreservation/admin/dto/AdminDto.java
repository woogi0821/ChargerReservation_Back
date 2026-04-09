package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 요청 / 응답 공용 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDto {

    private Long adminId;
    private Long memberId;
    private String adminRole;
    private String name;      // 관리자 이름 추가 — MEMBER 테이블에서 가져옴

    // name 없는 생성자 — 기존 코드 호환용
    public AdminDto(Long adminId, Long memberId, String adminRole) {
        this.adminId = adminId;
        this.memberId = memberId;
        this.adminRole = adminRole;
    }

    // Admin Entity → DTO 변환 (이름 없는 버전 — 기존 호환)
    public static AdminDto from(Admin admin) {
        return new AdminDto(
                admin.getAdminId(),
                admin.getMemberId(),
                admin.getAdminRole()
        );
    }

    // Admin Entity + 이름 → DTO 변환 (목록 조회용)
    public static AdminDto from(Admin admin, String name) {
        return new AdminDto(
                admin.getAdminId(),
                admin.getMemberId(),
                admin.getAdminRole(),
                name
        );
    }
}