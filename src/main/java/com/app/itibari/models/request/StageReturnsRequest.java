package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StageReturnsRequest {

    private Integer quantity;

    private UUID productItemPublicId;
}
