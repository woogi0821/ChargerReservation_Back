package com.simplecoding.chargerreservation.station.repository;

public interface MarkerProjection {
    String getStatId();
    String getStatNm();
    Double getLat();
    Double getLng();
    Integer getTotalCount();
    Integer getAvailableCount();
    Integer getBrokenCount();

    // --- 목록(List) 조회를 위해 추가 ---
    String getAddr();           // 주소
    String getBnm();            // 운영기관(환경부 등)
    String getUseTime();        // 이용시간
    String getParkingFree();    // 주차료 유무 (Y/N)
    String getLimitYn();        // 이용제한 유무 (Y/N)
    String getLimitDetail();    // 제한 상세사유
    Double getDistance();       // [핵심] DB에서 계산된 거리
    Double getCurrentPrice();
}