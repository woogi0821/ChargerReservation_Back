package com.simplecoding.chargerreservation.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
// 목적: 컨트롤러의 모든 에러는 여기서 가로채서 실행합니다.
//    1파일에서 모든 에러처리를 사용하면 관리가 쉽습니다.
// @RestControllerAdvice : 공통 에러처리 어노테이션, (AOP 기능)
@Log4j2
@RestControllerAdvice
@RequiredArgsConstructor
public class CommonException {

    // ResponseStatusException 처리 (예: 404, 400 등): 개발자가 직접 발생시킨 에러들 처리
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatusException(ResponseStatusException e) {
        log.error("에러 발생: {}", e);

        ApiResponse response = new ApiResponse<>();
        response.setSuccess(false);                // 결과 박스 클래스: success 필드에 false 리액트로 전송
        response.setMessage(e.getMessage());       // ResponseStatusException에 담긴 메시지: 개발자가 에러발생시킴

        return ResponseEntity.status(e.getStatusCode()).body(response);
    }

    // 위에서 잡히지 않는 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleAllException(Exception e) {
        log.error("벡엔드 서버 오류가 발생했습니다.", e);

        ApiResponse response = new ApiResponse<>();
        response.setSuccess(false);
//        response.setMessage(e.getMessage() != null ? e.getMessage() : "벡엔드 서버 오류가 발생했습니다.");
        response.setMessage(e.getMessage());

        return ResponseEntity.status(500).body(response);
    }
}
