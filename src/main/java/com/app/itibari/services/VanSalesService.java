package com.app.itibari.services;

import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.VanSales;
import com.app.itibari.models.request.UpdateVanSales;
import com.app.itibari.models.request.VanSaleRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VanSalesService {

    VanSales createVanSale(@Valid VanSaleRequest vanSaleRequest, String name);

    void updateVanSale(UUID publicId, ActionType actionType, @Valid UpdateVanSales vanSaleRequest, String name);

    VanSales getVanSale(UUID publicId, String name);

    Page<VanSales> getVanSales(String searchParam,
                               UUID deliveryAgentPublicId,
                               VanSaleStatus status,
                               String from,
                               String to,
                               Pageable pageable);

    void deleteVanSale(UUID publicId, String name);

    Page<ProductItem> getVanSalesProductItems(UUID publicId, Pageable pageable);

    Page<ProductItem> getRoutePlanningVanSalesProducts(UUID publicId, Pageable pageable);
}
