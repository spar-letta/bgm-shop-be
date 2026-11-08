package com.app.itibari.utils.helpers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DayDateCheckerHelper {

    //Orders placed Monday–Friday: Delivery should be scheduled for the next day.
    //
    //Orders placed Saturday: Delivery should be scheduled for Monday.
    //
    //Sundays are non-working days, and no deliveries should be scheduled or assigned.

    public LocalDate deliveryDateCalculator(LocalDate orderDate) {

        switch (orderDate.getDayOfWeek()) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY -> {
                return orderDate.plusDays(1);
            }
            case SATURDAY -> {
                return orderDate.plusDays(2);
            }
            default -> throw new IllegalStateException("Unexpected value: " + orderDate.getDayOfWeek());
        }
    }
}
