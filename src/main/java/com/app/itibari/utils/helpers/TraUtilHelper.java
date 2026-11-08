package com.app.itibari.utils.helpers;

import com.app.itibari.configuration.TraConfiguration;
import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.Events;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.request.InvoiceDetailDto;
import com.app.itibari.models.request.TRARequest;
import com.app.itibari.models.response.CategorySummaryDto;
import com.app.itibari.models.response.TRAResponse;
import com.app.itibari.repository.EventsRepository;
import com.app.itibari.repository.InvoiceItemRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.reportHelper.TraInvoiceReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraUtilHelper {
    private final TraConfiguration traConfiguration;
    private final TraInvoiceReceipt traInvoiceReceipt;
    private final SalesOrdersRepository salesOrdersRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private final EventsRepository eventsRepository;

    public TRARequest convertToTraRequest(SalesOrders salesOrder) {

        log.info("Converting sales order {} to TRA request process", salesOrder.getSalesOrderNumber());

        List<InvoiceDetailDto> invoiceDetailDtos = new ArrayList<>();

        TRARequest traRequest = new TRARequest();
        traRequest.setIDate(DateFormatter.formatDateToString(LocalDate.now()));
        traRequest.setITime(DateFormatter.formatTime(LocalDateTime.now()));
        traRequest.setCustInvoiceNo(this.formatInvoiceNumber(salesOrder.getSalesOrderNumber()) + traConfiguration.getInvoicePrefix());
        traRequest.setCustIdType(salesOrder.getCustomer().getTin()!=null ? traConfiguration.getCustomerIdType() : "6");
        traRequest.setCustId(salesOrder.getCustomer().getTin() != null ? salesOrder.getCustomer().getTin() : "");
        traRequest.setCustName(getCustomerNameWithoutSpecialCharacters(salesOrder.getCustomer().getName()));
        traRequest.setMobileNum(salesOrder.getCustomer().getPhoneNumber());
        traRequest.setUserName(salesOrder.getCreatedBy() != null ? salesOrder.getCreatedBy().getFullName() : "Itibari");
        traRequest.setBranch(traConfiguration.getBranch());
        traRequest.setDepartment(traConfiguration.getDepartment());
        traRequest.setDeviceNumber(traConfiguration.getDeviceNumber());
        traRequest.setPayType(traConfiguration.getPayType());
        traRequest.setFCode(traConfiguration.getFCode());
        traRequest.setFCodeToken(traConfiguration.getFCodeToken());
        traRequest.setOriginalCustomerName(salesOrder.getCustomer().getName());

//        createCombinedItems(salesOrder);

        salesOrder.getSalesOrderItems().forEach(item -> {
            if (item.getQuantityDelivered() > 0 && item.getProduct().getTaxCode() != null) {
                InvoiceDetailDto detailDto = new InvoiceDetailDto();
                detailDto.setDescription(this.getDescription(item.getProduct().getProductName()));
                detailDto.setProductName(item.getProduct().getProductName());
                detailDto.setQty(String.valueOf(item.getQuantityDelivered()));
                detailDto.setTaxCode(String.valueOf(item.getProduct().getTaxCode().getCode()));
                detailDto.setAmt(item.getTotalAmount());
                detailDto.setTaxValue(getTaxValue(item.getProduct().getTaxCode().getCodeValue()));
//                detailDto.setPricePerUnit(String.valueOf(item.getPricePerUnit()));
                detailDto.setPricePerUnit(getPrice(item.getProductBeforePriceAfterTax()));
                detailDto.setProductPublicId(String.valueOf(item.getProduct().getPublicId()));
                detailDto.setDiscountAmount(String.valueOf(item.getDiscount()));
                detailDto.setDiscountPercentage(String.valueOf(item.getNewDiscountPercentage()));
                detailDto.setSalesOrderItemId(String.valueOf(item.getPublicId()));
                invoiceDetailDtos.add(detailDto);
            }
        });

        traRequest.setOldInvoiceDetails(invoiceDetailDtos);
        List<CategorySummaryDto> newItems = createCombinedItems(salesOrder);

        traRequest.setInvoiceDetails(newItems);

        return traRequest;
    }

    private String getCustomerNameWithoutSpecialCharacters(String name) {
        // remove special characters
        if (name == null) {
            return "Customer";
        }
        return name.replaceAll("[^a-zA-Z0-9 ]", " ");
    }

    private List<CategorySummaryDto> createCombinedItems(SalesOrders salesOrder) {
        Map<Category, List<SalesOrderItems>> categoriseItems = new HashMap<>();
        for (SalesOrderItems salesOrderItem : salesOrder.getSalesOrderItems()) {
            Category category = salesOrderItem.getProduct().getCategory();
            categoriseItems
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(salesOrderItem);
        }

        List<CategorySummaryDto> categorySummaries = new ArrayList<>();
        categoriseItems.forEach((category, items) -> {
            CategorySummaryDto dto = new CategorySummaryDto();

            int totalQty = items.stream()
                    .mapToInt(SalesOrderItems::getQuantity)
                    .sum();

            BigDecimal totalAmt = items.stream()
                    .map(item -> Optional.ofNullable(item.getTotalAmount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDiscountAmt = items.stream()
                    .map(item -> Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal amountLessDiscount = totalAmt.subtract(totalDiscountAmt);

            String taxCode = String.valueOf(items.get(0).getProduct().getTaxCode().getCode());

            dto.setDescription(this.getDescription(category.getName()));
            dto.setQty(String.valueOf(totalQty));
//            dto.setAmt(totalAmt);
            dto.setAmt(amountLessDiscount);
            dto.setTaxCode(taxCode);

            categorySummaries.add(dto);
        });

        return categorySummaries;
    }

    private String getPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format("%.2f", price.setScale(2, RoundingMode.HALF_UP));
    }

    private String getTaxValue(BigDecimal codeValue) {
        if (codeValue == null) {
            return "0";
        }

        // 0.16 -> 16
        BigDecimal taxPercentage = codeValue.multiply(BigDecimal.valueOf(100));
        return taxPercentage.stripTrailingZeros().toPlainString();
    }

    private String getDescription(String productName) {

        //"KissKids Baby Pants M-32 x 6", but we need only  the name without special characters
        if (productName == null) {
            return "Item";
        }
        return productName.replaceAll("[^a-zA-Z0-9 ]", " ");
    }

    private String formatInvoiceNumber(String salesOrderNumber) {
        //order number format: ORD10012345,
        //invoice number format: INV10012345
        if (salesOrderNumber == null || !salesOrderNumber.startsWith("ORD")) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid sales order number format: " + salesOrderNumber)
                    .userMessage("Invalid sales order number format");
        }

        return "INV-" + salesOrderNumber.substring(3);
    }

    public void generateTraCustomerReceiptAndSend(TRARequest request, TRAResponse response, SalesOrders salesOrder) {

        log.info("Generating TRA customer receipt for sales order Number: {}", salesOrder.getSalesOrderNumber());

        if (traInvoiceReceipt.saveReceipt(request, response, salesOrder)) {
            salesOrder.setTraReceiptGenerated(true);

            String description = String.format("TRA customer receipt generated for sales order %s with invoice number %s",
                    salesOrder.getSalesOrderNumber(),
                    request.getCustInvoiceNo());

            eventsRepository.save(new Events(salesOrder.getPublicId().toString(),
                    "Order Updated",
                    LocalDateTime.now(),
                    description,
                    "TRA Receipt Generation",
                    null));

            salesOrdersRepository.save(salesOrder);

            log.info("TRA customer receipt generated and saved successfully for sales order: {}",
                    salesOrder.getSalesOrderNumber());
        }


    }
}
