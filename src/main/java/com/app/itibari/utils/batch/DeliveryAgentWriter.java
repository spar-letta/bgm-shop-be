package com.app.itibari.utils.batch;

import com.app.itibari.models.request.DeliveryAgentsRequest;
import com.app.itibari.services.DeliveryAgentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeliveryAgentWriter implements ItemWriter<DeliveryAgentsRequest>, InitializingBean {

    @Autowired
    private DeliveryAgentsService deliveryAgentsService;

    @Override
    public void write(Chunk<? extends DeliveryAgentsRequest> chunk) {

        chunk.getItems().forEach(item -> {

            try {
                deliveryAgentsService.createDeliveryAgent(item, "itibari@gmail.com");
            } catch (Exception e) {
                log.info("Error creating supplier: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
