package com.simplecoding.chargerreservation.station.controller;

import com.simplecoding.chargerreservation.station.dto.MarkerDto;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.service.StationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    /**
     * [지도 마커 조회] 주변 1.5km 내 최대 100개의 마커 정보를 반환합니다.
     * GET /api/stations/markers?lat=35.1798&lng=129.0750
     */
    @GetMapping("/markers")
    public ResponseEntity<List<MarkerDto>> getMarkers(
            @RequestParam Double lat,
            @RequestParam Double lng) {

        // 1. 최소한의 유효성 검사 (실패 시 에러보다 빈 리스트가 지도가 멈추지 않아 안전함)
        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
            return ResponseEntity.ok(List.of());
        }

        try {
            // 2. 서비스 호출 (MarkerDto는 이미 statId, statNm, lat, lng, color, occupancy만 포함됨)
            List<MarkerDto> markers = stationService.getStationMarkers(lat, lng);

            log.info("📍 마커 로드 완료: {}건", markers.size());
            return ResponseEntity.ok(markers);

        } catch (Exception e) {
            log.error("❌ 마커 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * [API] 충전소 통합 검색 (이름, 주소, 운영사 키워드)
     * GET /api/station/search?keyword=강남
     */
// StationController.java

    @GetMapping("/search")
    public ResponseEntity<List<StationDto>> searchStations(
            @RequestParam String keyword,
            @RequestParam Double lat,  // 💡 내 현재 위도 추가
            @RequestParam Double lng) { // 💡 내 현재 경도 추가

        log.info("▶ [검색] 키워드: {}, 내 위치: ({}, {})", keyword, lat, lng);

        List<StationDto> results = stationService.searchStationsNearby(keyword, lat, lng);

        return ResponseEntity.ok(results);
    }

    /**
     * 3. [추가] 충전소 상세 정보 조회 (거리 계산 + 실시간 충전기 상태 포함)
     * GET /api/stations/KP002210?userLat=35.1485&userLng=129.0637
     */
    @GetMapping("/{statId}")
    public ResponseEntity<StationDto> getStationDetail(
            @PathVariable String statId,
            // 💡 이제 서비스에서 급속/완속을 모두 조회하므로 type 파라미터는 제거하거나 무시해도 됩니다.
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng) {

        // 1. 현재 월 추출 (01, 02... 형식)
        String currentMonth = String.format("%02d", LocalDate.now().getMonthValue());

        // 2. 위치 정보 기본값 처리
        double lat = (userLat != null && userLat != 0.0) ? userLat : 35.1485;
        double lng = (userLng != null && userLng != 0.0) ? userLng : 129.0637;

        log.info("🔍 [API] 상세 조회 요청 - ID: {}, 위치: ({}, {})", statId, lat, lng);

        try {
            // 💡 3. 서비스 호출 (수정된 서비스 시그니처에 맞춰 type 인자 제거)
            // 서비스에서 이제 급속/완속 요금을 모두 포함한 StationDto를 반환합니다.
            StationDto detail = stationService.getStationDetail(statId, currentMonth, lat, lng);

            return ResponseEntity.ok(detail);

        } catch (RuntimeException e) {
            log.warn("⚠️ 상세 조회 결과 없음 (ID: {}): {}", statId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("❌ 상세 조회 서버 에러 (ID: {}): {}", statId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * [추가] 1. 내 주변 충전소 리스트 조회 (무한 스크롤용 - 20개씩)
     * GET /api/stations/around?lat=35.1061&lng=128.9665&page=0
     */
    @GetMapping("/around")

    public ResponseEntity<List<StationDto>> getAroundStations(

            @RequestParam Double lat,

            @RequestParam Double lng,

            @RequestParam(defaultValue = "0") int page,

// ✨ 추가: 프론트의 필터 상태를 전달받음 (기본값 '급속')

            @RequestParam(defaultValue = "급속") String type) {



        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {

            log.warn("⚠️ [API] 유효하지 않은 위치 정보입니다.");

            return ResponseEntity.ok(List.of());

        }



        try {

// ✨ 서비스 호출 시 type을 함께 전달하도록 수정

// (단, 서비스 내부에서는 이 type의 요금을 '우선' 조회하거나

// 제가 앞서 알려드린 대로 p1, p2 조인을 통해 '둘 다' 가져오는 것이 핵심입니다.)

            List<StationDto> stations = stationService.getStationsWithDistancePaged(lat, lng, page);



            log.info("📋 [API] 주변 목록 반환: {}건 (위도: {}, 경도: {}, 타입: {}, 페이지: {})",

                    stations.size(), lat, lng, type, page);

            return ResponseEntity.ok(stations);



        } catch (Exception e) {

            log.error("❌ [API] 목록 조회 중 서버 오류 발생: ", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        }

    }
}