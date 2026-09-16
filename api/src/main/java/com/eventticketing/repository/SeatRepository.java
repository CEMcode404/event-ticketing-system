package com.eventticketing.repository;

import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    Page<Seat> findByEventIdAndStatus(UUID eventId, SeatStatus status, Pageable pageable);
}