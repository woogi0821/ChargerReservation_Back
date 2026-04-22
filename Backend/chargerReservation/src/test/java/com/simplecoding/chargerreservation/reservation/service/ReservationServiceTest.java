package com.simplecoding.chargerreservation.reservation.service;

import com.simplecoding.chargerreservation.reservation.dto.ReservationDto;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("성공 : RAPID 예약 시 estimatedEndTime이 1시간 뒤로 계산되어야함")
    void createReservation_Rapid_Success() {
        Long memberId = 1L;
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);

        ReservationDto.Request req = ReservationDto.Request.builder()
                .chargerId("CH_001")
                .carNumber("12가1234")
                .chargerType("RAPID")
                .startTime(startTime)
                .build();
        given(reservationRepository.countByMemberIdAndStatusIn(anyLong(), any(List.class)))
                .willReturn(0L);
        given(reservationRepository.isChargerCurrentlyOccupied(any(), any(), any()))
                .willReturn(false);

        Reservation mockSaved = Reservation.builder()
                .chargerId("CH_001")
                .carNumber("12가1234")
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .status("RESERVED")
                .build();
        given(reservationRepository.save(any(Reservation.class))).willReturn(mockSaved);

        ReservationDto.Response response = reservationService.createReservation(memberId,req);

        assertNotNull(response);
        assertEquals(startTime.plusHours(1), response.getEndTime(),"RAPID 충전이므로 예상 종료시간은 1시간 뒤여야 합니다.");
    }
    @Test
    @DisplayName("실패 : 활성 예약이 2건이면 추가 예약 불가")
    void createReservation_Fail_MaxLimit(){
        Long memberId = 1L;
        ReservationDto.Request req = ReservationDto.Request.builder()
                .chargerId("CH_001")
                .chargerType("RAPID")
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
        given(reservationRepository.countByMemberIdAndStatusIn(anyLong(),any(List.class)))
                .willReturn(2L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        ()-> reservationService.createReservation(memberId, req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("실패 : 충전기가 이미 사용 중이면 예약불가")
    void createReservation_Fail_ChargerOccupied(){
        Long memberId = 1L;
        ReservationDto.Request req = ReservationDto.Request.builder()
                .chargerId("CH_001")
                .chargerType("SLOW")
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
        given(reservationRepository.countByMemberIdAndStatusIn(anyLong(),any(List.class)))
                .willReturn(0L);
        given(reservationRepository.isChargerCurrentlyOccupied(any(), any(), any()))
                .willReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.createReservation(memberId, req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("성공 : RESERVED 상태 예약 취소")
    void cancelReservation_Success() {
        Long reservationId = 1L;
        Long memberId = 1L;

        Reservation reservation = Reservation.builder()
                .chargerId("CH_001")
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status("RESERVED")
                .build();

        given(reservationRepository.findByIdAndMemberId(reservationId, memberId))
                .willReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> reservationService.cancelReservation(reservationId, memberId));
        assertEquals("CANCELED", reservation.getStatus());
    }
    @Test
    @DisplayName("실패 : 본인 예약이 아니면 취소 불가")
    void cancelReservation_Fail_NowOwner() {
        given(reservationRepository.findByIdAndMemberId(anyLong(),anyLong()))
                .willReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancelReservation(1L,999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("실패 : CHARGING 중인 예약은 취소 불가")
    void cancelReservation_Fail_Charging() {
        Reservation reservation = Reservation.builder()
                .chargerId("CH_001")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status("CHARGING")
                .build();

        given(reservationRepository.findByIdAndMemberId(anyLong(),anyLong()))
                .willReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancelReservation(1L, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
    @Test
    @DisplayName("성공 : 충전기가 비어있으면 true 반환")
    void isChargerAvailable_True() {
        given(reservationRepository.isChargerCurrentlyOccupied(any(), any(), any()))
                .willReturn(false);
        assertTrue(reservationService.isChargerAvailable("CH_001", "STAT_001"));
    }
    @Test
    @DisplayName("성공 : 충전기가 사용 중이면 false 반환")
    void isChargerAvailable_False() {
        given(reservationRepository.isChargerCurrentlyOccupied(any(), any(), any()))
                .willReturn(true);
        assertFalse(reservationService.isChargerAvailable("CH_001", "STAT_001"));
    }

    @Test
    void getMyReservations() {
    }


    @Test
    void processNoShow() {
    }
}