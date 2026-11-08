package com.app.itibari.models.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderItemsRequest {

    private UUID productId;

    private int quantity;
}
