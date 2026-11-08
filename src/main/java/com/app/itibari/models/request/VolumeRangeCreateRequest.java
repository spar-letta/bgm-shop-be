package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VolumeRangeCreateRequest {

    private Integer minVolume;

    private Integer maxVolume;

    private BigDecimal discountAmount;
}
