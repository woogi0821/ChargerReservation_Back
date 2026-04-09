package com.simplecoding.chargerreservation.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ReservationDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request{

        @NotBlank(message = "충전기를 선택해주세요.")
        private String chargerId;

        @NotBlank(message = "충전기 타입을 확인 해주세요.")
        private String chargerType;

        @NotBlank(message = "차량 번호를 입력해주세요.")
        private String carNumber;

        @NotNull(message = "예약 시작 시간을 선택해주세요.")
        @Future(message = "예약은 현재 시간 이후로만 가능합니다.")
        private LocalDateTime startTime;

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String chargerId;
        private String carNumber;
        private String reservationPin;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private LocalDateTime actualEndTime;
        private String chargerType;
        private String isAlertSent; // 👈 관리자 페이지에서 'Y/N' 확인용으로 추가
    }
}
