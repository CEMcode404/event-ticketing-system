package com.eventticketing.service;

import com.eventticketing.entity.Event;
import com.eventticketing.dto.CreateEventRequest;
import com.eventticketing.dto.EventResponse;
import com.eventticketing.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final EventRepository eventRepository;

    public AdminService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event(request.name(), request.saleOpensAt());
        Event saved = eventRepository.save(event);
        return EventResponse.from(saved);
    }

    public Page<EventResponse> listEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(EventResponse::from);
    }
}