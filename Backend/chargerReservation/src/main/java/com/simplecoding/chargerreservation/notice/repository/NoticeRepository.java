package com.simplecoding.chargerreservation.notice.repository;

import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {

    // 1. 고객용: 삭제 안 된 것만 최신순 (기존 유지)
    List<NoticeEntity> findByDeleteYnOrderByInsertTimeDesc(String deleteYn);

    // 2. 관리자용: 모든 데이터를 최신순으로 (추가 추천)
    // deleteYn 조건 없이 전체를 가져오되 시간순으로 정렬합니다.
    List<NoticeEntity> findAllByOrderByInsertTimeDesc();

    // 3. 상세 조회: 삭제 안 된 것만 찾기 (기존 유지)
    Optional<NoticeEntity> findByNoticeIdAndDeleteYn(Long noticeId, String deleteYn);
}
