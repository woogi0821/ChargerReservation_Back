package com.simplecoding.chargerreservation.reservation.controller;

import com.simplecoding.chargerreservation.reservation.dto.ReservationDto;
import com.simplecoding.chargerreservation.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationDto.Response> createReservation(
            @RequestHeader("X-MemberId") Long memberId,
            @Valid @RequestBody ReservationDto.Request req) {
        log.info("예약생성요청 - 회원 ID:{}, 충전기 : {}",memberId, req.getChargerId());
        ReservationDto.Response response = reservationService.createReservation(memberId, req);
        log.info("예약성공 리액트로 데이터를 반환합니다.(PIN : {})",response.getReservationPin());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/me")
    public ResponseEntity<List<ReservationDto.Response>> getMyReservation(
            @RequestHeader("X-MemberId") Long memberId){
        log.info("예약 목록 조회 요청 - 회원 ID : {}",memberId);
        return ResponseEntity.ok(reservationService.getMyReservations(memberId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @RequestHeader("X-MemberId") Long memberId) {
                log.info("예약 취소 요청 - 예약 ID : {},회원 ID : {}",id, memberId);
                reservationService.cancelReservation(id,memberId);
                return ResponseEntity.ok().build();
            }
    @GetMapping("/available")
    public ResponseEntity<Boolean> isChargerAvailable(
            @RequestParam String chargerId) {
        log.info("충전기 가용 여부 조회 - 충전기 ID : {}",chargerId);
        return ResponseEntity.ok(reservationService.isChargerAvailable(chargerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto.Response> getReservation(
            @PathVariable Long id,
            @RequestHeader("X-MemberId") Long memberId) {
        return ResponseEntity.ok(reservationService.getReservation(id, memberId));
    }
//    @GetMapping("/admin/all")
//    public ResponseEntity<List<ReservationDto.Response>> getAllReservationsForAdmin() {
//        log.info("관리자용 전체 예약 목록 조회 요청");
//        // 서비스에 getAllReservations() 기능을 하나 만드셔야 합니다.
//        return ResponseEntity.ok(reservationService.gerAll);
//    }
}
