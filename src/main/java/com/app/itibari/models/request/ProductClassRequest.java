package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductClassRequest {

    private String name;

    private String description;

    private UUID categoryPublicId;
}
