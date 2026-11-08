package com.app.itibari.models.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GoodsNoteItemRequest {

    @Min(value = 1, message = "Quantity must be greater than 0")
    private long quantity;

    private UUID itemPublicId;

    private UUID rejectionCodePublicId;
}
