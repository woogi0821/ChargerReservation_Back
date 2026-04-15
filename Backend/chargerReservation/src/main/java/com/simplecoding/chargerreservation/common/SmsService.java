package com.simplecoding.chargerreservation.common;

import jakarta.annotation.PostConstruct;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SmsService {

    private DefaultMessageService messageService;

    @Value("${solapi.api-key}")
    private String apiKey;

    @Value("${solapi.api-secret}")
    private String apiSecret;

    @Value("${solapi.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    public void sendPenaltyMessage(String to, String name, String reason, String until) {
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(to);
        message.setText("[EV충전]"+ name + "님, " + reason + " 기록으로 인해 " + until + "까지 예약이 제한됩니다.");
        this.messageService.sendOne(new SingleMessageSendingRequest(message));
    }

    public void sendPinMessage(String to, String name, String pin, LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(to);
        message.setText("[EV충전 예약완료]" + name + "님\n" + "키오스크 PIN : " + pin + "\n" + "예약시간" + startTime.format(dateFmt) + " ~ " + endTime.format(timeFmt));
        this.messageService.sendOne(new SingleMessageSendingRequest(message));
    }
}
