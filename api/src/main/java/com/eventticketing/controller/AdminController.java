package com.eventticketing.controller;

import com.eventticketing.dto.CreateEventRequest;
import com.eventticketing.dto.EventResponse;
import com.eventticketing.dto.UpdateEventStatusRequest;
import com.eventticketing.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
public class AdminController {

    private final EventService eventService;

    public AdminController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEventRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.listEvents(pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventStatusRequest request
    ) {
        return ResponseEntity.ok(eventService.updateStatus(id, request.status()));
    }
}