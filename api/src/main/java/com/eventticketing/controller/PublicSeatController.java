package com.eventticketing.controller;

import com.eventticketing.dto.HoldSeatResponse;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seats")
public class PublicSeatController {

    private final SeatService seatService;

    public PublicSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> browse(@RequestParam UUID eventId) {
        return ResponseEntity.ok(seatService.browse(eventId));
    }

    @PostMapping("/{seatId}/hold")
    public ResponseEntity<HoldSeatResponse> hold(@PathVariable UUID seatId) {
        return ResponseEntity.ok(seatService.hold(seatId));
    }
}