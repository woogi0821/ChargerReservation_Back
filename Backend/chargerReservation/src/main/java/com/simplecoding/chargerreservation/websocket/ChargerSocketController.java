package com.simplecoding.chargerreservation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Controller
@RequiredArgsConstructor
public class ChargerSocketController {

    //simpMessagingTemplate란? 서버가 클라이언트에게 메세지를 능동적으로 보낼 때 사용
    //REST의 ResponseEntity와 비슷한 역할이지만 WebSocket전용임
    //예약 완료시 서비스 레이어에서도 이걸 주입받아 상태 브로드캐스트 가능
    private final SimpMessagingTemplate messagingTemplate;

    //충전기 상태 변경 브로드캐스트 메소드
    //[흐름]
    //예약완료->현장PIN인증->충전종료/충전 조기종료등의 상태변경 이벤트발생
    //서비스레이어에서 해당 메소드를 호출하거나
    //클라이언트가 /app/charger/{statId}/{chargerId}/status로 직접 발행
    // topic/charger/{statId}_{chargerId}를 구독중인 키오스크 화면에 상태 전파

    @MessageMapping("/charger/{statId}/{chargerId}/status")
    public void broadcastChargerStatus(
            @DestinationVariable String statId,
            @DestinationVariable String chargerId, String status
    ) {
        log.info("충전기 상태 변경 수신 - chargerId : {}, status : {}", chargerId, status);
        messagingTemplate.convertAndSend("/topic/charger/" + statId + "_" + chargerId, status);
    }

    //서버 내부에서 직접 상태를 푸시할때 사용하는 메소드
    //@MessageMapping없이 순수하게 서버 -> 클라이언트 단방향 전송
    //예약서비스에서 예약완료 후 직접 호출
    public void pushStatus(String statId, String chargerId, String status) {
        log.info("서버 내부 상태 푸시 - chargerId : {}, status : {}", chargerId, status);
        messagingTemplate.convertAndSend("/topic/charger/" + statId + "_" + chargerId, status);
    }

    //예약 생성 시 /topic/reservation/new 토픽으로 브로드캐스트
    //키오스크 화면이 해당 토픽을 구독하고 있으면 PIN패드 입력 화면으로 자동 전환
    public void pushNewReservation(String statId, String chargerId) {
        log.info("신규 예약 브로드캐스트 - statId : {}, chargerId : {}", statId, chargerId);
        Map<String, String> payload = new HashMap<>();
        payload.put("statId", statId);
        payload.put("chargerId", chargerId);
        payload.put("status", "RESERVED");
        messagingTemplate.convertAndSend("/topic/reservation/new", payload);
    }
}
