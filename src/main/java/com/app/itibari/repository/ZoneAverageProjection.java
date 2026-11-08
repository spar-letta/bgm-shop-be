package com.app.itibari.repository;

import java.time.LocalDate;

public interface ZoneAverageProjection {
    LocalDate getDeliveryDate();

    Double getAverageTotalAmount();

    String getZoneName();

}
