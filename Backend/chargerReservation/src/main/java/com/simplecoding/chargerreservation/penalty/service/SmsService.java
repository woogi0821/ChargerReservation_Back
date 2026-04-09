package com.simplecoding.chargerreservation.penalty.service;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct; // 스프링 부트 3버전 이상은 jakarta입니다!

@Service
public class SmsService {
    private DefaultMessageService messageService;

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    // 객체가 생성된 후 실행되어 API를 초기화합니다.
    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    public void sendPenaltyMessage(String to, String name, String reason, String until) {
        Message message = new Message();
        message.setFrom(fromNumber); // 설정 파일에서 가져온 번호 사용
        message.setTo(to);
        message.setText("[EV충전] " + name + "님, " + reason + " 기록으로 인해 " + until + "까지 예약이 제한됩니다.");

        this.messageService.sendOne(new SingleMessageSendingRequest(message));
    }
}
