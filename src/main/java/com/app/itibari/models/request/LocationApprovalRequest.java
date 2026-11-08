package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LocationApprovalRequest {
    private String latitude;
    private String longitude;
    private UUID routePublicId;
    private String comment;
    private UUID orderPublicId;
    private UUID rejectionsCodePublicId;
}
