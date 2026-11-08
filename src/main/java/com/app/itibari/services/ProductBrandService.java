package com.app.itibari.services;

import com.app.itibari.models.entities.ProductBrand;
import com.app.itibari.models.request.ProductBrandRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.UUID;

public interface ProductBrandService {
    ProductBrand createProductBrand(@Valid ProductBrandRequest productBrandRequest, String name);

    Page<ProductBrand> fetchProductBrands(String searchParam, Pageable pageable, Principal principal);

    ProductBrand updateProductBrand(UUID publicId, @Valid ProductBrandRequest productBrandRequest, String name);

    void deleteProductBrand(UUID publicId, String name);
}
