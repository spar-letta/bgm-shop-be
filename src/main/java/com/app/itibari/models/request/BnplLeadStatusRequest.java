package com.app.itibari.models.request;

import com.app.itibari.dataTypes.BnplLeadStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplLeadStatusRequest {

    private BnplLeadStatus leadStatus;
}