package com.app.itibari.models.request;

import com.app.itibari.dataTypes.AmendAction;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AmendOrderRequest {

    private UUID deliveryAgentPublicId;

    private UUID salesOrderPublicId;

    private UUID routePublicId;

    private UUID reasonPublicId;

    private String description;

    private AmendAction amendAction;

    private List<ItemRequest> items = new ArrayList<>();
}
