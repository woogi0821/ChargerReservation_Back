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
    // 1. 전략은 유지하되, 사용할 generator 이름을 지정합니다.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_seq_gen")
    // 2. 상세 시퀀스 설정을 추가합니다.
    @SequenceGenerator(
            name = "admin_seq_gen",
            sequenceName = "ADMIN_SEQ", // 어제 팀원이 DB에 만든 시퀀스 이름
            allocationSize = 1          // 핵심! DB의 INCREMENT BY 1과 맞춰줍니다.
    )
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