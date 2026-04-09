package com.simplecoding.chargerreservation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 목적: 리액트로 결과를 포장해서 보낼 클래스
//  예) 결과배열, 현재페이지번호, 총개수, 성공메세지 또는 실패메세지, 추가)성공코드, 실패코드
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;  // 성공: true, 실패: false
    private String message;   // 성공(실패) 메세지
    private T result;         // 결과 배열: T 자료형(Dept, Emp, 등 임의의 클래스)
    private int page;         // 현재페이지 번호
    private long totalNumber; // 총개수
}
