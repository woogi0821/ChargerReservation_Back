package com.simplecoding.chargerreservation.notification.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 👈 리액트 주소 허용
public class SseController {
    private final SseEmitters sseEmitters;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String loginId) {
        // 30분 동안 연결 유지 (기본값)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 연결 즉시 더미 데이터를 하나 보내야 연결이 끊기지 않습니다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sseEmitters.add(loginId, emitter);
    }
}
