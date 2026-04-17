package com.simplecoding.chargerreservation.member.controller;

import com.simplecoding.chargerreservation.member.dto.EmailRequestDto;
import com.simplecoding.chargerreservation.member.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이메일 API", description = "회원가입 이메일 인증 API")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "인증번호 발송", description = "입력한 이메일로 6자리 인증번호를 발송합니다")
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailRequestDto.Send request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    @Operation(summary = "인증번호 확인", description = "발송된 인증번호가 일치하는지 검증합니다")
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody EmailRequestDto.Verify request) {
        emailService.verifyCode(request);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }
}