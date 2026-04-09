package com.simplecoding.chargerreservation.penalty.repository;

import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface PenaltyRepository extends JpaRepository<PenaltyHistory, Long> {
    // 특정 회원의 전체 패널티 내역 조회 (마이페이지용)
    List<PenaltyHistory> findByMemberId(String memberId);

    // [핵심] 특정 시간 범위 내에 특정 회원의 '최종 단계(3단계)' 기록이 있는지 확인
    // 예약 팀원이 "오늘(00시~23시) 이 사람 3단계 찍혔어?"라고 물어볼 때 사용합니다.
    boolean existsByMemberIdAndNudgeCountAndInsertTimeBetween(String memberId, Integer nudgeCount, LocalDateTime start, LocalDateTime end);
}
