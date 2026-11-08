package com.app.itibari.dataTypes;

import java.util.Arrays;
import java.util.List;

public enum DeliveryStatus {
    Delivered,
    NotDelivered,
    Failed,
    Pending,
    PartiallyDelivered;

    public static List<DeliveryStatus> inDeliveryStatus() {
        return Arrays.asList(Delivered, NotDelivered, Failed, PartiallyDelivered);
    }
}

