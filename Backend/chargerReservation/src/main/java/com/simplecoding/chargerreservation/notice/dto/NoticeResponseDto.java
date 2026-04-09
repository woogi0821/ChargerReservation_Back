package com.simplecoding.chargerreservation.notice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponseDto {
    private Long noticeId;
    private String title;
    private String content;
    private String writerId;
    private String fixYn;
    private LocalDateTime insertTime;
    private LocalDateTime updateTime;

    // UI 편의 필드
    private String formattedDate;
    private boolean isNew;
}