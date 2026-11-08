package com.app.itibari.models.response;

import lombok.Builder;

@Builder
public record TicketHistoryResponse(Long id,
                                    String actionedBy,
                                    String fromDepartment,
                                    String toDepartment,
                                    String statusFrom,
                                    String statusTo,
                                    String reason,
                                    String date,
                                    String time) {


}
