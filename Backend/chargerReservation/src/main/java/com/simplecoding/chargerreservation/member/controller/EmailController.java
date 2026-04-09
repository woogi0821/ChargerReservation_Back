package com.simplecoding.chargerreservation.member.controller;

import com.simplecoding.chargerreservation.member.dto.EmailRequestDto;
import com.simplecoding.chargerreservation.member.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    // 인증번호 발송 API
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailRequestDto.Send request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 인증번호 확인 API
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody EmailRequestDto.Verify request) {
        emailService.verifyCode(request);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

}
