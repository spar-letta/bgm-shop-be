package com.app.itibari.models.request;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DiscountCreateRequest {

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    private AmountType amountType;

    private BigDecimal amount;

    private BigDecimal productValue;

    @Deprecated
    private Integer minQuantity;

    @Deprecated
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    @Deprecated
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    private List<UUID> customerIds;

    private List<UUID> productIds;

    private BigDecimal invoiceAmountThreshold;

    public List<UUID> getProductIds() {

        return productIds == null ? Collections.emptyList() : productIds;
    }
}
