package com.simplecoding.chargerreservation.admin.service;

import com.simplecoding.chargerreservation.admin.dto.AdminChargerDto;
import com.simplecoding.chargerreservation.admin.dto.AdminDashboardDto;
import com.simplecoding.chargerreservation.admin.dto.AdminDto;
import com.simplecoding.chargerreservation.admin.dto.AdminInquiryDto;
import com.simplecoding.chargerreservation.admin.dto.AdminMemberDto;
import com.simplecoding.chargerreservation.admin.dto.AdminNoticeDto;
import com.simplecoding.chargerreservation.admin.dto.AdminPenaltyDto;
import com.simplecoding.chargerreservation.admin.dto.AdminReservationDto;
import com.simplecoding.chargerreservation.admin.dto.AdminStationDto;
import com.simplecoding.chargerreservation.admin.entity.Admin;
import com.simplecoding.chargerreservation.admin.repository.AdminRepository;
import com.simplecoding.chargerreservation.charger.entity.ChargerEntity;
import com.simplecoding.chargerreservation.charger.entity.ChargerId;
import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import com.simplecoding.chargerreservation.common.SecurityUtil;
import com.simplecoding.chargerreservation.inquiry.entity.Inquiry;
import com.simplecoding.chargerreservation.inquiry.repository.InquiryRepository;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyHistory;
import com.simplecoding.chargerreservation.penalty.entity.PenaltyStatus;
import com.simplecoding.chargerreservation.penalty.repository.PenaltyRepository;
import com.simplecoding.chargerreservation.reservation.entity.Reservation;
import com.simplecoding.chargerreservation.reservation.repository.ReservationRepository;
import com.simplecoding.chargerreservation.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ✅ 수정 — 기본을 readOnly 로 변경
public class AdminService {

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;
    private final PenaltyRepository penaltyRepository;
    private final ReservationRepository reservationRepository;
    private final NoticeRepository noticeRepository;
    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;
    private final InquiryRepository inquiryRepository;

    // ── 요청자 관리자 조회 헬퍼 ─────────────────────────────────────────────────
    private Admin getRequesterAdmin() {
        String loginId = SecurityUtil.getCurrentLoginId();
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("인증된 회원을 찾을 수 없습니다"));
        return adminRepository.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "관리자 권한이 없습니다"));
    }

    // ── 관리자 전체 목록 조회 ────────────────────────────────────────────────────
    public List<AdminDto> getAdminList() {
        List<Admin> admins = adminRepository.findAll();
        return admins.stream()
                .map(admin -> {
                    String name = memberRepository.findById(admin.getMemberId())
                            .map(Member::getName)
                            .orElse("알 수 없음");
                    return AdminDto.from(admin, name);
                })
                .collect(Collectors.toList());
    }

    // ── 관리자 등록 ──────────────────────────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminDto createAdmin(AdminDto dto) {
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new RuntimeException("등록 대상 회원을 찾을 수 없습니다"));
        Admin admin = new Admin(dto.getMemberId(), dto.getAdminRole());
        adminRepository.save(admin);
        member.setMemberGrade("Y");
        memberRepository.save(member);
        return AdminDto.from(admin);
    }

    // ── 관리자 단건 조회 ─────────────────────────────────────────────────────────
    public AdminDto getAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다"));
        return AdminDto.from(admin);
    }

    // ── 관리자 역할 변경 (SUPER 만 가능) ─────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminDto updateAdminRole(Long targetId, String newRole) {
        Admin requester = getRequesterAdmin();
        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 역할 변경이 가능합니다");
        }
        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));
        target.updateRole(newRole);
        return AdminDto.from(adminRepository.save(target));
    }

    // ── 관리자 해제 (SUPER 만 가능) ──────────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public void deleteAdmin(Long targetId) {
        Admin requester = getRequesterAdmin();
        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 관리자 해제가 가능합니다");
        }
        if (requester.getAdminId().equals(targetId)) {
            throw new RuntimeException("자기 자신은 해제할 수 없습니다");
        }
        Admin target = adminRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("대상 관리자를 찾을 수 없습니다"));
        adminRepository.delete(target);
        Member member = memberRepository.findById(target.getMemberId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));
        member.setMemberGrade("N");
        memberRepository.save(member);
    }

    // ── 회원 전체 목록 조회 (SUPER / ALL / MEMBER 파트만 가능) ───────────────────
    public List<AdminMemberDto> getMemberList() {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 조회 권한이 없습니다");
        }
        return memberRepository.findAll()
                .stream()
                .map(AdminMemberDto::from)
                .collect(Collectors.toList());
    }

    // ── 회원 상태 변경 (SUPER / ALL / MEMBER 파트만 가능) ────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminMemberDto updateMemberStatus(Long memberId, String newStatus) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole  = requester.getAdminRole().equals("SUPER");
        boolean isMemberPart = requester.getAdminPart().equals("MEMBER")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isMemberPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회원 상태 변경 권한이 없습니다");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다"));
        member.setStatus(newStatus);
        if (newStatus.equals("SUSPENDED")) {
            member.setSuspendedUntil(LocalDateTime.now().plusHours(24));
        }
        if (newStatus.equals("ACTIVE") || newStatus.equals("WITHDRAWN")) {
            member.setSuspendedUntil(null);
        }
        return AdminMemberDto.from(memberRepository.save(member));
    }

    // ── 패널티 전체 목록 조회 (SUPER / ALL / INQUIRY 파트만 가능) ────────────────
    public List<AdminPenaltyDto> getPenaltyList() {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "패널티 조회 권한이 없습니다");
        }
        return penaltyRepository.findAll()
                .stream()
                .map(AdminPenaltyDto::from)
                .collect(Collectors.toList());
    }

    // ── 패널티 취소 (SUPER / ALL / INQUIRY 파트만 가능) ──────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminPenaltyDto cancelPenalty(Long penaltyId) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "패널티 취소 권한이 없습니다");
        }
        PenaltyHistory penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new RuntimeException("패널티를 찾을 수 없습니다"));
        if (penalty.getStatus().equals(PenaltyStatus.CANCELED)) {
            throw new RuntimeException("이미 취소된 패널티입니다");
        }
        penalty.setStatus(PenaltyStatus.CANCELED);
        return AdminPenaltyDto.from(penaltyRepository.save(penalty));
    }

    // ── 예약 전체 목록 조회 (SUPER / ALL / RESERVATION 파트만 가능) ──────────────
    public List<AdminReservationDto> getReservationList() {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole       = requester.getAdminRole().equals("SUPER");
        boolean isReservationPart = requester.getAdminPart().equals("RESERVATION")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isReservationPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "예약 조회 권한이 없습니다");
        }
        return reservationRepository.findAll()
                .stream()
                .map(AdminReservationDto::from)
                .collect(Collectors.toList());
    }

    // ── 예약 강제 취소 (SUPER / ALL / RESERVATION 파트만 가능) ───────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminReservationDto cancelReservation(Long reservationId) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole       = requester.getAdminRole().equals("SUPER");
        boolean isReservationPart = requester.getAdminPart().equals("RESERVATION")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isReservationPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "예약 취소 권한이 없습니다");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다"));
        if (reservation.getStatus().equals("CANCELED")) {
            throw new RuntimeException("이미 취소된 예약입니다");
        }
        if (reservation.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("이미 완료된 예약입니다");
        }
        reservation.changeStatus("CANCELED");
        return AdminReservationDto.from(reservationRepository.save(reservation));
    }

    // ── 공지사항 목록 조회 (전체 접근 가능 / 삭제된 것 제외) ─────────────────────
    public List<AdminNoticeDto> getNoticeList() {
        return noticeRepository.findAll()
                .stream()
                .filter(n -> n.getDeleteYn().equals("N"))
                .map(AdminNoticeDto::from)
                .collect(Collectors.toList());
    }

    // ── 공지사항 등록 (SUPER 만 가능) ────────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminNoticeDto createNotice(AdminNoticeDto dto) {
        Admin requester = getRequesterAdmin();
        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 공지사항 등록이 가능합니다");
        }
        NoticeEntity notice = NoticeEntity.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .writerId(requester.getAdminId().toString())
                .fixYn(dto.getFixYn() != null ? dto.getFixYn() : "N")
                .build();
        return AdminNoticeDto.from(noticeRepository.save(notice));
    }

    // ── 공지사항 수정 (SUPER 만 가능) ────────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminNoticeDto updateNotice(Long noticeId, AdminNoticeDto dto) {
        Admin requester = getRequesterAdmin();
        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 공지사항 수정이 가능합니다");
        }
        NoticeEntity notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다"));
        if (notice.getDeleteYn().equals("Y")) {
            throw new RuntimeException("삭제된 공지사항입니다");
        }
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setFixYn(dto.getFixYn() != null ? dto.getFixYn() : notice.getFixYn());
        return AdminNoticeDto.from(noticeRepository.save(notice));
    }

    // ── 공지사항 삭제 (SUPER 만 가능) ────────────────────────────────────────────
    @Transactional // ✅ 쓰기 작업
    public void deleteNotice(Long noticeId) {
        Admin requester = getRequesterAdmin();
        if (!requester.getAdminRole().equals("SUPER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER 권한만 공지사항 삭제가 가능합니다");
        }
        NoticeEntity notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다"));
        if (notice.getDeleteYn().equals("Y")) {
            throw new RuntimeException("이미 삭제된 공지사항입니다");
        }
        notice.setDeleteYn("Y");
        notice.setDeleteTime(LocalDateTime.now());
        noticeRepository.save(notice);
    }

    // ── 충전소 목록 조회 (SUPER / ALL / CHARGER 파트만 가능) ─────────────────────
    public List<AdminStationDto> getStationList(String keyword) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isChargerPart = requester.getAdminPart().equals("CHARGER")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isChargerPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "충전소 조회 권한이 없습니다");
        }
        if (keyword != null && !keyword.isEmpty()) {
            return stationRepository.findByIntegratedSearch(keyword)
                    .stream()
                    .limit(50)
                    .map(AdminStationDto::from)
                    .collect(Collectors.toList());
        }
        return stationRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(AdminStationDto::from)
                .collect(Collectors.toList());
    }

    // ── 충전기 목록 조회 (SUPER / ALL / CHARGER 파트만 가능) ─────────────────────
    public List<AdminChargerDto> getChargerList(String statId) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isChargerPart = requester.getAdminPart().equals("CHARGER")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isChargerPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "충전기 조회 권한이 없습니다");
        }
        if (statId != null && !statId.isEmpty()) {
            return chargerRepository.findByStatId(statId)
                    .stream()
                    .map(AdminChargerDto::from)
                    .collect(Collectors.toList());
        }
        return chargerRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(AdminChargerDto::from)
                .collect(Collectors.toList());
    }

    // ── 충전기 상태 변경 (SUPER / ALL / CHARGER 파트만 가능) ─────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminChargerDto updateChargerStat(String statId, String chargerId, String newStat) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isChargerPart = requester.getAdminPart().equals("CHARGER")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isChargerPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "충전기 상태 변경 권한이 없습니다");
        }
        ChargerId id = new ChargerId(statId, chargerId);
        ChargerEntity charger = chargerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("충전기를 찾을 수 없습니다"));
        charger.setStat(newStat.trim());
        return AdminChargerDto.from(chargerRepository.save(charger));
    }

    // ── 문의 전체 목록 조회 (SUPER / ALL / INQUIRY 파트만 가능) ──────────────────
    public List<AdminInquiryDto> getInquiryList() {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "문의 조회 권한이 없습니다");
        }
        return inquiryRepository.findAll()
                .stream()
                .map(AdminInquiryDto::from)
                .collect(Collectors.toList());
    }

    // ── 문의 답변 등록 (SUPER / ALL / INQUIRY 파트만 가능) ───────────────────────
    @Transactional // ✅ 쓰기 작업
    public AdminInquiryDto answerInquiry(Long inquiryId, AdminInquiryDto dto) {
        Admin requester = getRequesterAdmin();
        boolean isSuperRole   = requester.getAdminRole().equals("SUPER");
        boolean isInquiryPart = requester.getAdminPart().equals("INQUIRY")
                || requester.getAdminPart().equals("ALL");
        if (!isSuperRole && !isInquiryPart) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "문의 답변 권한이 없습니다");
        }
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다"));
        if (inquiry.getStatus().equals("ANSWERED")) {
            throw new RuntimeException("이미 답변된 문의입니다");
        }
        inquiry.setAnswerContent(dto.getAnswerContent());
        inquiry.setAnswerAt(LocalDateTime.now());
        inquiry.setAdminId(requester.getAdminId());
        inquiry.setStatus("ANSWERED");
        return AdminInquiryDto.from(inquiryRepository.save(inquiry));
    }

    // ── 대시보드 통계 조회 ────────────────────────────────────────────────────────
    public AdminDashboardDto getDashboardStats() {
        long totalMembers = memberRepository.count();

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayReservations = reservationRepository
                .findByStartTimeBetween(startOfDay, endOfDay).size();

        long totalStations = stationRepository.count();

        long brokenChargers = chargerRepository.countByStatIn(List.of("4", "5"));

        long pendingInquiries = inquiryRepository.findByStatus("PENDING").size();

        return new AdminDashboardDto(
                totalMembers,
                todayReservations,
                totalStations,
                brokenChargers,
                pendingInquiries
        );
    }
}