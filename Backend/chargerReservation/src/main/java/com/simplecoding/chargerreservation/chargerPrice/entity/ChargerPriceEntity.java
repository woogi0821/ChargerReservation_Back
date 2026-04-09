package com.simplecoding.chargerreservation.chargerPrice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHARGER_PRICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert // 필드 값이 null인 경우 insert 구문에서 제외하여 DB의 Default값(SYSDATE)이 작동하게 함
public class ChargerPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "price_seq")
    @SequenceGenerator(name = "price_seq", sequenceName = "SEQ_PRICE_ID", allocationSize = 1)
    @Column(name = "PRICE_ID")
    private Long priceId; // NUMBER

    @Column(name = "BNM", nullable = false, length = 100)
    private String bnm; // VARCHAR2(100 BYTE), NOT NULL

    @Column(name = "SPEED_TYPE", nullable = false, length = 20)
    private String speedType; // VARCHAR2(20 BYTE), NOT NULL

    @Column(name = "APPLY_YEAR", nullable = false)
    private Integer applyYear; // NUMBER(4,0), NOT NULL

    @Column(name = "SEASON", nullable = false, length = 20)
    private String season; // VARCHAR2(20 BYTE), NOT NULL

    @Column(name = "UNIT_PRICE", nullable = false)
    private Double unitPrice; // NUMBER(7,2), NOT NULL

    @Column(name = "REMARK", length = 200)
    private String remark; // VARCHAR2(200 BYTE), NULL 허용

    @Column(name = "UPDATE_DT", insertable = false, updatable = false)
    @ColumnDefault("SYSDATE") // DB 레벨에서 SYSDATE가 들어가도록 설정
    private LocalDateTime updateDt; // DATE
}