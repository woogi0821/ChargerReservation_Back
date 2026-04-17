package com.simplecoding.chargerreservation.penalty.controller;

import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "SMS API", description = "패널티 문자 발송 API")
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SmsController {

    private final PenaltyService penaltyService;

    @Operation(summary = "패널티 문자 발송", description = "예약 ID 와 사유를 받아 패널티를 처리하고 문자를 발송합니다")
    @PostMapping("/send-penalty")
    public ResponseEntity<?> sendPenalty(@RequestBody Map<String, Object> data) {
        try {
            Long reservationId = Long.valueOf(data.get("reservationId").toString());
            String reason = data.get("reason").toString();

            penaltyService.processManualPenalty(reservationId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "패널티 처리 및 문자가 발송되었습니다."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "처리 중 오류가 발생했습니다."
            ));
        }
    }
}