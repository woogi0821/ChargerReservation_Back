package com.simplecoding.chargerreservation.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class KioskDto {
    @Getter
    public static class AuthRequest {
        @NotBlank(message = "충전기 ID가 필요합니다.")
        private String chargerId;

        @NotBlank(message = "핀번호 4자리를 입력 해주세요.")
        private String pin;

        private String statId;
    }

    @Getter
    public static class StopRequest {
        @NotBlank(message = "충전기 ID가 필요합니다.")
        private String chargerId;

        @NotBlank(message = "핀번호 4자리를 입력 해주세요.")
        private String pin;

        private String statId;
    }

    @Getter
    public static class EndRequest {
        @NotBlank(message = "충전기 ID가 필요합니다.")
        private String chargerId;

        private String statId;
    }

    @Getter
    @Builder
    public static class StatusResponse {
        private String chargerId;
        private String status;
        private String statId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
