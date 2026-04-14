package com.simplecoding.chargerreservation.inquiry.controller;

import com.simplecoding.chargerreservation.inquiry.dto.InquiryDto;
import com.simplecoding.chargerreservation.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // ── 문의 작성 ────────────────────────────────────────────────────────────────
    // POST /api/inquiries
    @PostMapping
    public ResponseEntity<InquiryDto> createInquiry(@RequestBody InquiryDto dto) {
        return ResponseEntity.ok(inquiryService.createInquiry(dto));
    }

    // ── 본인 문의 목록 조회 ──────────────────────────────────────────────────────
    // GET /api/inquiries?memberId=1
    @GetMapping
    public ResponseEntity<List<InquiryDto>> getInquiryList(
            @RequestParam Long memberId) {
        return ResponseEntity.ok(inquiryService.getInquiryList(memberId));
    }

    // ── 본인 문의 상세 조회 ──────────────────────────────────────────────────────
    // GET /api/inquiries/{inquiryId}?memberId=1
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDto> getInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(inquiryService.getInquiry(inquiryId, memberId));
    }

    // ── 문의 수정 (PENDING 상태만 가능) ─────────────────────────────────────────
    // PATCH /api/inquiries/{inquiryId}?memberId=1
    @PatchMapping("/{inquiryId}")
    public ResponseEntity<InquiryDto> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId,
            @RequestBody InquiryDto dto) {
        return ResponseEntity.ok(inquiryService.updateInquiry(inquiryId, memberId, dto));
    }

    // ── 문의 삭제 (PENDING 상태만 가능) ─────────────────────────────────────────
    // DELETE /api/inquiries/{inquiryId}?memberId=1
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId) {
        inquiryService.deleteInquiry(inquiryId, memberId);
        return ResponseEntity.noContent().build();
    }
}