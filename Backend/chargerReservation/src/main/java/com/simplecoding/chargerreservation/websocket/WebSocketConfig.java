package com.simplecoding.chargerreservation.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    //클라이언트(React)가 WebSocket 연결을 맺을 엔드포인트 등록
    //프론트에서 new Client({brokerURL : 'ws://localghos:8080/ws-charger'})로 연결
    //withSockJS() : SockJS 폴백 지원 (WebSocket 미지원 브라우저 대비)
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws-charger")
                .setAllowedOriginPatterns("*") //개발단계에선 전체 허용 배포시 도메인으로 바꿀것!
                .withSockJS();
    }

    //메세지 브로커 설정
    //[구독 prefix]/topic
    //클라이언트(React)가 특정 채널을 구독할 때 사용
    //예) A충전소 1번 충전기 상태 구독 : /topic/charger/1
    //[발행 prefix]/app
    //클라이언트(React)에서 서버로 메세지를 보낼때 사용
    //예) PIN입력 후 서버에 검증 요청 : /app/charger/verify-pin

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic"); //구독 채널 prefix
        registry.setApplicationDestinationPrefixes("/app"); //발행 prefix
    }
}
