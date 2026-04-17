package com.simplecoding.chargerreservation.penalty.controller;

import com.simplecoding.chargerreservation.penalty.dto.PenaltyRequestDto;
import com.simplecoding.chargerreservation.penalty.dto.PenaltyResponseDto;
import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "패널티 API", description = "패널티 등록 및 회원별 패널티 내역 조회 API")
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;

    @Operation(summary = "패널티 등록", description = "노쇼 발생 시 패널티를 등록하고 문자를 발송합니다")
    @PostMapping
    public ResponseEntity<PenaltyResponseDto> createPenalty(@RequestBody PenaltyRequestDto requestDto) {
        PenaltyResponseDto response = penaltyService.processPenaltyStep(requestDto, 1);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원별 패널티 내역 조회", description = "memberId 로 해당 회원의 패널티 내역 목록을 조회합니다")
    @GetMapping("/{memberId}")
    public ResponseEntity<List<PenaltyResponseDto>> getMemberPenalties(@PathVariable String memberId) {
        List<PenaltyResponseDto> list = penaltyService.getMemberPenalties(memberId);
        return ResponseEntity.ok(list);
    }
}