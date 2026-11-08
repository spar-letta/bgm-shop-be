package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductTransformerDTO {

    private UUID publicId;

    private String productName;

    private CategoryDTO category;

    private ProductClassDTO productClassDTO;

    private ProductSubClassDTO productSubClassDTO;

    private String sku;

    private BigDecimal sellPrice;

    private String unitOfMeasure;

    private String status;

    private Long availableStock;

    private Long totalStock;

    private Boolean inActive;

    private Boolean productBestSelling;

    private Integer discountMoq;

    private String description;

    private String productUrl;

    /** Alias aligned with POS inventory {@code imageUrl}. */
    private String imageUrl;

    private DiscountDetails discountDetails;

    private Long availableStockItems;

    private Long totalStockItems;

    private String brand;

    private boolean newProduct;

}
