package com.app.itibari.utils.feign;

import com.app.itibari.models.request.InvoiceListRequest;
import com.app.itibari.models.response.TRAResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "TraIntegrationService",
        url = "${tra.integration.base-url}"
)
public interface TraIntegrationService {


    // post

    /**
     * send payload to tra server with receipt body, no header needed
     */

    @PostMapping(
            value = "${tra.integration.receipt-endpoint}",
            consumes = "application/json",
            produces = "application/json"
    )
    TRAResponse sendReceiptToTraServer(@RequestBody InvoiceListRequest request);
}
