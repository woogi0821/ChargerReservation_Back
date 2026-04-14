package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 어드민 페이지에서 패널티 목록 조회 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminPenaltyDto {

    private Long penaltyId;
    private String memberId;
    private Long reservationId;
    private String carNumber;
    private String reason;
    private int nudgeCount;
    private PenaltyStatus status;
    private String notiSentYn;
    private LocalDateTime insertTime;

    // PenaltyHistory Entity → AdminPenaltyDto 변환
    public static AdminPenaltyDto from(PenaltyHistory penalty) {
        return new AdminPenaltyDto(
                penalty.getPenaltyId(),
                penalty.getMemberId(),
                penalty.getReservationId(),
                penalty.getCarNumber(),
                penalty.getReason(),
                penalty.getNudgeCount(),
                penalty.getStatus(),
                penalty.getNotiSentYn(),
                penalty.getInsertTime()
        );
    }
}