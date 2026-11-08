package com.app.itibari.utils.batch;

import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.models.request.SkuProcess;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.WarehouseRepository;
import com.app.itibari.services.helper.StockUpdateHelper;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class SkuProcessWriter implements ItemWriter<SkuProcess>, InitializingBean {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockUpdateHelper stockUpdateHelper;

    @Override
    public void write(@NonNull Chunk<? extends SkuProcess> chunk) throws Exception {

        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(
                "7371cd7e-da28-4325-b331-e5787e211b14"));

        if (optionalWarehouse.isPresent()) {
            Warehouse warehouse = optionalWarehouse.get();
            Map<Product, Long> productLongMap = new HashMap<>();
            chunk.getItems().forEach(item -> {
                try {
                    Optional<Product> optionalProduct = productRepository.findProductBySkuIgnoreCaseAndDeletedIsFalse(item.getSkuCode());

                    if (optionalProduct.isPresent()) {
                        Product product = optionalProduct.get();
                        productLongMap.put(product, Long.valueOf(item.getQuantity()));
                    } else {
                        log.info("Product not found with the given SKU code: {}", item.getSkuCode());
                    }
                } catch (Exception e) {
                    log.info("Error creating SKU: {}", e.getMessage());
                }
            });

            stockUpdateHelper.updateProductStockOnReceiveGoodsChunk(productLongMap, warehouse);
        } else {
            log.info("Warehouse not found with the given public ID.");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // This method can be used to perform any initialization after the properties are set.
        log.info("SkuProcessWriter initialized successfully.");
    }
}

