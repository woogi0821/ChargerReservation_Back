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

@SequenceGenerator(
        // 1. 시퀀스 생성기 선언 (이름과 실제 DB 시퀀스명을 매핑)
        name = "PENALTY_SEQ_GENERATOR",
        sequenceName = "SEQ_PENALTY_ID", // 실제 오라클 DB에 생성될 시퀀스 이름
        initialValue = 1,
        allocationSize = 1
)
public class PenaltyHistory extends BaseTimeEntity {
    @Id
    // 2. 위에서 만든 생성기를 사용하도록 설정
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "PENALTY_SEQ_GENERATOR"
    )
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
