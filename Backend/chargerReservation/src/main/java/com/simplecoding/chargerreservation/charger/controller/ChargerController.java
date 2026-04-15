package com.simplecoding.chargerreservation.charger.controller;

import com.simplecoding.chargerreservation.charger.dto.ChargerDto;
import com.simplecoding.chargerreservation.charger.repository.ChargerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerRepository chargerRepository;

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
