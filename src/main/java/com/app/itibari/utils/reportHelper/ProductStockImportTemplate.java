package com.app.itibari.utils.reportHelper;

import com.app.itibari.models.entities.ProductStock;
import com.app.itibari.models.request.ProductStockDto;
import com.app.itibari.repository.ProductStockRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductStockImportTemplate {

    private final ProductStockRepository productStockRepository;

    private Workbook workbook;
    private Sheet sheet;

    public void downloadTemplate(HttpServletResponse response, String prefix, List<ProductStock> productStockList, Principal principal) {
        try {
            workbook = createWorkbook();
            setResponseHeader(response, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx", prefix);
            writeHeaderLineProductStockTemplate("Product_stock_Template");
            writeDataLineProductStockTemplate(productStockList, principal);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue((String) value);
        cell.setCellStyle(style);
    }

    public void writeHeaderLineProductStockTemplate(String sheetName) {
        sheet = workbook.createSheet(sheetName);
        Row row = sheet.createRow(0);

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Inter");
        font.setBold(true);
        cellStyle.setFont(font);

        int COL_ID = 0;
        int COL_WAREHOUSE_ID = 1;
        int COL_PRODUCT_NAME = 2;
        int COL_PRODUCT_SKU = 3;
        int COL_AVAILABLE_STOCK = 4;
        int COL_RESERVED_STOCK = 5;
        int COL_QUARANTINE_STOCK = 6;
        int COL_TOTAL_STOCK = 7;
        int COL_CLOSING_STOCK = 8;


        createCell(row, COL_ID, "code_number*", cellStyle);
        createCell(row, COL_WAREHOUSE_ID, "code_number_two*", cellStyle);
        createCell(row, COL_PRODUCT_NAME, "product_name*", cellStyle);
        createCell(row, COL_PRODUCT_SKU, "product_sku*", cellStyle);
        createCell(row, COL_AVAILABLE_STOCK, "available_stock*", cellStyle);
        createCell(row, COL_RESERVED_STOCK, "reserved_stock*", cellStyle);
        createCell(row, COL_QUARANTINE_STOCK, "quarantine_stock*", cellStyle);
        createCell(row, COL_TOTAL_STOCK, "total_stock*", cellStyle);
        createCell(row, COL_CLOSING_STOCK, "closing_stock*", cellStyle);
    }

    private void writeDataLineProductStockTemplate(List<ProductStock> productStockList, Principal principal) {

        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        cellStyle.setFont(font);

        int COL_ID = 0;
        int COL_WAREHOUSE_ID = 1;
        int COL_PRODUCT_NAME = 2;
        int COL_PRODUCT_SKU = 3;
        int COL_AVAILABLE_STOCK = 4;
        int COL_RESERVED_STOCK = 5;
        int COL_QUARANTINE_STOCK = 6;
        int COL_TOTAL_STOCK = 7;
        int COL_CLOSING_STOCK = 8;

        int rowIndex = 1;

        for (ProductStock productStock : productStockList) {
            Row row = sheet.createRow(rowIndex++);

            createCell(row, COL_ID, productStock.getId().toString(), cellStyle);
            createCell(row, COL_WAREHOUSE_ID, productStock.getWarehouse().getId().toString(), cellStyle);
            createCell(row, COL_PRODUCT_NAME, productStock.getProductName(), cellStyle);
            createCell(row, COL_PRODUCT_SKU, productStock.getProductSku(), cellStyle);
            createCell(row, COL_AVAILABLE_STOCK, productStock.getAvailableStock().toString(), cellStyle);
            createCell(row, COL_RESERVED_STOCK, productStock.getReservedStock().toString(), cellStyle);
            createCell(row, COL_QUARANTINE_STOCK, productStock.getQuarantinedStock().toString(), cellStyle);
            createCell(row, COL_TOTAL_STOCK, productStock.getTotalStock().toString(), cellStyle);
            createCell(row, COL_CLOSING_STOCK, productStock.getClosingStock().toString(), cellStyle);
        }
    }


    public void uploadTemplate(InputStream inputStream, Principal loggedInUserDetails) throws IOException {
        int COL_ID = 0;
        int COL_WAREHOUSE_ID = 1;
        int COL_PRODUCT_NAME = 2;
        int COL_PRODUCT_SKU = 3;
        int COL_AVAILABLE_STOCK = 4;
        int COL_RESERVED_STOCK = 5;
        int COL_QUARANTINE_STOCK = 6;
        int COL_TOTAL_STOCK = 7;
        int COL_CLOSING_STOCK = 8;

        Workbook workbook = new XSSFWorkbook(inputStream);

        Set<ProductStockDto> productStockDtoSet = new HashSet<>();
        Sheet sheet = workbook.getSheetAt(0);
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row r = sheet.getRow(rowIndex);
            DataFormatter formatter = new DataFormatter();
            List<String> errors = new ArrayList<>();

            //row empty continue
            if (r == null ||
                    isRowEmpty(r)) {
                continue;
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_ID)))) {
                errors.add("Missing code_number");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_WAREHOUSE_ID)))) {
                errors.add("Missing code number two");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_PRODUCT_NAME)))) {
                errors.add("Missing product name");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_PRODUCT_SKU)))) {
                errors.add("Missing product sku");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_AVAILABLE_STOCK)))) {
                errors.add("Missing available stock");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_RESERVED_STOCK)))) {
                errors.add("Missing reserved stock");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_TOTAL_STOCK)))) {
                errors.add("Missing total stock");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_QUARANTINE_STOCK)))) {
                errors.add("Missing quarantine stock");
            }

            if (StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_CLOSING_STOCK)))) {
                errors.add("Missing total stock");
            }

            ProductStockDto.ProductStockDtoBuilder builder = ProductStockDto.builder();

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_ID)))) {
                String columnNumber = formatter.formatCellValue(r.getCell(COL_ID));
                builder.id(Long.valueOf(columnNumber));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_WAREHOUSE_ID)))) {
                String warehouseNumber = formatter.formatCellValue(r.getCell(COL_WAREHOUSE_ID));
                builder.warehouseId(Long.valueOf(warehouseNumber));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_PRODUCT_NAME)))) {
                String productName = formatter.formatCellValue(r.getCell(COL_PRODUCT_NAME));
                builder.productName(productName);
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_PRODUCT_SKU)))) {
                String productSku = formatter.formatCellValue(r.getCell(COL_PRODUCT_SKU));
                builder.productName(productSku);
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_AVAILABLE_STOCK)))) {
                String availableStock = formatter.formatCellValue(r.getCell(COL_AVAILABLE_STOCK));
                builder.availableStock(Long.valueOf(availableStock));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_RESERVED_STOCK)))) {
                String reservedStock = formatter.formatCellValue(r.getCell(COL_RESERVED_STOCK));
                builder.reservedStock(Long.valueOf(reservedStock));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_QUARANTINE_STOCK)))) {
                String quarantineStock = formatter.formatCellValue(r.getCell(COL_QUARANTINE_STOCK));
                builder.quarantineStock(Long.valueOf(quarantineStock));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_TOTAL_STOCK)))) {
                String totalStock = formatter.formatCellValue(r.getCell(COL_TOTAL_STOCK));
                builder.totalStock(Long.valueOf(totalStock));
            }

            if (!StringUtils.isEmpty(formatter.formatCellValue(r.getCell(COL_CLOSING_STOCK)))) {
                String closingStock = formatter.formatCellValue(r.getCell(COL_CLOSING_STOCK));
                builder.closingStock(Long.valueOf(closingStock));
            }

            ProductStockDto productStockDto = builder.build();

            productStockDtoSet.add(productStockDto);

        }
        if (!productStockDtoSet.isEmpty()) {
            createUpdateProductStockDetails(productStockDtoSet);
        }
    }

    private static boolean isRowEmpty(Row row) {
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

    private void createUpdateProductStockDetails(Set<ProductStockDto> productStockDtoSet) {
        // available_stock = availableStock - current_available_stock_reading
        // reserved_stock = current_reading + reserved_stock
        // total_stock = closing_stock
        // closing_stock = closing_stock

        for (ProductStockDto productStockDto : productStockDtoSet) {
            ProductStock productStock = productStockRepository.findById(productStockDto.id()).orElse(null);
            if (productStock != null) {
                productStock.setAvailableStock(productStockDto.availableStock());
                productStock.setTotalStock(productStockDto.availableStock());
                productStock.setQuarantinedStock(productStockDto.quarantineStock());
                productStock.setReservedStock(productStockDto.reservedStock());
                productStock.setClosingStock(productStockDto.closingStock());
                productStockRepository.save(productStock);
            }
        }
    }
}
