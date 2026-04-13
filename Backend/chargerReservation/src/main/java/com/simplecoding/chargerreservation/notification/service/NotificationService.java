package com.simplecoding.chargerreservation.notification.service;

import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository; // 추가
import com.simplecoding.chargerreservation.notification.dto.NotificationResponseDto; // 추가
import com.simplecoding.chargerreservation.notification.entity.NotiType;
import com.simplecoding.chargerreservation.notification.entity.Notification;
import com.simplecoding.chargerreservation.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; // 추가
import java.util.stream.Collectors; // 추가

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository; // 👈 1. 여기에 추가하면 빨간 줄이 사라집니다!

    // 공통 알림 생성 메서드
    @Transactional
    public void createNotification(Member member, String title, String message, NotiType type, String url) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotiType(type);
        notification.setTargetUrl(url);
        notification.setIsRead("N");
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    // 1. 특정 사용자의 알림 목록 조회 (최신순)
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getMyNotifications(String email) {
        return notificationRepository.findByMemberEmailOrderByCreatedAtDesc(email)
                .stream().map(noti -> NotificationResponseDto.builder()
                        .notiId(noti.getNotiId())
                        .title(noti.getTitle())
                        .message(noti.getMessage())
                        .notiType(noti.getNotiType().name())
                        .targetUrl(noti.getTargetUrl())
                        .isRead(noti.getIsRead())
                        .createdAt(noti.getCreatedAt().toString())
                        .build()
                ).collect(Collectors.toList());
    }

    // 2. 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notiId) {
        Notification noti = notificationRepository.findById(notiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        noti.setIsRead("Y");
    }
}