package com.simplecoding.chargerreservation.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_VERIFICATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class EmailVerification {
    @Id
    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "AUTH_CODE", nullable = false, length = 6)
    private String authCode;

    @Column(name = "IS_VERIFIED", nullable = false, length = 1)
    private String isVerified = "N";

    @Column(name = "RETRY_COUNT", nullable = false)
    private int retryCount = 0;

    @Column(name = "EXPIRED_AT", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "LAST_SENT_AT")
    private LocalDateTime lastSentAt;

    @Builder
    public EmailVerification(String email, String authCode, int validityMinutes) {
        this.email = email;
        this.authCode = authCode;
        this.isVerified = "N";
        this.retryCount = 0;
        this.expiredAt = LocalDateTime.now().plusMinutes(validityMinutes);
        this.lastSentAt = LocalDateTime.now();
    }

    // 사용자가 입력한 인증 코드 검증
    public void verifyCode(String code) {
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new IllegalStateException("인증 시간이 만료되었습니다.");
        }
        if (this.retryCount >= 5) {
            throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 다시 요청해주세요.");
        }

        if (!this.authCode.equals(code)) {
            this.retryCount++;
            throw new IllegalArgumentException("인증 번호가 일치하지 않습니다.");
        }

        this.isVerified = "Y";
    }

    // 인증번호 재발송 시 데이터 초기화
    public void renewVerification(String newCode, int validityMinutes) {
        this.authCode = newCode;
        this.isVerified = "N";
        this.retryCount = 0;
        this.expiredAt = LocalDateTime.now().plusMinutes(validityMinutes);
    }

}
