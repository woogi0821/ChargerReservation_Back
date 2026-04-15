package com.simplecoding.chargerreservation.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 어드민 대시보드 통계 데이터 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {

    // 총 회원수
    private long totalMembers;

    // 오늘 예약 건수
    private long todayReservations;

    // 총 충전소 건수
    private long totalStations;

    // 고장 충전기 건수 (stat = 4 또는 5)
    private long brokenChargers;

    // 미답변 문의 건수
    private long pendingInquiries;
}