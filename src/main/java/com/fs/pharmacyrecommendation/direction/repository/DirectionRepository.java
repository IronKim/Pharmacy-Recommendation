package com.fs.pharmacyrecommendation.direction.repository;

import com.fs.pharmacyrecommendation.direction.entity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectionRepository extends JpaRepository<Direction, Long> {
}
