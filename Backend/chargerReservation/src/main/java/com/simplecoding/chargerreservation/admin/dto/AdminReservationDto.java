package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 어드민 페이지에서 예약 목록 조회 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminReservationDto {

    private Long reservationId;
    private Long memberId;
    private String chargerId;
    private String carNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private String status;

    // Reservation Entity → AdminReservationDto 변환
    public static AdminReservationDto from(Reservation reservation) {
        return new AdminReservationDto(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getChargerId(),
                reservation.getCarNumber(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getActualEndTime(),
                reservation.getStatus()
        );
    }
}