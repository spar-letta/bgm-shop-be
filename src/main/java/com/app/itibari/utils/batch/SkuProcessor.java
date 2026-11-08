package com.app.itibari.utils.batch;

import com.app.itibari.models.request.SkuProcess;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SkuProcessor implements ItemProcessor<SkuProcess, SkuProcess> {

    @Override
    public SkuProcess process(SkuProcess item) throws Exception {

        // process
        SkuProcess skuProcess = new SkuProcess();
        skuProcess.setProductName(item.getProductName());
        skuProcess.setQuantity(item.getQuantity());
        skuProcess.setSkuCode(item.getSkuCode());
        skuProcess.setPrice(item.getPrice()); //0
        return skuProcess;
    }
}
