package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.inquiry.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 어드민 페이지에서 문의 목록 조회 시 사용하는 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminInquiryDto {

    private Long inquiryId;
    private Long memberId;
    private String statId;
    private String chargerId;
    private String category;
    private String title;
    private String content;
    private String status;
    private String answerContent;
    private LocalDateTime answerAt;
    private LocalDateTime insertTime;

    // Inquiry Entity → AdminInquiryDto 변환
    public static AdminInquiryDto from(Inquiry inquiry) {
        return new AdminInquiryDto(
                inquiry.getInquiryId(),
                inquiry.getMemberId(),
                inquiry.getStatId(),
                inquiry.getChargerId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAnswerContent(),
                inquiry.getAnswerAt(),
                inquiry.getInsertTime()
        );
    }
}