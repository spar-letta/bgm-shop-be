package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BannerRequest {
    private Boolean hasProducts;
    private List<UUID> products;
    private UUID categoryPublicId;
    private UUID classPublicId;
    private UUID subClassPublicId;
    private String brand;
}
