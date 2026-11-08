package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DisposedQuarantineProducts(UUID publicId,
                                         String disposalDate,

                                         String sku,

                                         String productName,

                                         boolean disposed,

                                         String disposalValue,

                                         String reasonCode,

                                         String comment,

                                         String initiatedBy,

                                         String approvedBy,
                                         String quantity) {
}
