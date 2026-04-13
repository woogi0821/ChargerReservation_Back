package com.simplecoding.chargerreservation.admin.service;

import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminNoticeDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.dto.AdminReservationDto;
import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.common.SecurityUtil;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyStatus;
import com.simplecoding.chargerreservation.penalty.repository.PenaltyRepository;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private NoticeRepository noticeRepository;

    @InjectMocks
    private AdminService adminService;

    // ── 테스트용 헬퍼 ────────────────────────────────────────────────────────────

    private Admin createAdmin(Long adminId, Long memberId, String role) {
        Admin admin = new Admin(memberId, role);
        ReflectionTestUtils.setField(admin, "adminId", adminId);
        return admin;
    }

    private void mockRequesterChain(MockedStatic<SecurityUtil> securityUtil, Admin requester) {
        Member mockMember = Member.builder().memberId(requester.getMemberId()).build();
        securityUtil.when(SecurityUtil::getCurrentLoginId).thenReturn("testAdmin");
        when(memberRepository.findByLoginId("testAdmin")).thenReturn(Optional.of(mockMember));
        when(adminRepository.findByMemberId(requester.getMemberId())).thenReturn(Optional.of(requester));
    }

    private PenaltyHistory createPenalty(Long penaltyId, PenaltyStatus status) {
        PenaltyHistory penalty = new PenaltyHistory();
        ReflectionTestUtils.setField(penalty, "penaltyId", penaltyId);
        penalty.setStatus(status);
        return penalty;
    }

    private Reservation createReservation(Long reservationId, String status) {
        Reservation reservation = Reservation.builder()
                .memberId(1L).chargerId("C001").carNumber("12가3456")
                .reservationPin("1234").startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1)).status(status).build();
        ReflectionTestUtils.setField(reservation, "id", reservationId);
        return reservation;
    }

    // NoticeEntity 헬퍼
    private NoticeEntity createNotice(Long noticeId, String deleteYn) {
        NoticeEntity notice = NoticeEntity.builder()
                .title("테스트 공지")
                .content("테스트 내용")
                .writerId("admin")
                .fixYn("N")
                .deleteYn(deleteYn)
                .build();
        ReflectionTestUtils.setField(notice, "noticeId", noticeId);
        return notice;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 관리자 전체 목록 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 이름 포함 반환")
    void getAdminList_성공() {
        Admin mockAdmin1 = new Admin(1L, "SUPER");
        Admin mockAdmin2 = new Admin(2L, "MANAGER");
        Member mockMember1 = Member.builder().memberId(1L).name("홍길동").build();
        Member mockMember2 = Member.builder().memberId(2L).name("김철수").build();
        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin1, mockAdmin2));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember1));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember2));
        List<AdminDto> result = adminService.getAdminList();
        assertEquals(2, result.size());
        assertEquals("홍길동", result.get(0).getName());
        assertEquals("김철수", result.get(1).getName());
    }

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 등록된 관리자 없음")
    void getAdminList_빈목록() {
        when(adminRepository.findAll()).thenReturn(List.of());
        assertEquals(0, adminService.getAdminList().size());
    }

    @Test
    @DisplayName("관리자 목록 조회 — 성공 : 매핑 회원 없을 때 이름 '알 수 없음' 처리")
    void getAdminList_멤버없을때() {
        Admin mockAdmin = new Admin(1L, "SUPER");
        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin));
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());
        List<AdminDto> result = adminService.getAdminList();
        assertEquals(1, result.size());
        assertEquals("알 수 없음", result.get(0).getName());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 관리자 등록 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 등록 — 성공")
    void createAdmin_성공() {
        AdminDto requestDto = new AdminDto(null, 1L, "MANAGER", "ALL");
        Member mockMember = Member.builder().memberId(1L).name("홍길동").build();
        Admin savedAdmin = new Admin(1L, "MANAGER");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);
        when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
        AdminDto result = adminService.createAdmin(requestDto);
        assertEquals(1L, result.getMemberId());
        assertEquals("MANAGER", result.getAdminRole());
        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("관리자 등록 — 실패 : 존재하지 않는 회원")
    void createAdmin_실패_없는회원() {
        AdminDto requestDto = new AdminDto(null, 999L, "MANAGER", "ALL");
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.createAdmin(requestDto));
        assertEquals("등록 대상 회원을 찾을 수 없습니다", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 관리자 단건 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 단건 조회 — 성공")
    void getAdmin_성공() {
        Admin mockAdmin = new Admin(1L, "SUPER");
        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockAdmin));
        AdminDto result = adminService.getAdmin(1L);
        assertEquals(1L, result.getMemberId());
        assertEquals("SUPER", result.getAdminRole());
    }

    @Test
    @DisplayName("관리자 단건 조회 — 실패 : 존재하지 않는 adminId")
    void getAdmin_실패_없는관리자() {
        when(adminRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.getAdmin(999L));
        assertEquals("관리자를 찾을 수 없습니다", exception.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 관리자 역할 변경 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 역할 변경 — 성공")
    void updateAdminRole_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Admin mockTarget    = createAdmin(2L, 2L, "MANAGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(adminRepository.findById(2L)).thenReturn(Optional.of(mockTarget));
            when(adminRepository.save(any(Admin.class))).thenReturn(mockTarget);
            AdminDto result = adminService.updateAdminRole(2L, "VIEWER");
            assertEquals("VIEWER", result.getAdminRole());
        }
    }

    @Test
    @DisplayName("관리자 역할 변경 — 실패 : SUPER 아닌 관리자 요청 (403)")
    void updateAdminRole_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.updateAdminRole(2L, "VIEWER"));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 관리자 해제 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("관리자 해제 — 성공")
    void deleteAdmin_성공() {
        Admin mockRequester     = createAdmin(1L, 1L, "SUPER");
        Admin mockTarget        = createAdmin(2L, 2L, "MANAGER");
        Member mockTargetMember = Member.builder().memberId(2L).memberGrade("Y").build();
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(adminRepository.findById(2L)).thenReturn(Optional.of(mockTarget));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(mockTargetMember));
            when(memberRepository.save(any(Member.class))).thenReturn(mockTargetMember);
            assertDoesNotThrow(() -> adminService.deleteAdmin(2L));
            verify(adminRepository, times(1)).delete(mockTarget);
            verify(memberRepository, times(1)).save(any(Member.class));
        }
    }

    @Test
    @DisplayName("관리자 해제 — 실패 : SUPER 아닌 관리자 요청 (403)")
    void deleteAdmin_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.deleteAdmin(2L));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    @Test
    @DisplayName("관리자 해제 — 실패 : 자기 자신 해제")
    void deleteAdmin_실패_자기자신해제() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.deleteAdmin(1L));
            assertEquals("자기 자신은 해제할 수 없습니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("관리자 해제 — 실패 : 존재하지 않는 대상")
    void deleteAdmin_실패_없는관리자() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(adminRepository.findById(999L)).thenReturn(Optional.empty());
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.deleteAdmin(999L));
            assertEquals("대상 관리자를 찾을 수 없습니다", exception.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 회원 목록 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("회원 목록 조회 — 성공 : SUPER")
    void getMemberList_성공_SUPER() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Member m1 = Member.builder().memberId(2L).loginId("user1").name("홍길동").email("u1@test.com").phone("010-1111-1111").status("ACTIVE").penaltyCount(0).build();
        Member m2 = Member.builder().memberId(3L).loginId("user2").name("김철수").email("u2@test.com").phone("010-2222-2222").status("SUSPENDED").penaltyCount(1).build();
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findAll()).thenReturn(List.of(m1, m2));
            List<AdminMemberDto> result = adminService.getMemberList();
            assertEquals(2, result.size());
            assertEquals("홍길동", result.get(0).getName());
        }
    }

    @Test
    @DisplayName("회원 목록 조회 — 성공 : MEMBER 파트")
    void getMemberList_성공_MEMBER파트() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("MEMBER");
        Member mockMember = Member.builder().memberId(2L).loginId("user1").name("홍길동").status("ACTIVE").penaltyCount(0).build();
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findAll()).thenReturn(List.of(mockMember));
            List<AdminMemberDto> result = adminService.getMemberList();
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("회원 목록 조회 — 성공 : 빈 목록")
    void getMemberList_빈목록() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findAll()).thenReturn(List.of());
            assertEquals(0, adminService.getMemberList().size());
        }
    }

    @Test
    @DisplayName("회원 목록 조회 — 실패 : 권한 없음 (403)")
    void getMemberList_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.getMemberList());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 회원 상태 변경 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("회원 상태 변경 — 정지 처리")
    void updateMemberStatus_정지처리() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Member mockMember = Member.builder().memberId(2L).loginId("user1").name("홍길동").status("ACTIVE").penaltyCount(0).build();
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember));
            when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
            assertEquals("SUSPENDED", adminService.updateMemberStatus(2L, "SUSPENDED").getStatus());
        }
    }

    @Test
    @DisplayName("회원 상태 변경 — 정지 해제")
    void updateMemberStatus_정지해제() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Member mockMember = Member.builder().memberId(2L).loginId("user1").name("홍길동").status("SUSPENDED").penaltyCount(0).build();
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember));
            when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
            assertEquals("ACTIVE", adminService.updateMemberStatus(2L, "ACTIVE").getStatus());
        }
    }

    @Test
    @DisplayName("회원 상태 변경 — 실패 : 존재하지 않는 회원")
    void updateMemberStatus_실패_없는회원() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.updateMemberStatus(999L, "SUSPENDED"));
            assertEquals("회원을 찾을 수 없습니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("회원 상태 변경 — 실패 : 권한 없음 (403)")
    void updateMemberStatus_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.updateMemberStatus(2L, "SUSPENDED"));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 패널티 목록 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("패널티 목록 조회 — 성공 : SUPER")
    void getPenaltyList_성공_SUPER() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        PenaltyHistory p1 = createPenalty(1L, PenaltyStatus.ACTIVE);
        PenaltyHistory p2 = createPenalty(2L, PenaltyStatus.CLEARED);
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(penaltyRepository.findAll()).thenReturn(List.of(p1, p2));
            assertEquals(2, adminService.getPenaltyList().size());
        }
    }

    @Test
    @DisplayName("패널티 목록 조회 — 성공 : INQUIRY 파트")
    void getPenaltyList_성공_INQUIRY파트() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("INQUIRY");
        PenaltyHistory p1 = createPenalty(1L, PenaltyStatus.ACTIVE);
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(penaltyRepository.findAll()).thenReturn(List.of(p1));
            assertEquals(1, adminService.getPenaltyList().size());
        }
    }

    @Test
    @DisplayName("패널티 목록 조회 — 실패 : 권한 없음 (403)")
    void getPenaltyList_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.getPenaltyList());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 패널티 취소 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("패널티 취소 — 성공")
    void cancelPenalty_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        PenaltyHistory mockPenalty = createPenalty(1L, PenaltyStatus.ACTIVE);
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(penaltyRepository.findById(1L)).thenReturn(Optional.of(mockPenalty));
            when(penaltyRepository.save(any(PenaltyHistory.class))).thenReturn(mockPenalty);
            assertEquals(PenaltyStatus.CANCELED, adminService.cancelPenalty(1L).getStatus());
        }
    }

    @Test
    @DisplayName("패널티 취소 — 실패 : 존재하지 않는 패널티")
    void cancelPenalty_실패_없는패널티() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(penaltyRepository.findById(999L)).thenReturn(Optional.empty());
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.cancelPenalty(999L));
            assertEquals("패널티를 찾을 수 없습니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("패널티 취소 — 실패 : 이미 취소된 패널티")
    void cancelPenalty_실패_이미취소된패널티() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        PenaltyHistory mockPenalty = createPenalty(1L, PenaltyStatus.CANCELED);
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(penaltyRepository.findById(1L)).thenReturn(Optional.of(mockPenalty));
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.cancelPenalty(1L));
            assertEquals("이미 취소된 패널티입니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("패널티 취소 — 실패 : 권한 없음 (403)")
    void cancelPenalty_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.cancelPenalty(1L));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 예약 목록 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("예약 목록 조회 — 성공 : SUPER")
    void getReservationList_성공_SUPER() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Reservation r1 = createReservation(1L, "RESERVED");
        Reservation r2 = createReservation(2L, "CHARGING");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findAll()).thenReturn(List.of(r1, r2));
            assertEquals(2, adminService.getReservationList().size());
        }
    }

    @Test
    @DisplayName("예약 목록 조회 — 성공 : RESERVATION 파트")
    void getReservationList_성공_RESERVATION파트() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("RESERVATION");
        Reservation r1 = createReservation(1L, "RESERVED");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findAll()).thenReturn(List.of(r1));
            assertEquals(1, adminService.getReservationList().size());
        }
    }

    @Test
    @DisplayName("예약 목록 조회 — 실패 : 권한 없음 (403)")
    void getReservationList_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.getReservationList());
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 예약 강제 취소 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("예약 강제 취소 — 성공")
    void cancelReservation_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Reservation mockReservation = createReservation(1L, "RESERVED");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(mockReservation);
            assertEquals("CANCELED", adminService.cancelReservation(1L).getStatus());
        }
    }

    @Test
    @DisplayName("예약 강제 취소 — 실패 : 존재하지 않는 예약")
    void cancelReservation_실패_없는예약() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.cancelReservation(999L));
            assertEquals("예약을 찾을 수 없습니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("예약 강제 취소 — 실패 : 이미 취소된 예약")
    void cancelReservation_실패_이미취소된예약() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Reservation mockReservation = createReservation(1L, "CANCELED");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.cancelReservation(1L));
            assertEquals("이미 취소된 예약입니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("예약 강제 취소 — 실패 : 이미 완료된 예약")
    void cancelReservation_실패_이미완료된예약() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        Reservation mockReservation = createReservation(1L, "COMPLETED");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.cancelReservation(1L));
            assertEquals("이미 완료된 예약입니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("예약 강제 취소 — 실패 : 권한 없음 (403)")
    void cancelReservation_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        mockRequester.updatePart("CHARGER");
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.cancelReservation(1L));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 공지사항 목록 조회 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("공지사항 목록 조회 — 성공 : 삭제된 것 제외")
    void getNoticeList_성공() {
        NoticeEntity n1 = createNotice(1L, "N");
        NoticeEntity n2 = createNotice(2L, "N");
        NoticeEntity n3 = createNotice(3L, "Y"); // 삭제된 것
        when(noticeRepository.findAll()).thenReturn(List.of(n1, n2, n3));
        List<AdminNoticeDto> result = adminService.getNoticeList();
        assertEquals(2, result.size()); // 삭제된 것 제외
    }

    @Test
    @DisplayName("공지사항 목록 조회 — 성공 : 빈 목록")
    void getNoticeList_빈목록() {
        when(noticeRepository.findAll()).thenReturn(List.of());
        assertEquals(0, adminService.getNoticeList().size());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 공지사항 등록 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("공지사항 등록 — 성공")
    void createNotice_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        AdminNoticeDto requestDto = new AdminNoticeDto(null, "제목", "내용", null, "N", null, null);
        NoticeEntity savedNotice = createNotice(1L, "N");

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(noticeRepository.save(any(NoticeEntity.class))).thenReturn(savedNotice);
            AdminNoticeDto result = adminService.createNotice(requestDto);
            assertNotNull(result);
            verify(noticeRepository, times(1)).save(any(NoticeEntity.class));
        }
    }

    @Test
    @DisplayName("공지사항 등록 — 실패 : 권한 없음 (403)")
    void createNotice_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        AdminNoticeDto requestDto = new AdminNoticeDto(null, "제목", "내용", null, "N", null, null);

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.createNotice(requestDto));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 공지사항 수정 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("공지사항 수정 — 성공")
    void updateNotice_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        NoticeEntity mockNotice = createNotice(1L, "N");
        AdminNoticeDto requestDto = new AdminNoticeDto(null, "수정된 제목", "수정된 내용", null, "Y", null, null);

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(mockNotice));
            when(noticeRepository.save(any(NoticeEntity.class))).thenReturn(mockNotice);
            AdminNoticeDto result = adminService.updateNotice(1L, requestDto);
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("공지사항 수정 — 실패 : 존재하지 않는 공지사항")
    void updateNotice_실패_없는공지사항() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        AdminNoticeDto requestDto = new AdminNoticeDto(null, "제목", "내용", null, "N", null, null);

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(noticeRepository.findById(999L)).thenReturn(Optional.empty());
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.updateNotice(999L, requestDto));
            assertEquals("공지사항을 찾을 수 없습니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("공지사항 수정 — 실패 : 권한 없음 (403)")
    void updateNotice_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");
        AdminNoticeDto requestDto = new AdminNoticeDto(null, "제목", "내용", null, "N", null, null);

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.updateNotice(1L, requestDto));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 공지사항 삭제 테스트
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("공지사항 삭제 — 성공")
    void deleteNotice_성공() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        NoticeEntity mockNotice = createNotice(1L, "N");

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(mockNotice));
            when(noticeRepository.save(any(NoticeEntity.class))).thenReturn(mockNotice);
            assertDoesNotThrow(() -> adminService.deleteNotice(1L));
            verify(noticeRepository, times(1)).save(any(NoticeEntity.class));
        }
    }

    @Test
    @DisplayName("공지사항 삭제 — 실패 : 이미 삭제된 공지사항")
    void deleteNotice_실패_이미삭제된공지사항() {
        Admin mockRequester = createAdmin(1L, 1L, "SUPER");
        NoticeEntity mockNotice = createNotice(1L, "Y");

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            when(noticeRepository.findById(1L)).thenReturn(Optional.of(mockNotice));
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adminService.deleteNotice(1L));
            assertEquals("이미 삭제된 공지사항입니다", exception.getMessage());
        }
    }

    @Test
    @DisplayName("공지사항 삭제 — 실패 : 권한 없음 (403)")
    void deleteNotice_실패_권한없음() {
        Admin mockRequester = createAdmin(1L, 1L, "MANAGER");

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            mockRequesterChain(securityUtil, mockRequester);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> adminService.deleteNotice(1L));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }
}