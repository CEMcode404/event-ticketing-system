package com.eventticketing.controller;

import com.eventticketing.dto.CheckoutRequest;
import com.eventticketing.dto.CheckoutResponse;
import com.eventticketing.dto.HoldRequest;
import com.eventticketing.dto.HoldResponse;
import com.eventticketing.dto.SectionAvailabilityResponse;
import com.eventticketing.service.CheckoutService;
import com.eventticketing.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}")
public class ShopController {

    private static final String ADMISSION_HEADER = "X-Admission-Token";

    private final SeatService seatService;
    private final CheckoutService checkoutService;

    public ShopController(SeatService seatService, CheckoutService checkoutService) {
        this.seatService = seatService;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/sections")
    public List<SectionAvailabilityResponse> sections(
            @PathVariable UUID eventId,
            @RequestHeader(ADMISSION_HEADER) String admissionToken
    ) {
        return seatService.sections(eventId, admissionToken);
    }

    @PostMapping("/holds")
    public ResponseEntity<HoldResponse> hold(
            @PathVariable UUID eventId,
            @RequestHeader(ADMISSION_HEADER) String admissionToken,
            @Valid @RequestBody HoldRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatService.holdSeats(eventId, admissionToken, request));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @PathVariable UUID eventId,
            @RequestHeader(ADMISSION_HEADER) String admissionToken,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.startCheckout(eventId, admissionToken, request));
    }
}