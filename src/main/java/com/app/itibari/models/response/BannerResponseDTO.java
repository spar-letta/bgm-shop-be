package com.app.itibari.models.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BannerResponseDTO {

    private String url;
    private String contentType;
    private String description;
    private UUID publicId;
    private boolean active;
    private String category;
    private String categoryName;
    private String productClass;
    private String productClassName;
    private String productSubClass;
    private String productSubClassName;
    private String brand;
    private List<ProductInfo> products;
}
