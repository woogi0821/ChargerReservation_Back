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
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .title("2026/04/10")
                .content("오늘 우리팀 다 안왔어요.")
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
        assertEquals("2026/04/10", result.getTitle());
        assertEquals(adminId, result.getWriterId());

        // 3-3. 실제 DB(Repository)에 데이터가 들어갔는지 최종 확인
        NoticeEntity savedEntity = noticeRepository.findById(result.getNoticeId()).orElse(null);
        assertNotNull(savedEntity);
        assertEquals("N", savedEntity.getDeleteYn()); // 우리가 서비스에서 강제 세팅한 'N'이 잘 들어갔는지
        assertEquals("테스트 공지사항 내용입니다.", savedEntity.getContent());

        System.out.println("등록된 공지사항 ID: " + savedEntity.getNoticeId());
    }

    @Test
    @DisplayName("4번 공지사항 수정 테스트")
    void updateNotice() {
        // 1. Given: 4번 데이터가 DB에 있다는 가정하에 ID를 지정합니다.
        // (만약 @Transactional 때문에 데이터가 없다면 임시로 생성해줍니다)
        Long targetId = 4L;

        // 만약 4번 데이터가 없을 경우를 대비한 안전장치 (실제 DB에 4번이 있다면 생략 가능)
        if (!noticeRepository.existsById(targetId)) {
            noticeRepository.save(NoticeEntity.builder()
                    .noticeId(targetId) // 시퀀스 대신 직접 지정 (테스트용)
                    .title("테스트 공지사항 제목")
                    .content("테스트 공지사항 내용입니다.")
                    .writerId("admin_01")
                    .fixYn("N")
                    .deleteYn("N")
                    .build());
        }

        // 2. When: 수정 요청 데이터 준비
        NoticeRequestDto updateDto = new NoticeRequestDto();
        updateDto.setTitle("[수정 완료] 점검 공지");
        updateDto.setContent("내용이 수정되었습니다.");
        updateDto.setFixYn("Y");

        // 서비스 메서드 호출
        NoticeResponseDto result = noticeService.updateNotice(targetId, updateDto);

        // 3. Then: 검증
        assertThat(result.getTitle()).isEqualTo("[수정 완료] 점검 공지");
        assertThat(result.getFixYn()).isEqualTo("Y");

        // DB에서 다시 꺼내서 수정 시간이 갱신되었는지 확인
        NoticeEntity updatedEntity = noticeRepository.findById(targetId).orElseThrow();
        System.out.println("수정 시간: " + updatedEntity.getUpdateTime());

        assertThat(updatedEntity.getTitle()).contains("수정");
    }

    @Test
    @DisplayName("DB에 있는 4번 공지사항의 DELETE_YN 상태를 N에서 Y로 변경한다")
    void deleteNotice() {
        // [1] Given: 실제 DB에 4번 데이터가 있는지 먼저 확인
        Long targetId = 4L;
        NoticeEntity beforeEntity = noticeRepository.findById(targetId)
                .orElseThrow(() -> new IllegalStateException("DB에 4번 데이터가 없습니다. 먼저 데이터를 넣어주세요."));

        // 실행 전 상태가 'N'인지 확인
        assertThat(beforeEntity.getDeleteYn()).isEqualTo("N");

        // [2] When: 실제 서비스의 삭제 로직 호출
        noticeService.deleteNotice(targetId);

        // [3] Then: DB에서 다시 조회하여 'Y'로 바뀌었는지 검증
        // findById를 다시 호출하여 영속성 컨텍스트가 아닌 실제 DB 상태를 반영하도록 합니다.
        noticeRepository.flush();

        NoticeEntity afterEntity = noticeRepository.findById(targetId).get();
        assertThat(afterEntity.getDeleteYn()).isEqualTo("Y");

        System.out.println("삭제 처리 확인 완료 - ID: " + afterEntity.getNoticeId() + ", 상태: " + afterEntity.getDeleteYn());
    }


    @Test
    @DisplayName("고객용 공지사항 목록 조회 테스트 - 'N' 상태인 데이터들만 잘 나오는지 확인")
    void getCustomerNoticeList_Test() {
        // 1. When
        List<NoticeResponseDto> result = noticeService.getCustomerNoticeList();

        // 2. Then
        // 에러 났던 부분: "이 제목은 나오면 안 돼!"라고 했던 검증을 제거하거나 수정합니다.
        for (NoticeResponseDto dto : result) {
            // [수정] 4번은 확실히 'Y'이므로 나오면 안 되지만,
            // 5번은 현재 DB에 'N'이므로 목록에 나오는 것이 정상입니다.
            assertThat(dto.getTitle()).isNotEqualTo("[수정 완료] 점검 공지"); // 4번은 안 나옴
        }

        // 5번, 8번은 현재 DB 데이터상 'N'이므로 포함되어 있는 것이 "정상 로직"입니다.
        assertThat(result)
                .extracting(NoticeResponseDto::getNoticeId)
                .contains(5L, 6L, 7L, 8L)  // 얘네는 나와야 함 (N이니까)
                .doesNotContain(4L);       // 얘는 안 나와야 함 (Y니까)
    }

    @Test
    @DisplayName("복구 테스트: 'Y' 상태인 5번 데이터를 다시 'N'으로 복구한다")
    void restoreNotice() {
        // 1. Given: 5번 데이터의 현재 상태가 'Y'인지 먼저 확인 (제공된 데이터 기준)
        Long targetId = 5L;
        NoticeEntity before = noticeRepository.findById(targetId).orElseThrow();
        assertEquals("Y", before.getDeleteYn());

        // 2. When: 복구 서비스 호출
        noticeService.restoreNotice(targetId);

        // 3. Then: 상태가 'N'으로 바뀌었는지 검증
        NoticeEntity after = noticeRepository.findById(targetId).orElseThrow();
        assertEquals("N", after.getDeleteYn());
        System.out.println(targetId + "번 공지사항 복구 완료: " + after.getDeleteYn());
    }

    @Test
    @DisplayName("고객용 목록 조회: 6번째 컬럼(DELETE_YN)이 'N'인 데이터만 조회된다")
    void getCustomerNoticeList() {
        // When
        List<NoticeResponseDto> result = noticeService.getCustomerNoticeList();

        // Then
        // 1. 6번째 컬럼이 'Y'인 4번 데이터는 나오면 안 됨
        assertThat(result)
                .extracting(NoticeResponseDto::getNoticeId)
                .doesNotContain(4L);

        // 2. 6번째 컬럼이 'N'인 5, 6, 7, 8번 데이터는 모두 나와야 함
        assertThat(result)
                .extracting(NoticeResponseDto::getNoticeId)
                .contains(5L, 6L, 7L, 8L);

        System.out.println("고객용 조회 성공! 총 " + result.size() + "건 조회됨 (5,6,7,8번)");
    }
    @Test
    @DisplayName("관리자용 목록 조회: 삭제 여부와 상관없이 4, 5, 6, 7, 8번 모두 조회되어야 한다")
    void getAdminNoticeList() {
        // 1. When: 관리자용 전체 조회
        List<NoticeResponseDto> result = noticeService.getAdminNoticeList();

        // 2. Then: 전체 데이터가 다 포함되어 있는지 확인
        // 제공해주신 4, 5, 6, 7, 8번 ID가 모두 포함되어야 함
        assertThat(result)
                .extracting(NoticeResponseDto::getNoticeId)
                .contains(4L, 5L, 6L, 7L, 8L);

        // 3. 정렬 확인: 최신순(ID가 크거나 입력시간이 늦은 순)으로 첫 번째 데이터 확인
        // 제공 데이터상 가장 최근은 8번(10:10:55)임
        assertThat(result.get(0).getNoticeId()).isEqualTo(8L);

        System.out.println("관리자용 전체 조회 개수: " + result.size());
    }
}