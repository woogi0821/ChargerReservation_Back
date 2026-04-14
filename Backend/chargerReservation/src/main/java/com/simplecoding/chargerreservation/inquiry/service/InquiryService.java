package com.simplecoding.chargerreservation.inquiry.service;

import com.simplecoding.chargerreservation.inquiry.dto.InquiryDto;
import com.simplecoding.chargerreservation.inquiry.entity.Inquiry;
import com.simplecoding.chargerreservation.inquiry.repository.InquiryRepository;
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
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    // ── 문의 작성 ────────────────────────────────────────────────────────────────
    public InquiryDto createInquiry(InquiryDto dto) {
        Inquiry inquiry = Inquiry.builder()
                .memberId(dto.getMemberId())
                .statId(dto.getStatId())
                .chargerId(dto.getChargerId())
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .status("PENDING")
                .insertTime(LocalDateTime.now())
                .build();
        return InquiryDto.from(inquiryRepository.save(inquiry));
    }

    // ── 본인 문의 목록 조회 ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<InquiryDto> getInquiryList(Long memberId) {
        return inquiryRepository.findByMemberId(memberId)
                .stream()
                .map(InquiryDto::from)
                .collect(Collectors.toList());
    }

    // ── 본인 문의 상세 조회 ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public InquiryDto getInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다"));

        // 본인 문의인지 확인
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 문의만 조회할 수 있습니다");
        }
        return InquiryDto.from(inquiry);
    }

    // ── 문의 수정 (PENDING 상태만 가능) ─────────────────────────────────────────
    public InquiryDto updateInquiry(Long inquiryId, Long memberId, InquiryDto dto) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다"));

        // 본인 문의인지 확인
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 문의만 수정할 수 있습니다");
        }

        // PENDING 상태인지 확인
        if (!inquiry.getStatus().equals("PENDING")) {
            throw new RuntimeException("답변 완료된 문의는 수정할 수 없습니다");
        }

        inquiry.setTitle(dto.getTitle());
        inquiry.setContent(dto.getContent());
        inquiry.setCategory(dto.getCategory());

        return InquiryDto.from(inquiryRepository.save(inquiry));
    }

    // ── 문의 삭제 (PENDING 상태만 가능) ─────────────────────────────────────────
    public void deleteInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다"));

        // 본인 문의인지 확인
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 문의만 삭제할 수 있습니다");
        }

        // PENDING 상태인지 확인
        if (!inquiry.getStatus().equals("PENDING")) {
            throw new RuntimeException("답변 완료된 문의는 삭제할 수 없습니다");
        }

        inquiryRepository.delete(inquiry);
    }
}