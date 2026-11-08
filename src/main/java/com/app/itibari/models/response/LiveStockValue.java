package com.app.itibari.models.response;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LiveStockValue {

    private LocalDateTime date;

    private String productName;

    private long openingStock;

    private long issuedStock;

    private long expectedStockAfterIssuance;

    private long goodsReceivedNote;

    private long goodsReturnedNote;

    private long returnsFromTrade;

    private long replacements;

    private long returnedQuarantineStock;

    private long expectedClosingStock;

    private long quarantineStock;

    private long totalStock;

    private long soldStock;

    private long stockAdjustment;

    private long negativeAdjustedStock;

    private long closingStock;

    private Integer bonusStock;

    private Integer rebateStock;

    private String productCode;

    private Integer incomingStock;

    private Integer customerReturnStock;
}
