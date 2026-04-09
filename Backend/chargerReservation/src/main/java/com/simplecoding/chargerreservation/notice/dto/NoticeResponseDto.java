//package com.simplecoding.chargerreservation.notice.dto;
//
//import com.simplecoding.chargerreservation.entity.Notice;
//import lombok.*;
//
//import java.time.LocalDate;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class NoticeResponseDto {
//
//    private Long noticeId;
//    private String noticeNo;
//    private String title;
//    private String content;
//    private String author;
//    private boolean isPinned;
//    private LocalDate createdAt;
//
//    public static NoticeResponseDto from(Notice notice) {
//        return NoticeResponseDto.builder()
//                .noticeId(notice.getNoticeId())
//                .noticeNo(notice.getNoticeNo())
//                .title(notice.getTitle())
//                .content(notice.getContent())
//                .author(notice.getAuthor())
//                .isPinned(notice.isPinned())
//                .createdAt(notice.getCreatedAt().toLocalDate())
//                .build();
//    }
//}