package com.simplecoding.chargerreservation.reservation.service;

//충전기 상태 관련 메모
//DONE = 예약 시간이 다 되어 자연 종료된 경우
//COMPLETED = 사용자가 키오스크에서 직접 조기 종료한 경우

import com.simplecoding.chargerreservation.penalty.dto.PenaltyRequestDto;
import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import com.simplecoding.chargerreservation.reservation.dto.KioskDto;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import com.simplecoding.chargerreservation.websocket.ChargerSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class KioskService {

    private final ReservationRepository reservationRepository;
    private final ChargerSocketController chargerSocketController;
    private final PenaltyService penaltyService;

    @Transactional
    public void startCharging(KioskDto.AuthRequest req) {
        try {
            Reservation reservation = reservationRepository.findByChargerIdAndStatusAndReservationPin(
                    req.getChargerId(), "RESERVED", req.getPin()
            ).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "핀번호가 일치하지 않거나 유효한 예약이 없습니다."));
            reservation.changeStatus("CHARGING");

            if (req.getStatId() == null) {
                log.warn("statId가 null입니다 - WebSocket 푸시 생략 (chargerId : {})", req.getChargerId());
            } else {
                chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "CHARGING"); // ← DONE → CHARGING 수정
            }
            log.info("웹소켓 푸시 완료 - chargerId : {}, status : CHARGING", req.getChargerId());

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // 노쇼 스케줄러가 동시에 NO_SHOW로 바꾼 경우
            throw new ResponseStatusException(HttpStatus.CONFLICT, "예약 상태가 변경되었습니다. 다시 시도해주세요.");
        }
    }
    //충전 종료
    @Transactional
    public void endCharging(KioskDto.EndRequest req){
        if (req.getStatId() == null) {
            log.warn("statId가 null입니다 - WebSocket 푸시 생략 (chargerId : {})", req.getChargerId());
        } else {
            chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "DONE");
        }

        //해당 충전기의 CHARGING 상태 예약 조회
        Reservation reservation = reservationRepository
                .findByChargerIdAndStatus(req.getChargerId(), "CHARGING")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "충전 중인 예약을 찾을 수 없습니다."));
        //Entity의 endCharging()호출 -> status=DONE + actualEndTime = 현재시간 기록
        reservation.endCharging("DONE", LocalDateTime.now());
        sendPenaltyNotice(reservation, 1, "충전 시간 만료");

        //키오스크의 상태를 WebSocket 푸쉬
        chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "DONE");
        log.info("충전 종료 - 충전기 : {}", req.getChargerId());
    }
    //충전 종료 버튼 -> 실제 종료 시각 기록 + 상태변경
    //물리적으로 충전기를 뽑는 행위를 버튼으로 표현
    @Transactional
    public void stopCharging(KioskDto.StopRequest req){
        if (req.getStatId() == null){
            log.warn("statId가 null입니다. - WebSocket 푸시 생략 (chargerId : {})", req.getChargerId());
        } else {
            chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "COMPLETED");
        }
        Reservation reservation = reservationRepository
                .findByChargerIdAndStatusAndReservationPin(req.getChargerId(),"CHARGING", req.getPin()
                ).orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST,"진행중인 충전이 없거나 핀번호가 일치하지 않습니다."));
        //endCharging()으로 상태 + actualEndTime 동시 기록
        reservation.endCharging("COMPLETED", LocalDateTime.now());
        sendPenaltyNotice(reservation, 1, "사용자 직접 종료 및 출차 확인");
        chargerSocketController.pushStatus(req.getStatId(), req.getChargerId(), "COMPLETED");
        log.info("충전 조기 종료 - 충전기 : {}", req.getChargerId());
    }
//     [공통 로직] 패널티 서비스 호출 및 문자 발송 요청
private void sendPenaltyNotice(Reservation r, int step, String reason) {
    try {
        PenaltyRequestDto penaltyDto = new PenaltyRequestDto();
        penaltyDto.setMemberId(String.valueOf(r.getMemberId()));
        penaltyDto.setReservationId(r.getId());
        penaltyDto.setReason(reason);

        penaltyService.processPenaltyStep(penaltyDto, step);
    } catch (Exception e) {
        log.warn("⚠️ 패널티 서비스 호출 실패 (예약ID: {}): {}", r.getId(), e.getMessage());
    }
}
    public KioskDto.StatusResponse getChargerStatus(String chargerId){
        Optional<Reservation> opt = reservationRepository
                .findTopByChargerIdAndStatusIn(chargerId, List.of("RESERVED", "CHARGING"));

        if (opt.isEmpty()) {
            return KioskDto.StatusResponse.builder()
                    .chargerId(chargerId)
                    .status("AVAILABLE")
                    .build();
        }
        Reservation r = opt.get();
        return KioskDto.StatusResponse.builder()
                .chargerId(chargerId)
                .status(r.getStatus())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .build();
    }
}
