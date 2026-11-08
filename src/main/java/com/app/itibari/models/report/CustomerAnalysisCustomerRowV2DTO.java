package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Customer Analysis V2 paged customer analytics row.")
public class CustomerAnalysisCustomerRowV2DTO {

    private UUID customerPublicId;

    @Schema(description = "Display name for Customer Name column: store_name when present; "
            + "otherwise first_name + ' ' + last_name. Null only when all are blank.")
    private String shopName;

    private UUID regionPublicId;

    private String regionName;

    private UUID organizationLocationPublicId;

    private String organizationLocationName;

    private String landmark;

    private long deliveredOrderCount;

    @Schema(description = "Supplier portal COGS (TZS) for this customer in the window: Σ(product.buying_price × quantity_delivered). Current buying_price — not frozen at order time.")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Settled tonnes: Σ(product.weight × quantity_delivered).")
    private BigDecimal settledTonnes;

    @Schema(description = "UI list: latest delivered order date in the window. "
            + "CUSTOMERS export (per-product/order rows): the date of that specific order.")
    private LocalDate lastOrderDate;

    @Schema(description = "Distinct supplier product names ordered by this customer in the window, comma-separated. CUSTOMERS export: a single product name.")
    private String productNames;

    @Schema(description = "Per-product delivered quantities in the window, e.g. 'Sugar: 50, Flour: 30'. CUSTOMERS export: numeric quantity for the product/order.")
    private String productPurchaseQuantities;

    @Schema(description = "Per-product purchase dates in the window, e.g. 'Sugar: 15/01/2026, 22/03/2026 | Flour: 20/02/2026'. CUSTOMERS export: the order date.")
    private String productPurchaseDates;
}
