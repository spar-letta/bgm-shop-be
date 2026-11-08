package com.app.itibari.models.request;

import com.app.itibari.dataTypes.LoadingType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LoadingDTO {

    private UUID productItemPublicId;

    private LoadingType loadingType;

    public LoadingDTO(UUID productItemPublicId, LoadingType loadingType) {

        this.productItemPublicId = productItemPublicId;
        this.loadingType = loadingType;
    }
}
