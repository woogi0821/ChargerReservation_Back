package com.simplecoding.chargerreservation.penalty.dto;
// (데이터를 받을 때)
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyRequestDto {
    private String memberId;
    private Long reservationId;
    private String carNumber;
    private String reason;
}
