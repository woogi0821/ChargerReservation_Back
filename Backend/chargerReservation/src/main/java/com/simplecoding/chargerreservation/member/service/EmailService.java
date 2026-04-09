package com.simplecoding.chargerreservation.member.service;

import com.simplecoding.chargerreservation.member.dto.EmailRequestDto;
import com.simplecoding.chargerreservation.member.entity.EmailVerification;
import com.simplecoding.chargerreservation.member.repository.EmailVerificationRepository;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailVerificationRepository verificationRepository;
    private final MemberRepository memberRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // 인증 번호 발송
    public void sendVerificationCode(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일 주소입니다.");
        }

        String authCode = String.valueOf((int)(Math.random() * 899999) + 100000);
        int validityMinutes = 5; // 5분 유효

        // 기존 인증 정보가 있으면 갱신, 없으면 신규 생성
        EmailVerification verification = verificationRepository.findById(email)
            .map(v -> {
                v.renewVerification(authCode, validityMinutes);
                return v;
            })
            .orElse(new EmailVerification(email, authCode, validityMinutes));

        verificationRepository.save(verification);
        sendMail(email, authCode);
    }

    // 메일 발송 처리
    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(senderEmail);
        message.setSubject("[ChargeNow] 회원가입 이메일 인증");
        message.setText("요청하신 인증 코드는 [ " + code + " ] \n5분 이내에 입력해주세요.");

        mailSender.send(message);
    }

    // 인증 번호 확인
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyCode(EmailRequestDto.Verify request) {
        EmailVerification verification = verificationRepository.findById(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("인증 요청 기록이 없습니다."));

        // 엔티티 내부 로직 실행 (만료시간, 횟수, 일치여부 검사)
        verification.verifyCode(request.getAuthCode());
    }

}
