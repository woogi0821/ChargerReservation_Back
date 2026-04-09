package com.simplecoding.chargerreservation.reservation.entity;

import com.simplecoding.chargerreservation.common.BaseTimeEntity;
import com.simplecoding.chargerreservation.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESERVATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reserv_seq")
    @SequenceGenerator(name = "reserv_seq",sequenceName = "SEQ_RESERVATION_ID",allocationSize = 1)
    @Column(name = "RESERVATION_ID")
    private Long id;

    @Column(name = "MEMBER_ID",nullable = false)
    private Long memberId;

    // '조회용'으로 Member 객체를 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", insertable = false, updatable = false)
//    ****** Merge후 클래스 가져오기 **********
    private Member member;

    @Column(name = "CHARGER_ID", nullable = false, length = 50)
    private String chargerId;

    @Column(name = "CAR_NUMBER", nullable = false, length = 20)
    private String carNumber;

    @Column(name = "RESERVATION_PIN",nullable = false)
    private String reservationPin;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "ACTUAL_END_TIME", nullable = true)
    private LocalDateTime actualEndTime;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "IS_ALERT_SENT", nullable = false, length = 1)
    private String isAlertSent = "N"; // 기본값은 'N' (아직 안 보냄)

    @Version
    @Column(name = "VERSION")
    private Long version;

    @Builder
    public Reservation(Long memberId, String chargerId, String carNumber, String reservationPin,
                       LocalDateTime startTime, LocalDateTime endTime, LocalDateTime actualEndTime,
                       String status, String isAlertSent) { // 👈 맨 뒤에 추가
        this.memberId = memberId;
        this.chargerId = chargerId;
        this.carNumber = carNumber;
        this.reservationPin = reservationPin;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actualEndTime = actualEndTime;
        this.status = status;
        this.isAlertSent = (isAlertSent != null) ? isAlertSent : "N"; // 👈 null 방어 로직
    }

    public void changeStatus(String newStatus){
        this.status = newStatus;
    }
    public void endCharging(String newStatus, LocalDateTime now){
        this.status = newStatus;
        this.actualEndTime = now;
    }
//    고객에게 문자 보낼 시에 db랑 연결하는 역할
    public void markAlertAsSent() {
        this.isAlertSent = "Y";
    }
    // 만약 수동으로 설정할 일이 필요하다면 사용
    public void setIsAlertSent(String isAlertSent) {
        this.isAlertSent = isAlertSent;
    }
}
