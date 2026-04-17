package com.simplecoding.chargerreservation.inquiry.controller;

import com.simplecoding.chargerreservation.inquiry.dto.InquiryDto;
import com.simplecoding.chargerreservation.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "문의 API", description = "1:1 문의 작성 / 조회 / 수정 / 삭제 API")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 작성", description = "새로운 1:1 문의를 작성합니다")
    @PostMapping
    public ResponseEntity<InquiryDto> createInquiry(@RequestBody InquiryDto dto) {
        return ResponseEntity.ok(inquiryService.createInquiry(dto));
    }

    @Operation(summary = "본인 문의 목록 조회", description = "memberId 로 본인이 작성한 문의 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<InquiryDto>> getInquiryList(
            @RequestParam Long memberId) {
        return ResponseEntity.ok(inquiryService.getInquiryList(memberId));
    }

    @Operation(summary = "본인 문의 상세 조회", description = "inquiryId 와 memberId 로 본인 문의 상세 내용을 조회합니다")
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDto> getInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(inquiryService.getInquiry(inquiryId, memberId));
    }

    @Operation(summary = "문의 수정", description = "PENDING 상태인 문의만 수정 가능합니다")
    @PatchMapping("/{inquiryId}")
    public ResponseEntity<InquiryDto> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId,
            @RequestBody InquiryDto dto) {
        return ResponseEntity.ok(inquiryService.updateInquiry(inquiryId, memberId, dto));
    }

    @Operation(summary = "문의 삭제", description = "PENDING 상태인 문의만 삭제 가능합니다")
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long memberId) {
        inquiryService.deleteInquiry(inquiryId, memberId);
        return ResponseEntity.noContent().build();
    }
}