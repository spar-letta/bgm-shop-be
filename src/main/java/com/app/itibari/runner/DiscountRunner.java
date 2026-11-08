//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.DiscountType;
//import com.app.itibari.models.entities.Invoice;
//import com.app.itibari.repository.DiscountRepository;
//import com.app.itibari.repository.InvoiceRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//
//@Slf4j
//@Component
//@Order(value = 11)
//public class DiscountRunner implements CommandLineRunner {
//    private final DiscountRepository discountRepository;
//    private final InvoiceRepository invoiceRepository;
//
//    public DiscountRunner(DiscountRepository discountRepository, InvoiceRepository invoiceRepository) {
//        this.discountRepository = discountRepository;
//        this.invoiceRepository = invoiceRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//
//        // get all specialDiscounts.. update orderPublicIds
//
//        log.info("====================starting to update discounts with orderPublicIds====================");
//
//        discountRepository.findAll().forEach(discount -> {
//            if ( discount.getDiscountType()== DiscountType.SpecialDiscount && discount.getOrderPublicId() == null) {
//                log.info("==========Updating discount with publicId: {} and name: {}", discount.getPublicId(), discount.getDiscountType());
//                Optional<Invoice> invoice = invoiceRepository.findByDiscountPublicIdAndDeletedIsFalse(discount.getPublicId().toString());
//
//                if (invoice.isPresent()) {
//                    log.info("================Found invoice with id: {} for discount with publicId: {}", invoice.get().getId(), discount.getPublicId());
//                    var foundInvoice = invoice.get();
//                    discount.setOrderPublicId(foundInvoice.getSalesOrderPublicId());
//                    discountRepository.save(discount);
//
//                    log.info("================Updated discount with publicId: {} with orderPublicId: {}", discount.getPublicId(), discount.getOrderPublicId());
//                }
//            }
//        });
//
//        log.info("====================finished updating discounts with orderPublicIds====================");
//
//    }
//}
