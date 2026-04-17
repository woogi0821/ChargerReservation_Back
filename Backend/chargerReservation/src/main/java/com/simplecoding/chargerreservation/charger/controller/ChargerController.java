package com.simplecoding.chargerreservation.charger.controller;

import com.simplecoding.chargerreservation.charger.dto.ChargerDto;
import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "충전기 API", description = "충전기 조회 API")
@RestController
@RequestMapping("api/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerRepository chargerRepository;

    @Operation(summary = "충전소별 충전기 목록 조회", description = "충전소 ID(statId)로 해당 충전소의 충전기 목록을 조회합니다")
    @GetMapping("/station/{statId}")
    public ResponseEntity<List<ChargerDto>> getChargerByStation(
            @PathVariable String statId) {
        List<ChargerDto> result = chargerRepository
                .findByStatIdWithStation(statId.trim().toUpperCase())
                .stream()
                .map(e -> ChargerDto.fromEntityWithStation(e, e.getStation()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}