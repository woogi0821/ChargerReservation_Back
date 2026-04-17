package com.simplecoding.chargerreservation.reservation.controller;

import com.simplecoding.chargerreservation.reservation.dto.ReservationDto;
import com.simplecoding.chargerreservation.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "예약 API", description = "충전소 예약 생성 / 조회 / 취소 API")
@Log4j2
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 생성", description = "회원 ID 와 충전기 정보로 새 예약을 생성합니다. 성공 시 예약 PIN 번호가 발급됩니다")
    @PostMapping
    public ResponseEntity<ReservationDto.Response> createReservation(
            @RequestHeader("X-MemberId") Long memberId,
            @Valid @RequestBody ReservationDto.Request req) {
        log.info("예약생성요청 - 회원 ID:{}, 충전기 : {}", memberId, req.getChargerId());
        ReservationDto.Response response = reservationService.createReservation(memberId, req);
        log.info("예약성공 리액트로 데이터를 반환합니다.(PIN : {})", response.getReservationPin());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 예약 목록 조회", description = "현재 로그인한 회원의 예약 목록을 전체 조회합니다")
    @GetMapping("/me")
    public ResponseEntity<List<ReservationDto.Response>> getMyReservation(
            @RequestHeader("X-MemberId") Long memberId) {
        log.info("예약 목록 조회 요청 - 회원 ID : {}", memberId);
        return ResponseEntity.ok(reservationService.getMyReservations(memberId));
    }

    @Operation(summary = "예약 취소", description = "예약 ID 와 회원 ID 로 본인 예약을 취소합니다")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @RequestHeader("X-MemberId") Long memberId) {
        log.info("예약 취소 요청 - 예약 ID : {}, 회원 ID : {}", id, memberId);
        reservationService.cancelReservation(id, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "충전기 가용 여부 조회", description = "chargerId 로 해당 충전기의 현재 예약 가능 여부를 조회합니다")
    @GetMapping("/available")
    public ResponseEntity<Boolean> isChargerAvailable(@RequestParam String chargerId) {
        log.info("충전기 가용 여부 조회 - 충전기 ID : {}", chargerId);
        return ResponseEntity.ok(reservationService.isChargerAvailable(chargerId));
    }

    @Operation(summary = "예약 단건 조회", description = "예약 ID 와 회원 ID 로 본인 예약 상세 정보를 조회합니다")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto.Response> getReservation(
            @PathVariable Long id,
            @RequestHeader("X-MemberId") Long memberId) {
        return ResponseEntity.ok(reservationService.getReservation(id, memberId));
    }
}