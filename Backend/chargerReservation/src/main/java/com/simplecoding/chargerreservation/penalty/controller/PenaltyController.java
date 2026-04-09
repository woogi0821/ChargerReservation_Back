package com.simplecoding.chargerreservation.penalty.controller;


import com.simplecoding.chargerreservation.penalty.dto.PenaltyRequestDto;
import com.simplecoding.chargerreservation.penalty.dto.PenaltyResponseDto;
import com.simplecoding.chargerreservation.penalty.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalties") // 리액트에서 부를 주소 (http://localhost:8080/api/penalties)
@RequiredArgsConstructor
public class PenaltyController {
    private final PenaltyService penaltyService;

    //    1. 패널티 등록 (POST 방식)
//     리액트의 등록 버튼을 누르면 이쪽으로 데이터가 들어옵니다.
    @PostMapping
    public ResponseEntity<PenaltyResponseDto> createPenalty(@RequestBody PenaltyRequestDto requestDto) {
        // 서비스의 로직(저장 + 문자발송 시뮬레이션)을 실행
        // 일단 기본값으로 1단계(충전완료 직후)를 보낸다고 가정
        PenaltyResponseDto response = penaltyService.processPenaltyStep(requestDto, 1);
        return ResponseEntity.ok(response); // 성공하면 200 OK와 함께 결과 전송
    }

    //    2. 특정 회원의 패널티 내역 조회 (GET 방식)
//     리액트 모달을 띄울 때 "해당 회원의 아이디"로 내역을 싹 가져옵니다.
    @GetMapping("/{memberId}")
    public ResponseEntity<List<PenaltyResponseDto>> getMemberPenalties(@PathVariable String memberId) {
        // 서비스에서 리스트를 가져와서 리턴합니다.
        List<PenaltyResponseDto> list = penaltyService.getMemberPenalties(memberId);
        return ResponseEntity.ok(list);
    }
}
