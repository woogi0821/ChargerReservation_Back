package com.simplecoding.chargerreservation.inquiry.dto;

import com.simplecoding.chargerreservation.inquiry.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDto {

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

    // Inquiry Entity → InquiryDto 변환
    public static InquiryDto from(Inquiry inquiry) {
        return new InquiryDto(
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