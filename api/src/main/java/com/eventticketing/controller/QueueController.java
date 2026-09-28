package com.eventticketing.controller;

import com.eventticketing.dto.QueueJoinResponse;
import com.eventticketing.dto.QueueStatusResponse;
import com.eventticketing.service.WaitingRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/queue/{eventId}")
public class QueueController {

    private final WaitingRoomService waitingRoomService;

    public QueueController(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    @PostMapping("/join")
    public ResponseEntity<QueueJoinResponse> join(@PathVariable UUID eventId) {
        return ResponseEntity.ok(waitingRoomService.join(eventId));
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> status(
            @PathVariable UUID eventId,
            @RequestHeader("X-Queue-Session") String sessionId
    ) {
        return ResponseEntity.ok(waitingRoomService.status(eventId, sessionId));
    }
}