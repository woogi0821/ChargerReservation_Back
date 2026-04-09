package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 어드민 페이지에서 회원 목록 조회 시 사용하는 DTO
// MemberDto 와 구분하기 위해 AdminMemberDto 로 명명
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
                member.getSuspendedUntil()
        );
    }
}