package com.eventticketing.scheduler;

import com.eventticketing.service.WaitingRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdmissionScheduler.class);

    private final WaitingRoomService waitingRoomService;

    public AdmissionScheduler(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    @Scheduled(fixedDelayString = "${waitingroom.admit-interval-ms:5000}")
    public void tick() {
        for (UUID eventId : waitingRoomService.activeEventIds()) {
            try {
                waitingRoomService.sweepAbandoned(eventId);
                waitingRoomService.admitIfOpen(eventId);
            } catch (Exception e) {
                log.error("Admission tick failed for event {}", eventId, e);
            }
        }
    }
}