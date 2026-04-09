package com.simplecoding.chargerreservation.notice.entity;

import com.simplecoding.chargerreservation.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class NoticeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_seq")
    @SequenceGenerator(name = "notice_seq", sequenceName = "SEQ_NOTICE_ID", allocationSize = 1)
    @Column(name = "NOTICE_ID")
    private Long noticeId;

    @Column(name = "TITLE", length = 500, nullable = false)
    private String title;

    @Lob // CLOB 매핑
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "WRITER_ID", length = 50, nullable = false)
    private String writerId;

    @Column(name = "FIX_YN", length = 1, nullable = false)
    @Builder.Default
    private String fixYn = "N";

    @Column(name = "DELETE_YN", length = 1, nullable = false)
    @Builder.Default
    private String deleteYn = "N";

    @Column(name = "DELETE_TIME")
    private LocalDateTime deleteTime;
}