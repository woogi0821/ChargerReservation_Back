package com.simplecoding.chargerreservation.notice.service;

import com.simplecoding.chargerreservation.notice.dto.NoticeRequestDto;
import com.simplecoding.chargerreservation.notice.dto.NoticeResponseDto;
import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import com.simplecoding.chargerreservation.notice.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest

class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    @Rollback(false) // 롤백하지 말고 DB에 그대로 남겨라!
    @DisplayName("관리자가 공지사항을 등록하면 DB에 정상적으로 저장되어야 한다.")
    void registerNotice() {
        // 1. Given: 테스트 데이터 준비
        NoticeRequestDto requestDto = NoticeRequestDto.builder()
                .title("테스트 공지사항 제목")
                .content("테스트 공지사항 내용입니다.")
                .fixYn("N")
                .build();

        String adminId = "admin_01";

        // 2. When: 서비스의 등록 로직 실행
        NoticeResponseDto result = noticeService.registerNotice(requestDto, adminId);

        // 3. Then: 검증
        // 3-1. 결과 값이 null이 아닌지 확인
        assertNotNull(result);
        assertNotNull(result.getNoticeId()); // 시퀀스에 의해 ID가 생성되었는지 확인

        // 3-2. 입력한 데이터가 결과값에 잘 들어있는지 확인
        assertEquals("테스트 공지사항 제목", result.getTitle());
        assertEquals(adminId, result.getWriterId());

        // 3-3. 실제 DB(Repository)에 데이터가 들어갔는지 최종 확인
        NoticeEntity savedEntity = noticeRepository.findById(result.getNoticeId()).orElse(null);
        assertNotNull(savedEntity);
        assertEquals("N", savedEntity.getDeleteYn()); // 우리가 서비스에서 강제 세팅한 'N'이 잘 들어갔는지
        assertEquals("테스트 공지사항 내용입니다.", savedEntity.getContent());

        System.out.println("등록된 공지사항 ID: " + savedEntity.getNoticeId());
    }
}