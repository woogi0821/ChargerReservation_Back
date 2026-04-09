//package com.simplecoding.chargerreservation.admin.service;
//
//import com.simplecoding.chargerreservation.admin.dto.AdminDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminLoginRequestDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminLoginResponseDto;
//import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
//import com.simplecoding.chargerreservation.admin.entity.Admin;
//import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
//import com.simplecoding.chargerreservation.common.JwtUtil;
//import com.simplecoding.chargerreservation.member.entity.Member;
//import com.simplecoding.chargerreservation.member.repository.MemberRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class AdminServiceTest {
//
//    @Mock
//    private AdminRepository adminRepository;
//
//    @Mock
//    private MemberRepository memberRepository;
//
//    @Mock
//    private JwtUtil jwtUtil;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private AdminService adminService;
//
//    // ── 기존 테스트 ──────────────────────────────
//
//    @Test
//    void createAdmin() {
//        AdminDto requestDto = new AdminDto(null, 1L, "SUPER");
//        Admin savedAdmin = new Admin(1L, "SUPER");
//        when(adminRepository.save(any(Admin.class))).thenReturn(savedAdmin);
//
//        AdminDto responseDto = adminService.createAdmin(requestDto);
//
//        assertEquals(1L, responseDto.getMemberId());
//        assertEquals("SUPER", responseDto.getAdminRole());
//    }
//
//    // ── 로그인 테스트 ────────────────────────────
//
//    @Test
//    void login_성공() {
//        Member mockMember = Member.builder()
//                .memberId(1L)
//                .loginId("admin")
//                .loginPw("1234")
//                .status("ACTIVE")
//                .suspendedUntil(null)
//                .build();
//
//        Admin mockAdmin = new Admin(1L, "SUPER");
//
//        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(mockMember));
//        when(adminRepository.findByMemberId(1L)).thenReturn(Optional.of(mockAdmin));
//        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
//        // adminPart 파라미터 추가
//        when(jwtUtil.generateAccessToken(any(), any(), anyString(), anyString()))
//                .thenReturn("fake.jwt.token");
//
//        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin", "1234");
//        AdminLoginResponseDto response = adminService.login(requestDto);
//
//        assertNotNull(response);
//        assertEquals("fake.jwt.token", response.getAccessToken());
//        assertEquals("SUPER", response.getAdminRole());
//    }
//
//    @Test
//    void login_실패_아이디없음() {
//        when(memberRepository.findByLoginId("없는아이디")).thenReturn(Optional.empty());
//
//        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("없는아이디", "1234");
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.login(requestDto));
//
//        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다", exception.getMessage());
//    }
//
//    @Test
//    void login_실패_비밀번호틀림() {
//        Member mockMember = Member.builder()
//                .memberId(1L)
//                .loginId("admin")
//                .loginPw("1234")
//                .status("ACTIVE")
//                .build();
//
//        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(mockMember));
//
//        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin", "틀린비밀번호");
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.login(requestDto));
//
//        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다", exception.getMessage());
//    }
//
//    @Test
//    void login_실패_관리자권한없음() {
//        Member mockMember = Member.builder()
//                .memberId(1L)
//                .loginId("admin")
//                .loginPw("1234")
//                .status("ACTIVE")
//                .suspendedUntil(null)
//                .build();
//
//        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(mockMember));
//        when(adminRepository.findByMemberId(1L)).thenReturn(Optional.empty());
//        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
//
//        AdminLoginRequestDto requestDto = new AdminLoginRequestDto("admin", "1234");
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.login(requestDto));
//
//        assertEquals("관리자 권한이 없습니다", exception.getMessage());
//    }
//
//    // ── 관리자 목록 조회 테스트 ──────────────────
//
//    @Test
//    void getAdminList_성공() {
//        Admin mockAdmin1 = new Admin(1L, "SUPER");
//        Admin mockAdmin2 = new Admin(2L, "MANAGER");
//
//        Member mockMember1 = Member.builder().memberId(1L).name("홍길동").build();
//        Member mockMember2 = Member.builder().memberId(2L).name("김철수").build();
//
//        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin1, mockAdmin2));
//        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember1));
//        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember2));
//
//        List<AdminDto> result = adminService.getAdminList();
//
//        assertEquals(2, result.size());
//        assertEquals("홍길동", result.get(0).getName());
//        assertEquals("김철수", result.get(1).getName());
//    }
//
//    @Test
//    void getAdminList_빈목록() {
//        when(adminRepository.findAll()).thenReturn(List.of());
//
//        List<AdminDto> result = adminService.getAdminList();
//
//        assertEquals(0, result.size());
//    }
//
//    @Test
//    void getAdminList_멤버없을때() {
//        Admin mockAdmin = new Admin(1L, "SUPER");
//
//        when(adminRepository.findAll()).thenReturn(List.of(mockAdmin));
//        when(memberRepository.findById(1L)).thenReturn(Optional.empty());
//
//        List<AdminDto> result = adminService.getAdminList();
//
//        assertEquals(1, result.size());
//        assertEquals("알 수 없음", result.get(0).getName());
//    }
//
//    // ── 관리자 해제 테스트 ───────────────────────
//
//    @Test
//    void deleteAdmin_성공() {
//        Admin mockRequester = new Admin(1L, "SUPER");
//        Admin mockTarget = new Admin(2L, "MANAGER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(adminRepository.findById(2L)).thenReturn(Optional.of(mockTarget));
//
//        assertDoesNotThrow(() -> adminService.deleteAdmin(1L, 2L));
//        verify(adminRepository, times(1)).delete(mockTarget);
//    }
//
//    @Test
//    void deleteAdmin_실패_권한없음() {
//        Admin mockRequester = new Admin(1L, "MANAGER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.deleteAdmin(1L, 2L));
//
//        assertEquals("권한이 없습니다", exception.getMessage());
//    }
//
//    @Test
//    void deleteAdmin_실패_자기자신해제() {
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.deleteAdmin(1L, 1L));
//
//        assertEquals("자기 자신을 해제할 수 없습니다", exception.getMessage());
//    }
//
//    @Test
//    void deleteAdmin_실패_없는관리자() {
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(adminRepository.findById(2L)).thenReturn(Optional.empty());
//
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.deleteAdmin(1L, 2L));
//
//        assertEquals("대상 관리자를 찾을 수 없습니다", exception.getMessage());
//    }
//
//    // ── 회원 목록 조회 테스트 ────────────────────
//
//    @Test
//    void getMemberList_성공() {
//        // given — SUPER 권한 요청자
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        Member mockMember1 = Member.builder()
//                .memberId(1L).loginId("user1").name("홍길동")
//                .email("user1@test.com").phone("010-1111-1111")
//                .status("ACTIVE").penaltyCount(0).build();
//
//        Member mockMember2 = Member.builder()
//                .memberId(2L).loginId("user2").name("김철수")
//                .email("user2@test.com").phone("010-2222-2222")
//                .status("SUSPENDED").penaltyCount(1).build();
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(memberRepository.findAll()).thenReturn(List.of(mockMember1, mockMember2));
//
//        // when
//        List<AdminMemberDto> result = adminService.getMemberList(1L);
//
//        // then
//        assertEquals(2, result.size());
//        assertEquals("홍길동", result.get(0).getName());
//        assertEquals("김철수", result.get(1).getName());
//    }
//
//    @Test
//    void getMemberList_빈목록() {
//        // given
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(memberRepository.findAll()).thenReturn(List.of());
//
//        // when
//        List<AdminMemberDto> result = adminService.getMemberList(1L);
//
//        // then
//        assertEquals(0, result.size());
//    }
//
//    @Test
//    void getMemberList_실패_권한없음() {
//        // given — CHARGER 파트 요청자
//        Admin mockRequester = new Admin(1L, "MANAGER");
//        mockRequester.updatePart("CHARGER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//
//        // when & then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.getMemberList(1L));
//
//        assertEquals("접근 권한이 없습니다", exception.getMessage());
//    }
//
//    // ── 회원 상태 변경 테스트 ────────────────────
//
//    @Test
//    void updateMemberStatus_정지처리() {
//        // given — SUPER 권한 요청자
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        Member mockMember = Member.builder()
//                .memberId(2L).loginId("user1").name("홍길동")
//                .status("ACTIVE").penaltyCount(0).build();
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember));
//        when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
//
//        // when
//        AdminMemberDto result = adminService.updateMemberStatus(1L, 2L, "SUSPENDED");
//
//        // then
//        assertEquals("SUSPENDED", result.getStatus());
//    }
//
//    @Test
//    void updateMemberStatus_정지해제() {
//        // given
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        Member mockMember = Member.builder()
//                .memberId(2L).loginId("user1").name("홍길동")
//                .status("SUSPENDED").penaltyCount(0).build();
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember));
//        when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
//
//        // when
//        AdminMemberDto result = adminService.updateMemberStatus(1L, 2L, "ACTIVE");
//
//        // then
//        assertEquals("ACTIVE", result.getStatus());
//    }
//
//    @Test
//    void updateMemberStatus_실패_없는회원() {
//        // given
//        Admin mockRequester = new Admin(1L, "SUPER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//        when(memberRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // when & then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.updateMemberStatus(1L, 999L, "SUSPENDED"));
//
//        assertEquals("회원을 찾을 수 없습니다", exception.getMessage());
//    }
//
//    @Test
//    void updateMemberStatus_실패_권한없음() {
//        // given — CHARGER 파트 요청자
//        Admin mockRequester = new Admin(1L, "MANAGER");
//        mockRequester.updatePart("CHARGER");
//
//        when(adminRepository.findById(1L)).thenReturn(Optional.of(mockRequester));
//
//        // when & then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> adminService.updateMemberStatus(1L, 2L, "SUSPENDED"));
//
//        assertEquals("접근 권한이 없습니다", exception.getMessage());
//    }
//
//    // ── 비밀번호 암호화 확인용 ──────────────────
//    @Test
//    void generatePassword() {
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String encoded = encoder.encode("1234");
//        System.out.println("암호화된 값: " + encoded);
//    }
//}