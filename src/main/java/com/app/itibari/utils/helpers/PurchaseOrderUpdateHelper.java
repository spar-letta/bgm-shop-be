package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.models.entities.PurchaseOrder;
import com.app.itibari.models.request.PurchaseOrderRequest;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.stereotype.Component;
@Component
public class PurchaseOrderUpdateHelper {

    public void validateUpdate(PurchaseOrder purchaseOrder) {
        validateExists(purchaseOrder);
        validateStatus(purchaseOrder);
        validateReplacement(purchaseOrder);
    }

    private void validateExists(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }
    }

    private void validateStatus(PurchaseOrder purchaseOrder) {
        if (!PurchaseOrderStatus.inThePurchaseOrderStatus().contains(purchaseOrder.getStatus())) {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("Action not allowed on purchase order in status %s",
                            purchaseOrder.getStatus().name()))
                    .userMessage(String.format("Sorry, Action not allowed on purchase order in status %s",
                            purchaseOrder.getStatus().name()));
        }
    }

    private void validateReplacement(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.isReplacementPurchaseOrder()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Replacement purchase order cannot be updated")
                    .userMessage("Replacement purchase order cannot be updated");
        }
    }
}
