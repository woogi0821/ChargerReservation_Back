package com.simplecoding.chargerreservation.notification.repository;

import com.simplecoding.chargerreservation.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 회원의 알림 목록을 최신순으로 가져오기
    List<Notification> findByMemberEmailOrderByCreatedAtDesc(String email); // 쿼리문 : 회원의 이메일로 데이터를 찾아라, 작성일시를 기준으로 내림차순(최신) 정렬

    // 읽지 않은 알림 개수 확인 (종 모양 배지에 숫자 띄울 때 사용)
    long countByMemberMemberIdAndIsRead(Long memberId, String isRead);
}
