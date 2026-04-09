package com.simplecoding.chargerreservation.chargerPrice.repository;

import com.simplecoding.chargerreservation.chargerPrice.entity.ChargerPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargerPriceRepository extends JpaRepository<ChargerPriceEntity, Long> {

}