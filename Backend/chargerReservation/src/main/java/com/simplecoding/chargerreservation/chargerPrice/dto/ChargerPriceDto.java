package com.simplecoding.chargerreservation.chargerPrice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargerPriceDto {

    private Long priceId;          // ✨ 엔티티의 ID (식별용)
    private String bnm;            // 운영사명 (에버온, 환경부 등)
    private String speedType;      // 급속 / 완속
    private Integer applyYear;     // 적용 연도 (2026)
    private String season;         // 계절 (봄가을, 여름, 겨울)
    private Double unitPrice;      // 올해(현재) 단가
    private String remark;         // 비고 (회원가 기준 등)

    // --- 비교 분석용 필드 (Service에서 계산하여 채움) ---
    private Double lastYearPrice;  // 작년 동기 단가
    private Double diff;           // 가격 차이 (올해 - 작년)
    private String trendMessage;   // "작년 봄가을 대비 20.0원 인상📈"

    private LocalDateTime updateDt; // 최종 업데이트 일시
}