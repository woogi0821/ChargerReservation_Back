package com.simplecoding.chargerreservation.notification.entity;

import com.simplecoding.chargerreservation.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "noti_seq")
    @SequenceGenerator(name = "noti_seq", sequenceName = "NOTI_SEQ", allocationSize = 1)
    private Long notiId;

    @ManyToOne(fetch = FetchType.LAZY) // 여러개의 알림은 한 회원에게 속한다
    @JoinColumn(name = "MEMBER_ID")
    private Member member; // 알림 대상자

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotiType notiType; // PENALTY, RESERVATION 등으로 저장하고 나중에 알람 종류가 늘어도 데이터가 꼬이지 않음

    private String targetUrl;

    @Column(columnDefinition = "CHAR(1) DEFAULT 'N'") // 생성되면 바로 안 읽은 상태가되는 형태
    private String isRead = "N";

    private LocalDateTime createdAt = LocalDateTime.now();
}
