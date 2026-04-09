package com.simplecoding.chargerreservation.penalty.dto;
// (데이터를 줄 때)
import com.simplecoding.chargerreservation.penalty.entity.PenaltyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyResponseDto {
    private Long penaltyId;
    private String memberId;
    private String carNumber;
    private String reason;
    private Integer nudgeCount;
    private PenaltyStatus status;
    private String notiSentYn;

    // BaseTimeEntity가 채워준 시간을 여기서 활용합니다.
    private LocalDateTime insertTime;
}
