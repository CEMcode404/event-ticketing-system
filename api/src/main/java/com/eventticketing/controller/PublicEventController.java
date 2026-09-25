package com.eventticketing.controller;

import com.eventticketing.dto.PublicEventResponse;
import com.eventticketing.enums.EventStatus;
import com.eventticketing.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/events")
public class PublicEventController {

    private final EventRepository eventRepository;

    public PublicEventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public Page<PublicEventResponse> listPublished(
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "saleOpensAt"),
                    @SortDefault(sort = "id")
            })
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(PublicEventResponse::from);
    }
}