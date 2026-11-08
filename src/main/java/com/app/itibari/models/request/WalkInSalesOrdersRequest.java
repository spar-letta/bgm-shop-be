package com.app.itibari.models.request;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Builder
public record WalkInSalesOrdersRequest(@NotNull UUID customerId,
                                       String sourceChannel,
                                       @NotEmpty Set<OrderItemsRequest> orderItemsRequest) {

    @Builder
    public record OrderItemsRequest(@NotNull UUID productId,
                                    @Min(1) int quantity,
                                    BigDecimal volumeDiscount,  // qty * discount given
                                    BigDecimal specialDiscount ) {

        public OrderItemsRequest{
            if (quantity <= 0){
                throw new BadRequestRestApiException()
                        .developerMessage("Hi! quantity must be greater than 0")
                        .userMessage("Sorry, quantity must be greater than 0");
            }

            if (volumeDiscount == null){volumeDiscount = BigDecimal.ZERO;}
            if (specialDiscount == null){specialDiscount = BigDecimal.ZERO;}
        }
    }
}
