//package com.simplecoding.chargerreservation.notice.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "NOTICE")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class NoticeEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_seq")
//    @SequenceGenerator(
//            name = "notice_seq",
//            sequenceName = "NOTICE_SEQ",
//            allocationSize = 1
//    )
//    @Column(name = "NOTICE_ID")
//    private Long noticeId;
//
//    @Column(name = "NOTICE_NO", length = 10, unique = true)
//    private String noticeNo;  // n001, n002 ...
//
//    @Column(name = "TITLE", nullable = false, length = 200)
//    private String title;
//
//    @Column(name = "CONTENT", nullable = false, columnDefinition = "CLOB")
//    private String content;
//
//    @Column(name = "AUTHOR", nullable = false, length = 50)
//    private String author;
//
//    @Column(name = "IS_PINNED", nullable = false)
//    @Builder.Default
//    private boolean isPinned = false;  // 고정 여부
//
//    @Column(name = "IS_DELETED", nullable = false)
//    @Builder.Default
//    private boolean isDeleted = false;  // 소프트 삭제
//
//    @CreationTimestamp
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "UPDATED_AT")
//    private LocalDateTime updatedAt;
//}