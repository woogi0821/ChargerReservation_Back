package com.simplecoding.chargerreservation.inquiry.repository;

import com.simplecoding.chargerreservation.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 특정 회원의 문의 목록 조회
    List<Inquiry> findByMemberId(Long memberId);

    // 상태별 문의 목록 조회
    List<Inquiry> findByStatus(String status);

    // 특정 충전소 / 충전기 관련 문의 조회
    List<Inquiry> findByStatIdAndChargerId(String statId, String chargerId);
}