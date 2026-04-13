package com.simplecoding.chargerreservation.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequestDto {
    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private String fixYn; // 'Y' or 'N'
    private Long noticeId; // 수정 시 필요
}