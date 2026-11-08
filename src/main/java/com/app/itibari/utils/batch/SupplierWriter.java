package com.app.itibari.utils.batch;

import com.app.itibari.models.request.SupplierRequest;
import com.app.itibari.services.ProcurementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupplierWriter implements ItemWriter<SupplierRequest>, InitializingBean {

    @Autowired
    private ProcurementService procurementService;

    @Override
    public void write(Chunk<? extends SupplierRequest> chunk) {

        chunk.getItems().forEach(item -> {

            try {
                procurementService.createSupplier(item, "itibari@gmail.com");
            } catch (Exception e) {
                log.info("Error creating supplier: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
