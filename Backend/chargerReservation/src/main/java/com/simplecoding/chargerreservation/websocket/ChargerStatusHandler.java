package com.simplecoding.chargerreservation.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class ChargerStatusHandler {

    // 현재 연결된 전체 세션 목록 (키오스크 & 사용자 화면 추적용)
    private final Set<String> connectedSessions = ConcurrentHashMap.newKeySet();

    // chargerId별 구독 중인 세션 목록
    // ex) {"CHARGER_001" -> ["session-a", "session-b"]}
    private final Map<String, Set<String>> chargerSubscriptions = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────
    // WebSocket 연결 완료 이벤트
    // 클라이언트가 /ws-charger 에 연결됐을 때 실행
    // ─────────────────────────────────────────
    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        connectedSessions.add(sessionId);
        log.info("🔌 WebSocket 연결 - sessionId: {}, 현재 총 연결 수: {}", sessionId, connectedSessions.size());
    }

    // ─────────────────────────────────────────
    // WebSocket 연결 해제 이벤트
    // 클라이언트가 브라우저를 닫거나 네트워크가 끊겼을 때 실행
    // ─────────────────────────────────────────
    @EventListener
    public void handleDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        connectedSessions.remove(sessionId);

        // 해당 세션이 구독하던 충전기 목록에서도 제거
        chargerSubscriptions.values().forEach(sessions -> sessions.remove(sessionId));
        // 구독자가 0명이 된 충전기 항목은 Map에서 정리
        chargerSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        log.info("🔴 WebSocket 해제 - sessionId: {}, 남은 연결 수: {}", sessionId, connectedSessions.size());
    }

    // ─────────────────────────────────────────
    // 충전기 채널 구독 이벤트
    // 클라이언트가 /topic/charger/{chargerId} 를 구독할 때 실행
    // ─────────────────────────────────────────
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/topic/charger/")) {
            String chargerId = destination.replace("/topic/charger/", "");
            chargerSubscriptions
                    .computeIfAbsent(chargerId, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);
            log.info("📡 충전기 구독 - chargerId: {}, sessionId: {}, 구독자 수: {}",
                    chargerId, sessionId, chargerSubscriptions.get(chargerId).size());
        }
    }

    // ─────────────────────────────────────────
    // 구독 해제 이벤트
    // ─────────────────────────────────────────
    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("📴 구독 해제 - sessionId: {}", sessionId);
    }

    // ─────────────────────────────────────────
    // 모니터링용 조회 메서드 (필요 시 Admin API에서 호출 가능)
    // ─────────────────────────────────────────

    // 현재 연결된 총 세션 수
    public int getConnectedSessionCount() {
        return connectedSessions.size();
    }

    // 특정 충전기를 구독 중인 세션 수
    public int getSubscriberCount(String chargerId) {
        Set<String> sessions = chargerSubscriptions.get(chargerId);
        return sessions != null ? sessions.size() : 0;
    }

    // 현재 구독자가 있는 충전기 목록
    public Set<String> getActiveChargerIds() {
        return chargerSubscriptions.keySet();
    }
}

