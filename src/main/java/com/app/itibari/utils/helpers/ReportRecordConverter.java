package com.app.itibari.utils.helpers;

import com.app.itibari.models.response.ProductItemDTO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReportRecordConverter {

    private final ExcelUtils excelUtils;

    private final DecimalFormat myFormatter = new DecimalFormat("#,##0.00;(#,##0.00)");

    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public ReportRecordConverter(ExcelUtils excelUtils) {

        this.excelUtils = excelUtils;
    }

    public void addRecordData(List<ProductItemDTO> itemDTOList,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        int colIndex;

        for (ProductItemDTO productItemDTO : itemDTOList) {
            colIndex = -1;
            XSSFRow row = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row, ++colIndex, productItemDTO.getProductName(), dataCellStyle);
            excelUtils.createStringDataCell(row, ++colIndex, productItemDTO.getProductCode(), dataCellStyle);
            excelUtils.createStringDataCell(row,
                    ++colIndex,
                    String.valueOf(productItemDTO.getQuantity()),
                    dataCellStyleRightAlign); // Convert to double
        }
    }
}
