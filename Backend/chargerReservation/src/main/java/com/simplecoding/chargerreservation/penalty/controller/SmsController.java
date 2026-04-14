package com.simplecoding.chargerreservation.penalty.controller;

import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor // 이걸 쓰면 생성자를 직접 안 써도 스프링이 알아서 주입해줍니다!
@CrossOrigin(origins = "http://localhost:5173")
public class SmsController {

    private final PenaltyService penaltyService;

    @PostMapping("/send-penalty")
    public ResponseEntity<?> sendPenalty(@RequestBody Map<String, Object> data) {
        try {
            // 1. 리액트에서 보낸 데이터 꺼내기 (reservationId와 reason이 핵심!)
            Long reservationId = Long.valueOf(data.get("reservationId").toString());
            String reason = data.get("reason").toString();

            // 2. 서비스 실행! (문자 발송 + DB 상태 CANCELLED_PENALTY로 변경)
            penaltyService.processManualPenalty(reservationId, reason);

            // 3. 성공 보고
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "패널티 처리 및 문자가 발송되었습니다."
            ));
        } catch (IllegalStateException e) {
            // 이미 취소된 예약인 경우 (Validation 에러)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 에러
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "처리 중 오류가 발생했습니다."));
        }
    }
}