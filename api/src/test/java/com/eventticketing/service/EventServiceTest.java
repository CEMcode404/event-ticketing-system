package com.eventticketing.service;

import com.eventticketing.IntegrationTestBase;
import com.eventticketing.dto.CreateEventRequest;
import com.eventticketing.dto.EventResponse;
import com.eventticketing.enums.EventStatus;
import com.eventticketing.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventServiceTest extends IntegrationTestBase {

    @Autowired
    EventService eventService;

    @Autowired
    EventRepository eventRepository;

    @Test
    void publishingIsPersistedToTheDatabase() {
        Instant now = Instant.now();
        EventResponse created = eventService.createEvent(new CreateEventRequest(
                "Test Show", "Test Venue", null, now.plus(Duration.ofHours(1)), now.plus(Duration.ofHours(2))));

        eventService.updateStatus(created.id(), EventStatus.PUBLISHED);

        EventStatus storedStatus = eventRepository.findById(created.id()).orElseThrow().getStatus();
        assertThat(storedStatus).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void anEventThatHasStartedCannotChangeStatus() {
        Instant now = Instant.now();
        EventResponse started = eventService.createEvent(new CreateEventRequest(
                "Past Show", "Test Venue", null, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1))));

        assertThatThrownBy(() -> eventService.updateStatus(started.id(), EventStatus.PUBLISHED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("closed");
    }
}