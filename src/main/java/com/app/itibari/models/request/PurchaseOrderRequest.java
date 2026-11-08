package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PurchaseOrderRequest {

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate orderDate;

    private String comment;

    @NotNull(message = "Supplier id is required")
    private UUID supplierId;

    private List<PurchaseOrderItemRequest> purchaseOrderItems;

    private String proformaInvoiceNumber;

    private UUID warehouseId;

    private boolean rebate;

    private UUID reasonCodePublicId;


    public LocalDate getOrderDate() {
        return orderDate==null? LocalDate.now() : orderDate;
    }
}
