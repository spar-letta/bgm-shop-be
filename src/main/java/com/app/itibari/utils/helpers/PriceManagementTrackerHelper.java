package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.PriceManagementTracker;
import com.app.itibari.models.entities.Product;
import com.app.itibari.repository.PriceManagementTrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceManagementTrackerHelper {

    @Autowired
    private PriceManagementTrackerRepository priceManagementTrackerRepository;


    public void createPriceManagementTracker(Product product, BigDecimal price, Person person) {
        PriceManagementTracker priceManagementTracker = new PriceManagementTracker();
        priceManagementTracker.setProductSku(product.getSku());
        priceManagementTracker.setPreviousSellingPrice(product.getSellPrice());
        priceManagementTracker.setCurrentSellingPrice(price);
        priceManagementTracker.setUpdatedBy(person);
        priceManagementTrackerRepository.save(priceManagementTracker);
    }
}
