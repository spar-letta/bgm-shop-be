package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.response.FileResponseDTO;
import com.app.itibari.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Resolves thumbnail URLs the same way POS inventory does: look up the latest file for a
 * reference type, expose it as {@code imageUrl} / {@code productImage}.
 * On the {@code local} profile, missing uploads get a stable placeholder URL so list UIs can render.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageResolver {

    private final FileService fileService;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public String resolve(UUID productPublicId) {
        return resolve(productPublicId, ReferenceType.PRODUCT);
    }

    public String resolve(UUID productPublicId, String sku) {
        return resolve(productPublicId, ReferenceType.PRODUCT);
    }

    public String resolveCategory(UUID categoryPublicId) {
        return resolve(categoryPublicId, ReferenceType.CATEGORY);
    }

    public String resolve(UUID referenceId, ReferenceType referenceType) {
        if (referenceId == null || referenceType == null) {
            return null;
        }
        try {
            FileResponseDTO fileResponseDTO = fileService.getImages(referenceId, referenceType);
            if (fileResponseDTO != null && StringUtils.hasText(fileResponseDTO.getUrl())) {
                return fileResponseDTO.getUrl();
            }
        } catch (Exception ex) {
            log.debug("{} image lookup skipped for {}: {}", referenceType, referenceId, ex.toString());
        }
        if (isLocalProfile()) {
            return "https://picsum.photos/seed/" + referenceId + "/128/128";
        }
        return null;
    }

    public void enrich(Product product) {
        if (product == null) {
            return;
        }
        String url = resolve(product.getPublicId(), ReferenceType.PRODUCT);
        product.setImageUrl(url);
        product.setProductImage(url);
    }

    public void enrich(Collection<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (Product product : products) {
            enrich(product);
        }
    }

    public void enrichCategory(Category category) {
        if (category == null) {
            return;
        }
        String url = resolveCategory(category.getPublicId());
        category.setImageUrl(url);
        category.setProductImage(url);
    }

    public void enrichCategories(Collection<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        for (Category category : categories) {
            enrichCategory(category);
        }
    }

    public <T> void enrichRows(Collection<T> rows,
                               java.util.function.Function<T, UUID> productIdGetter,
                               java.util.function.Function<T, String> skuGetter,
                               BiConsumer<T, String> imageSetter) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (T row : rows) {
            UUID productId = productIdGetter.apply(row);
            String url = resolve(productId, ReferenceType.PRODUCT);
            if (url != null) {
                imageSetter.accept(row, url);
            }
        }
    }

    private boolean isLocalProfile() {
        return activeProfiles != null && activeProfiles.toLowerCase().contains("local");
    }
}
