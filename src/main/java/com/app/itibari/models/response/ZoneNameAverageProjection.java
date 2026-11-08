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
public class ZoneNameAverageProjection {

    private Double averageTotalAmount;
    private String zoneName;
}

