package com.app.itibari.models.request;

import com.app.itibari.dataTypes.BnplCallStatus;
import com.app.itibari.dataTypes.BnplLeadStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplCallOutcomeRequest {

    private BnplCallStatus callStatus;
    private BnplLeadStatus leadStatus;
    private String notes;
    private LocalDate scheduledDate;
    private List<UUID> reasonCodePublicIds;
    private UUID callStatusPublicId;
}