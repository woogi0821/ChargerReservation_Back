package com.simplecoding.chargerreservation.admin.entity;

import com.simplecoding.chargerreservation.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ADMIN")
@Getter
@NoArgsConstructor
public class Admin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "ADMIN_ID")
    private Long adminId;

    @Column(name = "MEMBER_ID", nullable = false)
    private long memberId;

    @Column(name = "ADMIN_ROLE", length = 20, nullable = false)
    private String adminRole;

    @Column(name = "ADMIN_PART", length = 20)
    private String adminPart;

    // 기존 생성자 유지
    public Admin(Long memberId, String adminRole) {
        this.memberId = memberId;
        this.adminRole = adminRole;
        this.adminPart = "ALL";
    }

    // ✅ adminPart 받는 생성자 추가
    public Admin(Long memberId, String adminRole, String adminPart) {
        this.memberId = memberId;
        this.adminRole = adminRole;
        this.adminPart = adminPart != null ? adminPart : "ALL";
    }

    // 역할 변경 메서드
    public void updateRole(String newRole) {
        this.adminRole = newRole;
    }

    // 파트 변경 메서드
    public void updatePart(String newPart) {
        this.adminPart = newPart;
    }
}