package com.eventticketing.service;

import com.eventticketing.dto.PublicEventResponse;
import com.eventticketing.entity.Event;
import com.eventticketing.dto.CreateEventRequest;
import com.eventticketing.dto.EventResponse;
import com.eventticketing.enums.EventStatus;
import com.eventticketing.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event(request.name(), request.venue(), request.description(), request.saleOpensAt());
        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    public EventResponse updateEvent(UUID eventId, CreateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setName(request.name());
        event.setVenue(request.venue());
        event.setDescription(request.description());
        event.setSaleOpensAt(request.saleOpensAt());

        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    public EventResponse getEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return EventResponse.from(event);
    }

    public Page<EventResponse> listEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse updateStatus(UUID eventId, EventStatus newStatus) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled events cannot change status");
        }

        event.setStatus(newStatus);
        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PublicEventResponse> listPublished(Pageable pageable) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(PublicEventResponse::from);
    }

    @Transactional(readOnly = true)
    public PublicEventResponse getPublished(UUID id) {
        return eventRepository.findById(id)
                .filter(event -> event.getStatus() == EventStatus.PUBLISHED)
                .map(PublicEventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}