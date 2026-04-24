package com.simplecoding.chargerreservation.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Component
@Slf4j
public class SseEmitters {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String loginId, SseEmitter emitter) {
        this.emitters.put(loginId, emitter);
        emitter.onCompletion(() -> this.emitters.remove(loginId));
        emitter.onTimeout(() -> this.emitters.remove(loginId));
        return emitter;
    }

    public void send(String loginId, Object data) {
        SseEmitter emitter = emitters.get(loginId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(data));
            } catch (IOException e) {
                emitters.remove(loginId);
            }
        }
    }
}
