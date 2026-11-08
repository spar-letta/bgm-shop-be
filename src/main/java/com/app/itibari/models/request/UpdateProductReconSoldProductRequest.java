package com.app.itibari.models.request;

import com.app.itibari.dataTypes.IncrementType;
import com.app.itibari.dataTypes.ProductColumn;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateProductReconSoldProductRequest {

    private UUID productItemPublicId;

    private long quantity;

    private IncrementType incrementType;

    private ProductColumn productColumn;
}
