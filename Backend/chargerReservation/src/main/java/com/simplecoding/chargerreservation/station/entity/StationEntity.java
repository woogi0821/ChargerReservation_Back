package com.simplecoding.chargerreservation.station.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

/**
 * 전기차 충전소 정보 엔티티 (DB Table: STATION)
 * 오라클 테이블 명세서의 16개 컬럼을 기준으로 작성되었습니다.
 */
@Entity
@Table(name = "STATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@DynamicUpdate // 변경된 필드만 선택적으로 SQL Update를 수행하여 성능을 최적화합니다.
public class StationEntity {

    @Id
    @Column(name = "STAT_ID", length = 50)
    private String statId; // 1. 충전소 ID (Primary Key)

    @Column(name = "STAT_NM", length = 500, nullable = false)
    private String statNm; // 2. 충전소 명칭 (Not Null)

    @Column(name = "ADDR", length = 2000)
    private String addr; // 3. 지번/도로명 주소

    @Column(name = "LOCATION", length = 2000)
    private String location; // 4. 상세 위치 (예: 주차장 지하 2층)

    @Column(name = "LAT")
    private Double lat; // 5. 위도 (Latitude, NUMBER 12,8)

    @Column(name = "LNG")
    private Double lng; // 6. 경도 (Longitude, NUMBER 12,8)

    @Column(name = "USE_TIME", length = 500)
    private String useTime; // 7. 이용 가능 시간 (예: 24시간, 09:00~18:00)

    @Column(name = "BNM", length = 300)
    private String bnm; // 8. 운영기관명 (예: 환경부, 한국전력)

    @Column(name = "ZCODE", length = 10)
    private String zcode; // 9. 시도 코드 (법정동 코드 앞 2자리)

    @Column(name = "ZSCODE", length = 10)
    private String zscode; // 10. 구군 코드 (법정동 코드 앞 5자리)

    @Column(name = "KIND", length = 50)
    private String kind; // 11. 충전소 구분 코드

    @Column(name = "PARKING_FREE", length = 1)
    private String parkingFree; // 12. 주차료 무료 여부 (Y: 무료, N: 유료)

    @Column(name = "LIMIT_YN", length = 1)
    private String limitYn; // 13. 이용 제한 여부 (Y: 제한있음, N: 제한없음)

    @Column(name = "LIMIT_DETAIL", length = 2000)
    private String limitDetail; // 14. 이용 제한 상세 사유 (예: 아파트 입주민 전용)

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt; // 15. 데이터 최초 등록 일시 (Default: SYSDATE)

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt; // 16. 데이터 최종 수정 일시 (스케줄러 동기화 시점)

}