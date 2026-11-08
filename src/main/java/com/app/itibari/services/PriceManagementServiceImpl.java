package com.app.itibari.services;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.models.entities.AvcoItems;
import com.app.itibari.models.entities.Events;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.request.PriceManagementRequest;
import com.app.itibari.models.request.PriceManagementRequestItem;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.PriceManagementTrackerHelper;
import com.app.itibari.utils.specification.AvcoSpecification;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceManagementServiceImpl implements PriceManagementService {

    private final PriceManagementRepository priceManagementRepository;

    private final PersonRepository personRepository;

    private final CounterService counterService;

    private final ProductRepository productRepository;

    private final EventsRepository eventsRepository;

    private final AvcoRepository avcoRepository;
    private final EmailService emailService;

    private Workbook workbook;

    private Sheet sheet;

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Set<String> NON_TAXABLE_CODES = Set.of("Zero Rate", "Exempt");
    private static final int SCALE = 2;

    private final PriceManagementTrackerHelper priceManagementTrackerHelper;

    @Transactional
    @Override
    public void createPriceManagement(PriceManagementRequest priceManagementRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry, we could not find the person with the provided email.");
        }

        if (priceManagementRequest.getItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Price management items are required")
                    .userMessage("Price management items are required");
        }

        List<Product> products = new ArrayList<>();

        priceManagementRequest.getItems().forEach(item -> {

            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(item.getProductId());

            if (optionalProduct.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product not found")
                        .userMessage("Sorry, we could not find the product with the provided ID.");
            }
            Person person = optionalPerson.get();

            Product product = optionalProduct.get();


            product.setUpdateDate(LocalDateTime.now());
            product.setUpdatedBy(person);
            product.setPreviousSellPrice(product.getSellPrice());

            priceManagementTrackerHelper.createPriceManagementTracker(product, item.getPrice(), person);

            product.setSellPrice(item.getPrice());
            Product savedProduct = productRepository.save(product);
            calculateAmountWithTax(savedProduct);
            products.add(savedProduct);

            String description = String.format("Product price changed from %s to %s by %s",
                    savedProduct.getPreviousSellPrice(),
                    savedProduct.getSellPrice(),
                    person.getFullName());

            eventsRepository.save(new Events(product.getPublicId().toString(),
                    "PRODUCT PRICE UPDATE",
                    LocalDateTime.now(),
                    description,
                    "UPDATE",
                    person));
        });


        // text block for product changed message

        StringBuilder message = new StringBuilder();
        try {
            for (Product product : products) {
                message.append(String.format("Product: %s, Previous Price: %s, New Price: %s\n",
                        product.getProductName(),
                        MONEY_FORMAT.format(product.getPreviousSellPrice()),
                        MONEY_FORMAT.format(product.getSellPrice())));
            }
            // add updated by
            message.append("\n");
            message.append(String.format("Changed by: %s at %s  Phone ( %s )", optionalPerson.get().getFullName(), formatter.format(LocalDateTime.now()), optionalPerson.get().getPhoneNumber()));
            message.append("\n");


        } catch (Exception e) {
            log.info("Error creating price management message: {}", e.getMessage());
        }

        //TODO to be fixed - throws 500
//        emailService.sendPriceChangeNotificationEmail(message.toString());

    }

    @Override
    public void getPriceManagementTemplate(HttpServletResponse response, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry, we could not find the person with the provided email.");
        }
        Person person = optionalPerson.get();

        String prefix = "PriceManagement_Template";

        try {
            workbook = createWorkbook();
            setResponseHeader(response, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx", prefix);
            writeHeaderLinePriceManagementTemplate("Price Management Template");
            writeDataLinePriceManagementTemplate(person);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void uploadPriceManagement(MultipartFile file, String name) throws IOException {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry, we could not find the person with the provided email.");
        }

        Person person = optionalPerson.get();

        InputStream inputStream = file.getInputStream();

        int COL_PRODUCT_NAME = 0;
        int COL_SKU_NUMBER = 1;
        int COL_SELLING_PRICE = 2;

        Workbook workbook = new XSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);
        PriceManagementRequest priceManagementRequest = new PriceManagementRequest();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row r = sheet.getRow(rowIndex);
            DataFormatter formatter = new DataFormatter();
            List<String> errors = new ArrayList<>();

            //row empty continue
            if (r == null ||
                    isRowEmpty(r)) {
                continue;
                //skip empty rows
            }

            PriceManagementRequestItem priceManagementRequestItem = new PriceManagementRequestItem();

            if (r.getCell(COL_PRODUCT_NAME) != null) {
                String productName = formatter.formatCellValue(r.getCell(COL_PRODUCT_NAME));
                priceManagementRequestItem.setProductName(productName);
            } else {
                errors.add("Product name is required");
            }

            if (r.getCell(COL_SKU_NUMBER) != null) {
                String skuNumber = formatter.formatCellValue(r.getCell(COL_SKU_NUMBER));
                priceManagementRequestItem.setSku(skuNumber);
            } else {
                errors.add("SKU number is required");
            }

            if (r.getCell(COL_SELLING_PRICE) != null) {
                String sellingPrice = formatter.formatCellValue(r.getCell(COL_SELLING_PRICE));
                try {
                    priceManagementRequestItem.setPrice(new BigDecimal(sellingPrice));
                } catch (NumberFormatException e) {
                    errors.add("Invalid price format");
                }
            } else {
                errors.add("Selling price is required");
            }

            // validate product

            Optional<Product> optionalProduct = productRepository.findProductBySkuIgnoreCaseAndDeletedIsFalse(priceManagementRequestItem.getSku());

            if (optionalProduct.isEmpty()) {
                errors.add("Product not found");
            } else {
                priceManagementRequestItem.setSku(optionalProduct.get().getSku());
                priceManagementRequestItem.setProductId(optionalProduct.get().getPublicId());
            }

            if (errors.isEmpty()) {
                // update product
                try {
                    priceManagementRequest.getItems().add(priceManagementRequestItem);
                } catch (Exception e) {
                    log.info("Error updating product price: {}", e.getMessage());
                }
            }
        }

        try {
            this.createPriceManagement(priceManagementRequest, name);
        } catch (Exception e) {
            log.info("Error creating price management: {}", e.getMessage());
        }
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    public static XSSFWorkbook createWorkbook() {

        return new XSSFWorkbook();
    }

    public void setResponseHeader(HttpServletResponse response, String contentType, String extension, String prefix) {

        String timeStamp = String.valueOf(System.currentTimeMillis()), fileName = prefix + timeStamp + extension;
        response.setContentType(contentType);
        String headerKey = "Content-Disposition", headerValue = "attachment; filename=" + fileName;
        response.setHeader(headerKey, headerValue);
    }

    public void writeHeaderLinePriceManagementTemplate(String sheetName) {

        sheet = workbook.createSheet(sheetName);
        Row row = sheet.createRow(0);

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Inter");
        font.setBold(true);
        font.setColor(IndexedColors.SEA_GREEN.getIndex());
        cellStyle.setFont(font);

        int COL_PRODUCT_NAME = 0;
        int COL_SKU_NUMBER = 1;
        int COL_SELLING_PRICE = 2;

        createCell(row, COL_PRODUCT_NAME, "Product Name*", cellStyle);
        createCell(row, COL_SKU_NUMBER, "Sku No*", cellStyle);
        createCell(row, COL_SELLING_PRICE, "Price*", cellStyle);
    }

    public void writeDataLinePriceManagementTemplate(Person person) {

        String userName = person.getFirstName();

        Row row = sheet.createRow(1);

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        cellStyle.setFont(font);

        int COL_PRODUCT_NAME = 0;
        int COL_SKU_NUMBER = 1;
        int COL_SELLING_PRICE = 2;

        createCell(row, COL_PRODUCT_NAME, "Sugar", cellStyle);
        createCell(row, COL_SKU_NUMBER, "SKU378292922", cellStyle);
        createCell(row, COL_SELLING_PRICE, "1300.00", cellStyle);
    }

    public void createCell(Row row, int columnIndex, Object value, CellStyle style) {

        Cell cell = row.createCell(columnIndex);
        cell.setCellValue((String) value);
        cell.setCellStyle(style);
    }

    @Override
    public Page<AvcoItems> getAllAvco(String searchParam,
                                      LocalDate fromDate,
                                      LocalDate toDate,
                                      String orderNumber,
                                      UUID productPublicId,
                                      TransactionType transactionType,
                                      Pageable pageable) {

        Specification<AvcoItems> specification = Specification
                .where(AvcoSpecification.isNotDeleted())
                .and(AvcoSpecification.hasOrderNumber(orderNumber))
                .and(AvcoSpecification.hasProduct(productPublicId))
                .and(AvcoSpecification.hasFromToDates(fromDate, toDate))
                .and(AvcoSpecification.searchParamMatches(searchParam))
                .and(AvcoSpecification.hasTransactionType(transactionType));

        return avcoRepository.findAll(specification, pageable);
    }

    @Transactional
    public void calculateAmountWithTax(Product savedProduct) {
        BigDecimal sellPrice = savedProduct.getSellPrice();
        if (sellPrice == null) {
            log.warn("Sell price cannot be null for product {}", savedProduct.getPublicId());
            return;
        }

        BigDecimal amountBeforeTax;
        BigDecimal taxAmount;

        if (savedProduct.getTaxCode() != null &&
                NON_TAXABLE_CODES.contains(savedProduct.getTaxCode().getCodeName())) {

            amountBeforeTax = sellPrice;
            taxAmount = BigDecimal.ZERO;

        } else {
            BigDecimal taxRate = savedProduct.getTaxCode() != null
                    ? savedProduct.getTaxCode().getCodeValue()
                    : BigDecimal.ZERO;

            BigDecimal taxValue = taxRate.add(BigDecimal.ONE);
            amountBeforeTax = sellPrice.divide(taxValue, SCALE, RoundingMode.HALF_UP);
            taxAmount = sellPrice.subtract(amountBeforeTax);
        }

        savedProduct.setSellingPriceBeforeTax(amountBeforeTax);
        savedProduct.setTaxAmount(taxAmount);

        log.debug("Product {}: sellPrice={}, taxRate={}, beforeTax={}, taxAmount={}",
                savedProduct.getPublicId(), sellPrice,
                savedProduct.getTaxCode() != null ? savedProduct.getTaxCode().getCodeValue() : BigDecimal.ZERO,
                amountBeforeTax, taxAmount);

        productRepository.save(savedProduct);
    }
}
