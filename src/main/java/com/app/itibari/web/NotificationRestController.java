package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.Notification;
import com.app.itibari.services.PersonService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Notification", description = "Notification API")
@RestController
@RequestMapping("/notification")
public class NotificationRestController {

    private final PersonService personService;

    public NotificationRestController(PersonService personService) {

        this.personService = personService;
    }

    @JsonView(BaseView.NotificationView.class)
    @GetMapping
    @Operation(summary = "Get all person notifications", description = "Get all person notifications",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "Get_notification", value = Examples.FETCH_NOTIFICATIONS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Notification> getLoggedInPersonNotifications(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "isRead", required = false) Boolean isRead,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return personService.getLoggedInPersonNotifications(searchParam, isRead, principal.getName(), pageable);
    }

    @PatchMapping("/{publicId}/read")
    @Operation(summary = "Mark notification as read", description = "Mark notification as read",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void markNotificationAsRead(
            @PathVariable("publicId") String notificationPublicId,
            @Parameter(hidden = true) Principal principal) {

        personService.markNotificationAsRead(notificationPublicId, principal.getName());
    }
}
