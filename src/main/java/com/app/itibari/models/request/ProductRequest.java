package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductRequest {

    @NotNull(message = "Product name is required")
    private String productName;

    private UUID categoryPublicId;

    private UUID classPublicId;

    private UUID subClassPublicId;

    private String brand;

    private UUID taxMappingPublicId;

    private BigDecimal buyingPrice;

    private BigDecimal sellPrice;

    private Integer quantity;

    private BigDecimal customTaxAddition;

    private BigDecimal weight;

    private Integer length;

    private Integer width;

    private Integer height;

    private String unitOfMeasurement;

    private String barcode;

    private String packageSize;

    private BigDecimal margin;

    private List<UUID> supplierPublicIds;

    private Integer bulkPurchaseUnit;

    private String bulkPurchaseUnitName;

    private Integer sellingUnit;

    private String description;

    private String sellingUnitName;

    @NotNull(message = "Missing tax code") private UUID taxCodePublicId;

    private UUID productBrandPublicId;

    private boolean notification;

    private List<SupplierTargetRequestDto> supplierTargetRequestDtos = new ArrayList<>();
}
