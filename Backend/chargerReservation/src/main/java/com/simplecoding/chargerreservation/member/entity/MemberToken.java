package com.simplecoding.chargerreservation.member.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TOKEN_JPA")
    @SequenceGenerator(name = "SQ_TOKEN_JPA", sequenceName = "SEQ_TOKEN_ID", allocationSize = 1)
    private Long tokenId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;
    private String refreshToken;
    private String userAgent;
    private String clientIp;
    private LocalDateTime expiresAt;
}
