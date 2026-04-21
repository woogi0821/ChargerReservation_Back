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
    @SequenceGenerator(
            name = "admin_seq_gen",
            sequenceName = "ADMIN_SEQ",
            initialValue = 1,
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_seq_gen")
    @Column(name = "ADMIN_ID")
    private Long adminId;

    @Column(name = "MEMBER_ID", nullable = false)
    private long memberId;

    // nullable = false — 관리자 권한은 필수값
    @Column(name = "ADMIN_ROLE", length = 20, nullable = false)
    private String adminRole;

    // 담당 파트 — ALL / MEMBER / RESERVATION / CHARGER / INQUIRY
    // DEFAULT 'ALL' — 기본값은 전체 접근
    @Column(name = "ADMIN_PART", length = 20)
    private String adminPart;

    public Admin(Long memberId, String adminRole) {
        this.memberId = memberId;
        this.adminRole = adminRole;
        this.adminPart = "ALL"; // 기본값 ALL
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