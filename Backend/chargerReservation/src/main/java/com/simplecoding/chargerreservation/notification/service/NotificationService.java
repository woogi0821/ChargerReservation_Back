package com.simplecoding.chargerreservation.notification.service;

import com.simplecoding.chargerreservation.common.SmsService;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository; // 추가
import com.simplecoding.chargerreservation.notification.dto.NotificationResponseDto; // 추가
import com.simplecoding.chargerreservation.notification.entity.NotiType;
import com.simplecoding.chargerreservation.notification.entity.Notification;
import com.simplecoding.chargerreservation.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; // 추가
import java.util.stream.Collectors; // 추가

@Slf4j
@Service
@RequiredArgsConstructor //
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository; // 👈 1. 여기에 추가하면 빨간 줄이 사라집니다!
    private final SmsService smsService;

    // 공통 알림 생성 메서드
    @Transactional
    public void createNotification(Member member, String title, String message, NotiType type, String url) { // 알림을 db에 저장
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
    @Transactional(readOnly = true) // 이 메서드가 끝날 때 변경사항을 자동으로 반영
    public List<NotificationResponseDto> getMyNotifications(String loginId) {


        // ✅ Repository의 새 메서드 호출
        return notificationRepository.findByMemberLoginId(loginId)
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

    // 노쇼 발생 시 DB 알림 저장 + 문자 발송(로그)을 한 번에 처리하는 메서드
    @Transactional
    public void sendNoShowSms(Member member) {
        // 1. 웹 알림 저장 (이미 만들어둔 메서드 활용)
        // 사용자가 로그인했을 때 종 모양에 뜨게 합니다.
        createNotification(
                member,
                "⚠ 노쇼 안내",
                "예약 시간이 경과하여 노쇼 처리되었습니다.",
                NotiType.NOSHOW,
                "/mypage"
        );

        // 2. 실제 문자 발송 (SmsService 활용)
        try {
            // PenaltyService에서 쓰던 형식을 그대로 빌려옵니다.
            smsService.sendPenaltyMessage(
                    member.getPhone(),
                    member.getName(),
                    "예약 시간 15분 경과 노쇼",
                    "오늘 자정"
            );
            log.info(" 노쇼 SMS 발송 완료: {}", member.getName());
        } catch (Exception e) {
            log.error(" 노쇼 SMS 발송 실패: {}", e.getMessage());
        }
    }
}