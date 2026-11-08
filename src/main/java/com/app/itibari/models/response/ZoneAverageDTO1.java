package com.app.itibari.models.response;

import java.time.LocalDate;

public class ZoneAverageDTO1 {
    private LocalDate deliveryDate;
    private Integer averageTotalAmount;

    public ZoneAverageDTO1(LocalDate deliveryDate, Integer averageTotalAmount) {
        this.deliveryDate = deliveryDate;
        this.averageTotalAmount = averageTotalAmount;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Integer getAverageTotalAmount() {
        return averageTotalAmount;
    }

    public void setAverageTotalAmount(Integer averageTotalAmount) {
        this.averageTotalAmount = averageTotalAmount;
    }
}
