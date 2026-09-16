package com.eventticketing.controller;

import com.eventticketing.dto.HoldSeatResponse;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.service.SeatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer-facing, unauthenticated on purpose — deliberately NOT under
 * /api/admin/**, so WebConfig's admin guard never applies here.
 *
 * KNOWN GAP (tracked, not forgotten): these endpoints don't yet check
 * waiting-room admission at all — anyone can call them directly, bypassing
 * the queue entirely. The doc requires this ("Backend validates this
 * token on every subsequent shopping API call"), but the admission token
 * itself isn't wired into this project yet (waiting room join/heartbeat
 * still needs porting over). Needs an AdmissionGuardInterceptor here,
 * same shape as AdminAuthInterceptor, once that's done.
 */
@RestController
@RequestMapping("/api/seats")
public class PublicSeatController {

    private final SeatService seatService;

    public PublicSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public ResponseEntity<Page<SeatResponse>> browse(@RequestParam UUID eventId, Pageable pageable) {
        return ResponseEntity.ok(seatService.browse(eventId, pageable));
    }

    @PostMapping("/{seatId}/hold")
    public ResponseEntity<HoldSeatResponse> hold(@PathVariable UUID seatId) {
        return ResponseEntity.ok(seatService.hold(seatId));
    }
}