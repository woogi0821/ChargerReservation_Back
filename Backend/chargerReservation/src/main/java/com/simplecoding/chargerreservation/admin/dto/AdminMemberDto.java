package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberDto {

    private Long memberId;
    private String loginId;
    private String name;
    private String email;
    private String phone;
    private String status;
    private Integer penaltyCount;
    private LocalDateTime suspendedUntil;
    private String memberGrade; // ✅ 추가

    // Member Entity → AdminMemberDto 변환
    public static AdminMemberDto from(Member member) {
        return new AdminMemberDto(
                member.getMemberId(),
                member.getLoginId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getStatus(),
                member.getPenaltyCount(),
                member.getSuspendedUntil(),
                member.getMemberGrade() // ✅ 추가
        );
    }
}