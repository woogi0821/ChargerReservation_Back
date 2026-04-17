package com.simplecoding.chargerreservation.reservation.controller;

import com.simplecoding.chargerreservation.reservation.dto.KioskDto;
import com.simplecoding.chargerreservation.reservation.service.KioskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "키오스크 API", description = "충전소 키오스크 인증 / 충전 시작 / 종료 / 상태 조회 API")
@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final KioskService kioskService;

    @Operation(summary = "키오스크 인증 및 충전 시작", description = "예약 PIN 번호로 인증 후 충전을 시작합니다")
    @PostMapping("/auth")
    public ResponseEntity<String> authenticateAndStart(@RequestBody @Valid KioskDto.AuthRequest req) {
        kioskService.startCharging(req);
        return ResponseEntity.ok("인증성공 충전을 시작합니다.");
    }

    @Operation(summary = "충전 정상 종료", description = "충전을 정상적으로 종료합니다")
    @PostMapping("/end")
    public ResponseEntity<String> endCharging(@RequestBody @Valid KioskDto.EndRequest req) {
        kioskService.endCharging(req);
        return ResponseEntity.ok("충전이 종료되었습니다.");
    }

    @Operation(summary = "충전 조기 종료", description = "충전을 예약 시간 전에 조기 종료합니다")
    @PostMapping("/stop")
    public ResponseEntity<String> stopCharging(@RequestBody @Valid KioskDto.StopRequest req) {
        kioskService.stopCharging(req);
        return ResponseEntity.ok("충전이 조기 종료되었습니다.");
    }

    @Operation(summary = "충전기 상태 조회", description = "chargerId 로 특정 충전기의 현재 상태를 조회합니다")
    @GetMapping("/status/{chargerId}")
    public ResponseEntity<KioskDto.StatusResponse> getChargerStatus(@PathVariable String chargerId) {
        return ResponseEntity.ok(kioskService.getChargerStatus(chargerId));
    }
}