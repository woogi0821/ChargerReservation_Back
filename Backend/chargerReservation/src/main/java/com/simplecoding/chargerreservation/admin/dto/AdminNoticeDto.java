package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 어드민 페이지에서 공지사항 목록 조회 / 등록 / 수정 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminNoticeDto {

    private Long noticeId;
    private String title;
    private String content;
    private String writerId;
    private String fixYn;
    private String deleteYn;
    private LocalDateTime insertTime;

    // NoticeEntity → AdminNoticeDto 변환
    public static AdminNoticeDto from(NoticeEntity notice) {
        return new AdminNoticeDto(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getWriterId(),
                notice.getFixYn(),
                notice.getDeleteYn(),
                notice.getInsertTime()
        );
    }
}