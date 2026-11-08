package com.app.itibari.models.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneNameDateAverageProjection {
    private LocalDate deliveryDate;

    private Double averageTotalAmount;

    public Double getRoundedAverageTotalAmount() {
        return averageTotalAmount == null ? null : (double) Math.round(averageTotalAmount);
    }
}
