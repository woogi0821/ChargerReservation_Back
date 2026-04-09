package com.simplecoding.chargerreservation.reservation.repository;

import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //한개의 계정당 최대 2개의 예약 제한용(매크로 및 독점 방지)
    //memberId의 예약 상태를 확인해서 예약 또는 충전중인 상태의 갯수를 파악함
    Long countByMemberIdAndStatusIn(Long memberId, List<String> statuses);

    @Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false  END 
    FROM Reservation r
    WHERE r.chargerId = :chargerId
    AND(
    r.status = 'CHARGING' OR (r.status = 'RESERVED' AND r.startTime > :graceDeadline)
    )
""")
boolean isChargerCurrentlyOccupied(
        @Param("chargerId") String chargerId,
        @Param("graceDeadline") LocalDateTime graceDeadline
    );

    //노쇼 처리용 - grace period 지난 RESERVED 조회
    List<Reservation> findByStatusAndStartTimeBefore(String status, LocalDateTime deadline);


    Optional<Reservation> findByChargerIdAndStatusAndReservationPin(String chargerId, String status, String reservationPin);
// ─────────────────────────────────────────
// [예약 조회용] 특정 회원의 모든 예약을 최신순으로 가져옴
// JPA 네이밍 규칙: findBy + 필드명 + OrderBy + 필드명 + Desc
// ─────────────────────────────────────────
    List<Reservation> findByMemberIdOrderByStartTimeDesc(Long memberId);

    // ─────────────────────────────────────────
// [예약 취소 - 본인 검증용] reservationId + memberId 동시 조회
// → 남의 예약을 취소하는 것을 방지하기 위해 두 조건 모두 충족해야 조회됨
// ─────────────────────────────────────────
    Optional<Reservation> findByIdAndMemberId(Long id, Long memberId);

    Optional<Reservation> findByChargerIdAndStatus(String chargerId, String status);


    // 예약 시간이 1분 지났고, 아직 알림을 안 보낸 '예약중' 상태인 사람 조회
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.startTime <= :targetTime 
        AND r.status = 'RESERVED' 
        AND r.isAlertSent = 'N'
    """)
    List<Reservation> findNoShowAlertTargets(@Param("targetTime") LocalDateTime targetTime);

    // 예약 시간이 10분 지났고, 아직 충전을 시작 안 한(여전히 RESERVED) 사람 조회
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.startTime <= :targetTime 
        AND r.status = 'RESERVED'
    """)
    List<Reservation> findNoShowCancelTargets(@Param("targetTime") LocalDateTime targetTime);

//    Example<? extends Reservation> id(Long id);
}
