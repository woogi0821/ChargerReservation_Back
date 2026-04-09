//package com.simplecoding.chargerreservation.notice.repository;
//
//
//import com.simplecoding.chargerreservation.notice.entity.NoticeEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {
//
//    // 삭제 안 된 공지 전체 조회 (고정 우선 → 최신순)
//    @Query("SELECT n FROM NoticeEntity n WHERE n.isDeleted = false " +
//            "ORDER BY n.isPinned DESC, n.createdAt DESC")
//    List<NoticeEntity> findAllActive();
//
//    // 삭제 안 된 단건 조회
//    Optional<NoticeEntity> findByNoticeIdAndIsDeletedFalse(Long noticeId);
//
//    // noticeNo 최대값 조회 (채번용)
//    @Query("SELECT MAX(n.noticeNo) FROM NoticeEntity n")
//    String findMaxNoticeNo();
//}