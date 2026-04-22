package com.simplecoding.chargerreservation.station.controller;

import com.simplecoding.chargerreservation.station.dto.MarkerDto;
import com.simplecoding.chargerreservation.station.dto.StationDto;
import com.simplecoding.chargerreservation.station.dto.StationStatsDto;
import com.simplecoding.chargerreservation.station.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Tag(name = "충전소 API", description = "충전소 조회 / 검색 / 마커 / 통계 API")
@Slf4j
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @Operation(summary = "메인페이지 통계 조회", description = "총 충전소 수 / 총 충전기 수 / 가용 충전기 수를 반환합니다")
    @GetMapping("/stats")
    public ResponseEntity<StationStatsDto> getStationStats() {
        try {
            StationStatsDto stats = stationService.getStationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("통계 조회 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "지도 마커 조회", description = "위도/경도 기준 반경 1.5km 이내 충전소 마커 정보를 반환합니다")
    @GetMapping("/markers")
    public ResponseEntity<List<MarkerDto>> getMarkers(
            @RequestParam Double lat,
            @RequestParam Double lng) {

        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
            return ResponseEntity.ok(List.of());
        }

        try {
            List<MarkerDto> markers = stationService.getStationMarkers(lat, lng);
            log.info("마커 로드 완료: {}건", markers.size());
            return ResponseEntity.ok(markers);
        } catch (Exception e) {
            log.error("마커 조회 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "충전소 검색", description = "키워드와 현재 위치로 주변 충전소를 검색합니다 (작년 요금 비교 포함)")
    @GetMapping("/search")
    public ResponseEntity<List<StationDto>> searchStations(
            @RequestParam(required = false) String keyword, // 키워드 없이 위치로만 검색할 경우 대비
            @RequestParam Double lat,
            @RequestParam Double lng) {

        log.info("[검색 요청] 키워드: '{}', 위도: {}, 경도: {}", keyword, lat, lng);

        // 1. 서비스 호출 (이미 MarkerProjection을 통해 요금 계산이 완료된 StationDto 리스트를 반환함)
        List<StationDto> results = stationService.searchStationsNearby(keyword, lat, lng);

        // 2. 검색 결과가 없을 경우 204 No Content 대신 빈 리스트를 200 OK로 반환 (프론트엔드 처리 편의성)
        if (results.isEmpty()) {
            log.info("[검색 결과] 일치하는 충전소가 없습니다.");
            return ResponseEntity.ok(Collections.emptyList());
        }

        log.info("[검색 성공] 총 {}건의 충전소 발견", results.size());
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "충전소 상세 조회", description = "statId 로 특정 충전소의 상세 정보 / 실시간 현황 / 요금 정보를 반환합니다")
    @GetMapping("/{statId}")
    public ResponseEntity<StationDto> getStationDetail(
            @PathVariable String statId,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng) {

        String currentMonth = String.format("%02d", LocalDate.now().getMonthValue());
        double lat = (userLat != null && userLat != 0.0) ? userLat : 35.1485;
        double lng = (userLng != null && userLng != 0.0) ? userLng : 129.0637;

        log.info("[상세 조회] ID: {}, 위치: ({}, {})", statId, lat, lng);

        try {
            StationDto detail = stationService.getStationDetail(statId, currentMonth, lat, lng);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            log.warn("상세 조회 결과 없음 (ID: {}): {}", statId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("상세 조회 서버 에러 (ID: {}): {}", statId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "주변 충전소 목록 조회", description = "위도/경도 기준 반경 1.5km 이내 충전소 100개를 반환합니다")
    @GetMapping("/around")
    public ResponseEntity<List<StationDto>> getAroundStations(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "급속") String type) {

        if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
            log.warn("[API] 유효하지 않은 위치 정보");
            return ResponseEntity.ok(List.of());
        }

        try {
            List<StationDto> stations = stationService.getStationsWithDistancePaged(lat, lng, page);
            log.info("[주변 목록] {}건 반환 (위도: {}, 경도: {})", stations.size(), lat, lng);
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            log.error("[목록 조회] 서버 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}