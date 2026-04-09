package com.simplecoding.chargerreservation.reservation.controller;

import com.simplecoding.chargerreservation.reservation.dto.KioskDto;
import com.simplecoding.chargerreservation.reservation.service.KioskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final KioskService kioskService;

    @PostMapping("/auth")
    public ResponseEntity<String> authenticateAndStart(@RequestBody @Valid KioskDto.AuthRequest req){
        kioskService.startCharging(req);
        return ResponseEntity.ok("인증성공 충전을 시작합니다.");
    }

    @PostMapping("/end")
    public ResponseEntity<String> endCharging(
            @RequestBody @Valid KioskDto.EndRequest req) {
        kioskService.endCharging(req);
        return ResponseEntity.ok("충전이 종료되었습니다.");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopCharging(
            @RequestBody @Valid KioskDto.StopRequest req) {
        kioskService.stopCharging(req);
        return ResponseEntity.ok("충전이 조기 종료되었습니다.");
    }
}
