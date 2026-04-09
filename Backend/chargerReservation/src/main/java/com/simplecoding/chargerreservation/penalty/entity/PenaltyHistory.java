package com.simplecoding.chargerreservation.penalty.entity;

import com.simplecoding.chargerreservation.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PenaltyHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long penaltyId;

    private String memberId;
    private Long reservationId;
    private String carNumber;
    private String reason;
    private int nudgeCount;    // 독촉 문자 발송 횟수

    // Enum을 적용한 상태값 (숫자나 문자보다 안전함)
    @Enumerated(EnumType.STRING)
    private PenaltyStatus status = PenaltyStatus.ACTIVE;

    // 문자 발송 여부 (Y/N)
    private String notiSentYn = "N";
}
