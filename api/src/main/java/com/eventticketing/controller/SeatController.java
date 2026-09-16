package com.eventticketing.controller;

import com.eventticketing.dto.BulkGenerateSeatsRequest;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<SeatResponse>> bulkGenerate(@Valid @RequestBody BulkGenerateSeatsRequest request) {
        List<SeatResponse> seats = seatService.bulkGenerate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seats);
    }
}