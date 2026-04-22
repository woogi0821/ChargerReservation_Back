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
    Double getSlowPrice();    // ✨ 완속 추가
    Double getLastCurrentPrice();
    Double getLastSlowPrice();

    default Double getPriceDiff() {
        if (getCurrentPrice() != null && getLastCurrentPrice() != null) {
            return getCurrentPrice() - getLastCurrentPrice();
        }
        return 0.0;
    }

    // 2. 완속 요금 차이 계산
    default Double getSlowPriceDiff() {
        if (getSlowPrice() != null && getLastSlowPrice() != null) {
            return getSlowPrice() - getLastSlowPrice();
        }
        return 0.0;
    }
}
