package com.simplecoding.chargerreservation.notification.entity;

import com.simplecoding.chargerreservation.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "noti_seq")
    @SequenceGenerator(name = "noti_seq", sequenceName = "NOTI_SEQ", allocationSize = 1)
    private Long notiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member; // 알림 대상자

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotiType notiType; // PENALTY, RESERVATION 등

    private String targetUrl;

    @Column(columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String isRead = "N";

    private LocalDateTime createdAt = LocalDateTime.now();
}
