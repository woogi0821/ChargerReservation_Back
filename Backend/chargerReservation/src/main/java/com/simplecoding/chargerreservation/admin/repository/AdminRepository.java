package com.simplecoding.chargerreservation.admin.repository;

import com.simplecoding.chargerreservation.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin,Long> {
    Optional<Admin> findByMemberId(long memberId);
}