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
    @GetMapping("/search")
    public ResponseEntity<List<StationDto>> searchStations(@RequestParam(value = "keyword", required = false) String keyword) {
        log.info("▶ [API 호출] 충전소 검색 - 키워드: [{}]", keyword);
// 서비스 호출
        List<StationDto> results = stationService.searchStations(keyword);
        log.info("◀ [API 응답] 검색 완료 - 결과: {}건", results.size());
// 200 OK 상태코드와 함께 결과 반환
        return ResponseEntity.ok(results);
    }

    /**
     * 3. [추가] 충전소 상세 정보 조회 (거리 계산 + 실시간 충전기 상태 포함)
     * GET /api/stations/KP002210?userLat=35.1485&userLng=129.0637
     */
    @GetMapping("/{statId}")
    public ResponseEntity<StationDto> getStationDetail(
            @PathVariable String statId,
            @RequestParam(defaultValue = "급속") String type,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng) {

        // 1. 현재 월 추출 (01, 02... 형식)
        String currentMonth = String.format("%02d", LocalDate.now().getMonthValue());

        // 2. 위치 정보 기본값 처리
        // 프론트에서 위치 권한을 거부했을 때를 대비해 기본 좌표(범내골)를 사용하는 로직은 좋습니다.
        double lat = (userLat != null) ? userLat : 35.1485;
        double lng = (userLng != null) ? userLng : 129.0637;

        log.info("🔍 [API] 상세 조회 요청 - ID: {}, 타입: {}, 위치: ({}, {})", statId, type, lat, lng);

        try {
            // 3. 서비스 호출
            StationDto detail = stationService.getStationDetail(statId, type, currentMonth, lat, lng);

            // 💡 [추가 제안] 충전기 목록(List<ChargerDto>)이 있다면 여기서 추가로 세팅하거나
            // 서비스 내부에서 이미 세팅되어 내려오는지 확인이 필요합니다.

            return ResponseEntity.ok(detail);

        } catch (Exception e) { // RuntimeException보다 넓은 범위의 예외 처리 권장
            log.error("❌ 상세 조회 실패 (ID: {}): {}", statId, e.getMessage());
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
            @RequestParam(defaultValue = "0") int page) {

        // 1. 유효하지 않은 좌표 방어 (0.0 또는 null)
        // 리액트 무한 스크롤이나 리스트 맵핑 시 null보다는 빈 리스트([])가 훨씬 안전합니다.
        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
            log.warn("⚠️ [API] 유효하지 않은 위치 정보입니다. 빈 리스트를 반환합니다.");
            return ResponseEntity.ok(List.of());
        }

        try {
            // 2. 서비스 호출 (거리순 페이징 + 요금/현황 정보 포함)
            List<StationDto> stations = stationService.getStationsWithDistancePaged(lat, lng, page);

            // 3. 결과 로깅 및 응답 반환
            // 데이터가 없더라도 빈 리스트([])를 반환해야 프론트의 .map() 함수가 터지지 않습니다.
            log.info("📋 [API] 주변 목록 반환: {}건 (위도: {}, 경도: {}, 페이지: {})", stations.size(), lat, lng, page);
            return ResponseEntity.ok(stations);

        } catch (Exception e) {
            log.error("❌ [API] 목록 조회 중 서버 오류 발생: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}