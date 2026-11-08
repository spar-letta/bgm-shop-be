package com.app.itibari.models.report;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Rich customer detail for Supplier Portal customer locations V2.")
public class CustomerLocationDetailV2DTO {

    private UUID customerPublicId;

    private String customerName;

    @Schema(description = "Masked phone number for supplier portal display when phoneVisible is true.")
    private String phoneNumber;

    @Schema(description = "Explicit flag for whether the phone field should be shown; do not infer from phoneNumber.")
    private boolean phoneVisible;

    private Double latitude;

    private Double longitude;

    private CustomerLocationMarkerCategoryV2 category;

    private UUID regionPublicId;

    private String regionName;

    private UUID organizationLocationPublicId;

    private String organizationLocationName;

    private String landmark;

    private String address;

    @Schema(description = "Present only when both startDate and endDate are supplied on the request.")
    private CustomerLocationPeriodMetricsV2DTO periodMetrics;
}
