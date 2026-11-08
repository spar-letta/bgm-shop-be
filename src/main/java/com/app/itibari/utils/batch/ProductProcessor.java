package com.app.itibari.utils.batch;

import com.app.itibari.models.request.ProductProcess;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ProductProcessor implements ItemProcessor<ProductProcess, ProductProcess> {

    @Override
    public ProductProcess process(ProductProcess input) throws Exception {

        ProductProcess productProcess = new ProductProcess();
        productProcess.setProductName(input.getProductName());
        productProcess.setBuyingPrice(input.getBuyingPrice());
        productProcess.setSellingPrice(input.getSellingPrice());
        productProcess.setTaxMapping(input.getTaxMapping());
        productProcess.setSpecifyCustomTax(input.getSpecifyCustomTax());
        productProcess.setSuppliers(input.getSuppliers());
        productProcess.setCategory(input.getCategory());
        productProcess.setProductClass(input.getProductClass());
        productProcess.setSubClass(input.getSubClass());
        productProcess.setPurchaseUnitSize(input.getPurchaseUnitSize());
        productProcess.setPurchaseUnitName(input.getPurchaseUnitName());
        productProcess.setSellingUnit(input.getSellingUnit());
        productProcess.setUnitOfMeasurement(input.getUnitOfMeasurement());
        productProcess.setBarCode(input.getBarCode());
        productProcess.setWeight(input.getWeight());
        productProcess.setLength(input.getLength());
        productProcess.setWidth(input.getWidth());
        productProcess.setHeight(input.getHeight());
        return productProcess;
    }
}
