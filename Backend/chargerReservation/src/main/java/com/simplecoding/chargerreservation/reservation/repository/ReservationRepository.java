package com.simplecoding.chargerreservation.reservation.repository;

import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ✅ 추가 — 메인페이지 통계용 예약 중 카운트
    long countByStatus(String status);

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

    List<Reservation> findByStatusAndStartTimeBefore(String status, LocalDateTime deadline);

    Optional<Reservation> findByChargerIdAndStatusAndReservationPin(String chargerId, String status, String reservationPin);

    List<Reservation> findByMemberIdOrderByStartTimeDesc(Long memberId);

    Optional<Reservation> findByIdAndMemberId(Long id, Long memberId);

    Optional<Reservation> findByChargerIdAndStatus(String chargerId, String status);

    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.startTime <= :targetTime 
        AND r.status = 'RESERVED' 
        AND r.isAlertSent = 'N'
    """)
    List<Reservation> findNoShowAlertTargets(@Param("targetTime") LocalDateTime targetTime);

    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.startTime <= :targetTime 
        AND r.status = 'RESERVED'
    """)
    List<Reservation> findNoShowCancelTargets(@Param("targetTime") LocalDateTime targetTime);

    List<Reservation> findByStatusAndEndTimeBefore(String status, LocalDateTime endTime);

    Optional<Reservation> findTopByChargerIdAndStatusIn(String chargerId, List<String> statues);

    List<Reservation> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'CANCELLED' WHERE r.memberId = :memberId AND r.status = 'RESERVED'")
    void cancelAllByMemberId(@Param("memberId") Long memberId);
}