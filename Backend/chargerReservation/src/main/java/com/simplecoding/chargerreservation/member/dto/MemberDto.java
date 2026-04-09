package com.simplecoding.chargerreservation.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "loginPw")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDto {
    private Long memberId;
    private String loginId;
    private	String loginPw;
    private	String email;
    private	String name;
    private	String phone;
    private	String status;
    private	String memberGrade;
    private	String provider;                // LOCAL, KAKAO, GOOGLE
    private	String providerId;              // 소셜 로그인시 고유 식별자
    private	Integer penaltyCount;
    private LocalDateTime suspendedUntil;
}
