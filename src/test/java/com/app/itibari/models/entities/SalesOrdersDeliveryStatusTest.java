package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DeliveryStatus;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.OrderItemStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesOrdersDeliveryStatusTest {

    @Test
    void getDeliveryStatus_returnsFailed_whenLoadingStatusIsFailedDelivery() {
        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.FAILED_DELIVERY);
        salesOrder.setSalesOrderItems(Set.of(notDeliveredItem()));

        assertEquals(DeliveryStatus.Failed, salesOrder.getDeliveryStatus());
    }

    @Test
    void getDeliveryStatus_returnsNotDelivered_whenItemsAreUndeliveredAndOrderNotFailed() {
        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
        salesOrder.setSalesOrderItems(Set.of(notDeliveredItem()));

        assertEquals(DeliveryStatus.NotDelivered, salesOrder.getDeliveryStatus());
    }

    @Test
    void getDeliveryStatus_returnsPartiallyDelivered_whenItemsArePartiallyDeliveredAndOrderNotFailed() {
        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);

        SalesOrderItems item = new SalesOrderItems();
        item.setStatus(OrderItemStatus.PartiallyDelivered);
        salesOrder.setSalesOrderItems(Set.of(item));

        assertEquals(DeliveryStatus.PartiallyDelivered, salesOrder.getDeliveryStatus());
    }

    private SalesOrderItems notDeliveredItem() {
        SalesOrderItems item = new SalesOrderItems();
        item.setStatus(OrderItemStatus.NotDelivered);
        return item;
    }
}
