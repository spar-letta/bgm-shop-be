package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Events;
import com.app.itibari.services.EventService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Events", description = "Events API")
@RestController
@RequestMapping("/events")
public class EventsRestController {

    private final EventService eventService;

    public EventsRestController(EventService eventService) {

        this.eventService = eventService;
    }

    @JsonView({BaseView.EventsView.class})
    @GetMapping("/{publicId}")
    @Operation(summary = "Fetch Event", description = "Fetch Event by publicId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_EVENTS", value = Examples.GET_EVENTS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Events> fetchEvent(@PathVariable UUID publicId,
                                   @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return eventService.getEvent(publicId, pageable);
    }
}
