//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.DiscountStatus;
//import com.app.itibari.dataTypes.DiscountType;
//import com.app.itibari.models.entities.Product;
//import com.app.itibari.repository.DiscountApprovalRepository;
//import com.app.itibari.repository.DiscountRepository;
//import com.app.itibari.repository.ProductRepository;
//import com.app.itibari.repository.VolumeRangeRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//
//@Slf4j
//@Component
//@Order(10)
//public class LocationRunner implements CommandLineRunner {
//    private final DiscountRepository discountRepository;
//    private final DiscountApprovalRepository discountApprovalRepository;
//
//    private final VolumeRangeRepository volumeRangeRepository;
//    private final ProductRepository productRepository;
//
//    public LocationRunner(DiscountRepository discountRepository, DiscountApprovalRepository discountApprovalRepository,
//                          VolumeRangeRepository volumeRangeRepository, ProductRepository productRepository) {
//        this.discountRepository = discountRepository;
//        this.discountApprovalRepository = discountApprovalRepository;
//        this.volumeRangeRepository = volumeRangeRepository;
//        this.productRepository = productRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//
//        discountRepository.findAllByDiscountTypeAndDeletedIsFalse(DiscountType.VolumeDiscount, Pageable.unpaged())
//                .forEach(discount -> {
//                    if(discount.getVolumeRanges()!=null && !discount.getVolumeRanges().isEmpty()){
//
//                        log.info("Processing Volume Ranges for Discount Public ID: {}, for size {}", discount.getPublicId(), discount.getVolumeRanges().size());
//                        discount.getVolumeRanges().forEach(range -> {
//                            range.setDiscountPublicId(discount.getPublicId().toString());
//                            volumeRangeRepository.save(range);
//                        });
//
//                    }
//
//                    log.info("Processing Discount ended for Discount Public ID: {}", discount.getPublicId());
//                });
//
//
//        discountRepository.findAllByDiscountTypeAndDeletedIsFalse(DiscountType.VolumeDiscount, Pageable.unpaged())
//                .forEach(discount -> {
//                    if (discount.getStatus() == DiscountStatus.Cancelled || discount.getStatus() == DiscountStatus.InActive) {
//
//                        log.info("discount  found for deletion Public ID: {}", discount.getPublicId());
//                        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(discount.getProductPublicId());
//                        if (optionalProduct.isPresent()) {
//                            Product product = optionalProduct.get();
//                            product.setDiscountMoq(null);
//                            productRepository.save(product);
//                        }
//
//                    }
//
//                });
//    }
//}
