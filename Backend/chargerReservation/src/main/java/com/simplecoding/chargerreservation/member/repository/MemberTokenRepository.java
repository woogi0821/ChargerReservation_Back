package com.simplecoding.chargerreservation.member.repository;

import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.entity.MemberToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberTokenRepository extends JpaRepository<MemberToken, Long> {
    // 특정 회원의 토큰이 이미 있는지 확인 (로그인 시 기존 토큰 업데이트용)
    Optional<MemberToken> findByMember(Member member);
    // 리프레시 토큰 값으로 DB에 저장된 정보가 있는지 확인 (토큰 재발급용)
    Optional<MemberToken> findByRefreshToken(String refreshToken);
    // Member 객체로 토큰 정보 삭제
    void deleteByMember(Member member);
}
