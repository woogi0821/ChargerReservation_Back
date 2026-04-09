package com.simplecoding.chargerreservation.penalty.entity;

public enum PenaltyStatus {
    ACTIVE,   // 패널티 적용 중
    CLEARED,  // 기간 만료 또는 해제됨
    CANCELED  // 관리자가 취소함
}
