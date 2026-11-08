package com.app.itibari.converter;

import com.app.itibari.models.entities.TicketViewer;
import com.app.itibari.models.response.TicketViewerResponse;
import com.app.itibari.utils.formatter.DateFormatter;
import org.springframework.stereotype.Component;

@Component
public class TicketViewerToResponse implements Converter<TicketViewer, TicketViewerResponse> {
    @Override
    public TicketViewerResponse convert(TicketViewer ticketViewer) {
        if (ticketViewer == null){
            return null;
        }

        return TicketViewerResponse.builder()
                .department(ticketViewer.getDepartment())
                .viewedAt(DateFormatter.formatDate(ticketViewer.getViewedAt()))
                .viewedBy(ticketViewer.getViewedBy())
                .build();
    }
}
