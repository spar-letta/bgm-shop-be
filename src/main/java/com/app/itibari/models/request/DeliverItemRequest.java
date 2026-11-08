package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeliverItemRequest {

    @NotNull(message = "Route public ID cannot be null")
    private UUID routePublicId;

    @NotNull(message = "Sales order public ID cannot be null")
    private UUID salesOrderPublicId;

    private List<ItemRequest> items = new ArrayList<>();
}
