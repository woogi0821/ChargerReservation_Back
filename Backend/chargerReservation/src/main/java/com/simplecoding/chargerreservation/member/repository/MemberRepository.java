package com.simplecoding.chargerreservation.member.repository;

import com.simplecoding.chargerreservation.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 아이디 및 이메일 중복 확인 메서드
    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByEmail(String email);

    // 소셜 로그인 중복 확인 및 기존 회원 찾기
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    // 이메일인증 아이디 중복 확인
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'CANCELLED' " +
        "WHERE r.member = :member AND r.status IN ('RESERVED')")
    void cancelAllReservationsByMember(@Param("member") Member member);
}
