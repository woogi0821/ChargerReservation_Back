package com.simplecoding.chargerreservation.member.entity;

import com.simplecoding.chargerreservation.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER")
@SequenceGenerator(
    name = "SQ_MEMBER_JPA",
    sequenceName = "SEQ_MEMBER_ID",
    allocationSize = 1
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "loginPw")
@EqualsAndHashCode(of = "memberId", callSuper = false)
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MEMBER_JPA")
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String loginId;
    @Column(nullable = true)
    private	String loginPw;     // 소셜 로그인 시 비밀번호x -> nullable을 true로 변경 (기본값)
    @Column(nullable = false, unique = true)
    private	String email;
    @Column(nullable = false)
    private	String name;
    @Column(nullable = false)
    private	String phone;

    @Builder.Default
    private	String status = "ACTIVE";

    @Builder.Default
    private	String memberGrade = "N";

    // 소셜 로그인을 구분하기 위한 필드 (LOCAL, KAKAO, GOOGLE 등)
    @Builder.Default
    private	String provider = "LOCAL";
    private	String providerId;

    @Builder.Default
    private	Integer penaltyCount = 0;
    private	LocalDateTime suspendedUntil;

    // 소셜 로그인 사용자 정보 업데이트 메서드(더티체킹용)
    public Member updateSocialInfo(String name, String phone) {
        this.name = name;
        if (phone != null) {
            this.phone = phone;
        }
        return this;
    }

    public void updateMember(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void updatePassword(String encodedPassword) {
        this.loginPw = encodedPassword;
    }

}
