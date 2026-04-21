package com.simplecoding.chargerreservation.notification.repository;

import com.simplecoding.chargerreservation.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // DB에 접근하는 통로
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 회원의 알림 목록을 최신순으로 가져오기
    @Query("SELECT n FROM Notification n WHERE n.member.loginId = :loginId ORDER BY n.createdAt DESC")
    List<Notification> findByMemberLoginId(@Param("loginId") String loginId);

    // 읽지 않은 알림 개수 (memberId 기반 - 토큰에 memberId가 111로 들어있으니 이게 더 정확할 수 있습니다)
    long countByMemberMemberIdAndIsRead(Long memberId, String isRead);
}
