package com.simplecoding.chargerreservation.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inquiry_seq")
    @SequenceGenerator(name = "inquiry_seq", sequenceName = "SEQ_INQUIRY_ID", allocationSize = 1)
    @Column(name = "INQUIRY_ID")
    private Long inquiryId;

    @Column(name = "ADMIN_ID")
    private Long adminId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "STAT_ID", length = 50)
    private String statId;

    @Column(name = "CHARGER_ID", length = 50)
    private String chargerId;

    @Column(name = "CATEGORY", length = 20, nullable = false)
    private String category;

    @Column(name = "TITLE", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Lob
    @Column(name = "ANSWER_CONTENT")
    private String answerContent;

    @Column(name = "ANSWER_AT")
    private LocalDateTime answerAt;

    @Column(name = "INSERT_TIME")
    private LocalDateTime insertTime;
}