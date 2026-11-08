package com.app.itibari.transformer;

import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.Discounts;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.DiscountRepository;
import com.app.itibari.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductTransformer implements Converter<Product, ProductTransformerDTO> {

    private final FileService fileService;

    private final DiscountRepository discountRepository;

    @Override
    public ProductTransformerDTO convert(Product product) {

        ProductTransformerDTO dto = new ProductTransformerDTO();
        dto.setPublicId(product.getPublicId());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setCategory(new CategoryDTO(product.getCategory().getName(), product.getCategory().getDescription()));
        dto.setSku(product.getSku());
        dto.setSellPrice(new BigDecimal(String.valueOf(product.getSellPrice())));
        dto.setUnitOfMeasure(product.getUnitOfMeasure());
        dto.setStatus(product.getStatus().name());
        dto.setAvailableStock(product.getAvailableStock());
        dto.setTotalStock(product.getTotalStock());
        dto.setInActive(product.isInActive());
        dto.setProductBestSelling(product.isProductBestSelling());
        dto.setDiscountMoq(product.getDiscountMoq());
        dto.setBrand(product.getBrand());
        dto.setAvailableStockItems(product.getAvailableStockItems());
        dto.setTotalStockItems(product.getTotalStockItems());
        dto.setProductClassDTO(product.getProductClass()!=null? new ProductClassDTO(product.getProductClass().getPublicId().toString(),product.getProductClass().getName()): null);
        dto.setProductSubClassDTO(product.getProductSubClass()!=null? new ProductSubClassDTO(product.getProductSubClass().getPublicId().toString(),product.getProductSubClass().getName()): null);
        dto.setNewProduct(product.isNewProduct());

        try {
            FileResponseDTO fileResponseDTO = fileService.getImages(product.getPublicId(), ReferenceType.PRODUCT);
            if (fileResponseDTO != null && fileResponseDTO.getUrl() != null) {
                dto.setProductUrl(fileResponseDTO.getUrl());
                dto.setImageUrl(fileResponseDTO.getUrl());
            }
        } catch (Exception e) {
            log.debug("Error while getting product image for {}", product.getPublicId());
        }

        Optional<Discounts> optionalDiscount =
                discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(product.getPublicId(),
                                                                                                          DiscountType.VolumeDiscount,
                                                                                                          DiscountStatus.Active);
        optionalDiscount.ifPresent(discount -> {
            List<DiscountRangeDTO> discountRangeDTOS = new ArrayList<>();

            discount.getVolumeRanges().forEach(volumeRange -> {
                DiscountRangeDTO discountRangeDTO = new DiscountRangeDTO();
                discountRangeDTO.setMinPurchaseQuantity(volumeRange.getMinVolume());
                discountRangeDTO.setMaxPurchaseQuantity(volumeRange.getMaxVolume());
                discountRangeDTO.setDiscountAmount(volumeRange.getDiscountedAmount());

                discountRangeDTOS.add(discountRangeDTO);
            });

            DiscountDetails discountDetails = new DiscountDetails();
            discountDetails.setDiscountRanges(discountRangeDTOS);

            dto.setDiscountDetails(discountDetails);
        });

        return dto;
    }
}
