package com.app.itibari.converter;

import com.app.itibari.models.entities.TicketHistory;
import com.app.itibari.models.response.TicketHistoryResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TicketHistoryToResponse implements Converter<TicketHistory, TicketHistoryResponse> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public TicketHistoryResponse convert(TicketHistory ticketHistory) {

        if (ticketHistory == null) {
            return null;
        }

        LocalDateTime creationTime = ticketHistory.getActionedTime();
        String dateString = creationTime.toLocalDate().format(DATE_FORMATTER);
        String timeString = creationTime.format(TIME_FORMATTER);

        return TicketHistoryResponse.builder()
                .id(ticketHistory.getId())
                .actionedBy(ticketHistory.getActionedBy())
                .fromDepartment(ticketHistory.getFromDepartment())
                .toDepartment(ticketHistory.getToDepartment())
                .statusFrom(ticketHistory.getStatusFrom())
                .statusTo(ticketHistory.getStatusTo())
                .reason(ticketHistory.getReason())
                .time(timeString)
                .date(dateString)
                .build();
    }
}
