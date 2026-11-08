package com.app.itibari.repository;

import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.ProductRecon;
import com.app.itibari.models.entities.VanSales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {

    Optional<ProductItem> findByPublicIdAndDeletedIsFalse(UUID itemPublicId);

    Page<ProductItem> findAllByProductReconAndDeletedIsFalse(ProductRecon productRecon, Pageable pageable);

    Page<ProductItem> findAllByVanSalesAndDeletedIsFalse(VanSales vanSales, Pageable pageable);

    Optional<ProductItem> findByProductReconAndProductPublicIdAndDeletedIsFalse(ProductRecon productRecon, UUID productId);

    Page<ProductItem> findAllByProductReconAndHasVanSaleAndDeletedIsFalse(ProductRecon productRecon, boolean b, Pageable pageable);

    Page<ProductItem> findAllByReturnNotePublicIdAndDeletedIsFalse(String string, Pageable pageable);

    Optional<ProductItem> findByProductReconAndPublicIdAndDeletedIsFalse(ProductRecon productRecon, UUID productItemPublicId);

    List<ProductItem> findByProductReconAndDeletedIsFalse(ProductRecon recon);

    List<ProductItem> findByReturnNotePublicIdAndDeletedIsFalse(String publicId);
}
