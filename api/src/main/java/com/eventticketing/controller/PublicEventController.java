package com.eventticketing.controller;

import com.eventticketing.dto.PublicEventResponse;
import com.eventticketing.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public Page<PublicEventResponse> listPublished(
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "startsAt"),
                    @SortDefault(sort = "id")
            })
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventService.listPublished(pageable);
    }

    @GetMapping("/{id}")
    public PublicEventResponse get(@PathVariable UUID id) {
        return eventService.getPublished(id);
    }
}