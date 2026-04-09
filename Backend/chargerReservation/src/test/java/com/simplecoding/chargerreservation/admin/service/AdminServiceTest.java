package com.simplecoding.chargerreservation.admin.service;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.member.service.MemberService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
//@SpringBootTest
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AdminService adminService;

// ════════════════════════════════════════════════════════════════════════════
// 관리자 전체 목록 조회 테스트
// ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 이름 포함 반환")
    void getAdminList_성공() {
        // given
        Admin mockAdmin1 = new Admin(1L, "SUPER");
        Admin mockAdmin2 = new Admin(2L, "MANAGER");

        Member mockMember1 = Member.builder().memberId(1L).name("홍길동").build();
        Member mockMember2 = Member.builder().memberId(2L).name("김철수").build();

        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin1, mockAdmin2));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember1));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember2));

        // when
        List<AdminDto> result = adminService.getAdminList();

        // then
        assertEquals(2, result.size());
        assertEquals("홍길동", result.get(0).getName());
        assertEquals("김철수", result.get(1).getName());
    }

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 등록된 관리자 없음")
    void getAdminList_빈목록() {
        // given
        when(adminRepository.findAll()).thenReturn(List.of());

        // when
        List<AdminDto> result = adminService.getAdminList();

        // then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 매핑 회원 없을 때 이름 '알 수 없음' 처리")
    void getAdminList_멤버없을때() {
        // given — Admin 은 있지만 연결된 Member 가 없는 케이스
        Admin mockAdmin = new Admin(1L, "SUPER");

        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin));
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        List<AdminDto> result = adminService.getAdminList();

        // then — 이름이 "알 수 없음" 으로 반환되어야 함
        assertEquals(1, result.size());
        assertEquals("알 수 없음", result.get(0).getName());
    }

    @Test
    void createAdmin() {
    }

    @Test
    void getAdmin() {
    }

    @Test
    void updateAdminRole() {
    }

    @Test
    void deleteAdmin() {
    }

    @Test
    void getMemberList() {
    }

    @Test
    void updateMemberStatus() {
    }
}