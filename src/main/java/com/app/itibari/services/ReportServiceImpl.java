package com.app.itibari.services;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.PurchaseOrderStatus;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.report.*;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.transformer.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.*;
import com.app.itibari.utils.reportHelper.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm");

    private final LoanManagementReportExport loanManagementReportExport;
    private final BnplCustomerListReportExport bnplCustomerListReportExport;
    private final BnplEligibleCustomerListReportExport bnplEligibleCustomerListReportExport;
    private final CreditReconHelper creditReconHelper;
    private final CreditReconReportExport creditReconReportExport;
    private final CustomerCreditItemsReportExport customerCreditItemsReportExport;
    private final CustomerCreditPaymentReportExport customerCreditPaymentReportExport;
    private final BnplSalesOrdersReportExport bnplSalesOrdersReportExport;
    private final WalletReportExport walletReportExport;
    private final LocationApprovalReportExport locationApprovalReportExport;
    private final RoundedBackgroundCellEvent backgroundEvent;
    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;
    private final GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository;
    private final GrnPdf grnPdf;
    private final GrtnPdf grtnPdf;
    private final LeadReportExport leadReportExport;
    private final CashCreditReconReportExport cashCreditReconReportExport;
    private final CreditLimitAdjustmentsReportExport creditLimitAdjustmentsReportExport;

    @PersistenceContext
    private EntityManager entityManager;

    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final DecimalFormat moneyFormatter = new DecimalFormat("#,##0.00;(#,##0.00)");

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final GoodsReturnRepository goodsReturnRepository;

    private static final BaseColor PRIMARY_COLOR = new BaseColor(36, 176, 53);

    private static final BaseColor LIGHT_GREEN = new BaseColor(240, 247, 240);

    private static final String company = "Itibari Technologies Limited";

    private static final String companyAddress = "Plot 544/1, Mikocheni light industrial area, Dar es Salaam";

    private static final String companyTin = "TIN: 176-727-497";

    private static final String companyVrn = "VRN: 40-321297-M";

    private static final String companyPhone = "Phone: +255 123 456 789";

    //    private static final String companyEmail = "Email: info@itibari.io";
    private static final String companyEmail = "Email: commercial@itibari.io";

    private final RoutePlanningRepository routePlanningRepository;

    private final LogisticService logisticService;

    private final ExcelUtils excelUtils;

    private final ReportRecordConverter reportRecordConverter;

    private final SalesOrderReportHelper salesOrderReportHelper;

    private final CustomerReportHelper customerReportHelper;

    private final ProductHelperReport productHelperReport;

    private final SalesAgentReport salesAgentReport;

    private final SupplierDashboardAnalysisReport supplierDashboardAnalysisReport;

    private final ProductDistributionReport productDistributionReport;

    private final PurchaseReport purchaseReport;

    private final PurchaseExportReport purchaseExportReport;

    private final PersonRepository personRepository;

    private final SupplierRepository supplierRepository;

    private final ProductService productService;

    private final ProductRepository productRepository;

    private final SalesOrderItemsRepository salesOrderItemsRepository;
    private final StagingReport stagingReport;

    private final StatisticsTransformer statisticsTransformer;
    private final ProductSaleDistributionTransformer productSaleDistributionTransformer;
    private final OrganizationRegionsRepository organizationRegionsRepository;
    private final SalesRevenueTransformer salesRevenueTransformer;
    private final ProductPerformanceTransformer productPerformanceTransformer;
    private final ProductPerformancePerZoneTransformer productPerformancePerZoneTransformer;
    private final ProductCustomerPerformanceTransformer productCustomerPerformanceTransformer;
    private final SalesOrdersRepository salesOrdersRepository;
    private final CustomerPerformanceExportHelper customerPerformanceExportHelper;

    public ReportServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                             GoodsReturnRepository goodsReturnRepository,
                             RoutePlanningRepository routePlanningRepository,
                             LogisticService logisticService,
                             ExcelUtils excelUtils,
                             ReportRecordConverter reportRecordConverter,
                             SalesOrderReportHelper salesOrderReportHelper,
                             CustomerReportHelper customerReportHelper,
                             ProductHelperReport productHelperReport,
                             SalesAgentReport salesAgentReport,
                             SupplierDashboardAnalysisReport supplierDashboardAnalysisReport,
                             ProductDistributionReport productDistributionReport,
                             PurchaseReport purchaseReport,
                             PurchaseExportReport purchaseExportReport,
                             PersonRepository personRepository,
                             SupplierRepository supplierRepository, ProductService productService, ProductRepository productRepository,
                             SalesOrderItemsRepository salesOrderItemsRepository, StagingReport stagingReport,
                             StatisticsTransformer statisticsTransformer, ProductSaleDistributionTransformer productSaleDistributionTransformer, OrganizationRegionsRepository organizationRegionsRepository, SalesRevenueTransformer salesRevenueTransformer, ProductPerformanceTransformer productPerformanceTransformer, ProductPerformancePerZoneTransformer productPerformancePerZoneTransformer, ProductCustomerPerformanceTransformer productCustomerPerformanceTransformer, SalesOrdersRepository salesOrdersRepository, CustomerPerformanceExportHelper customerPerformanceExportHelper, LoanManagementReportExport loanManagementReportExport, BnplCustomerListReportExport bnplCustomerListReportExport, BnplEligibleCustomerListReportExport bnplEligibleCustomerListReportExport, CreditReconHelper creditReconHelper, CreditReconReportExport creditReconReportExport, CustomerCreditItemsReportExport customerCreditItemsReportExport, CustomerCreditPaymentReportExport customerCreditPaymentReportExport, BnplSalesOrdersReportExport bnplSalesOrdersReportExport, WalletReportExport walletReportExport, LocationApprovalReportExport locationApprovalReportExport, RoundedBackgroundCellEvent backgroundEvent, GoodsReceivedNoteRepository goodsReceivedNoteRepository, GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository, GrnPdf grnPdf, GrtnPdf grtnPdf, LeadReportExport leadReportExport, CashCreditReconReportExport cashCreditReconReportExport, CreditLimitAdjustmentsReportExport creditLimitAdjustmentsReportExport) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReturnRepository = goodsReturnRepository;
        this.routePlanningRepository = routePlanningRepository;
        this.logisticService = logisticService;
        this.excelUtils = excelUtils;
        this.reportRecordConverter = reportRecordConverter;
        this.salesOrderReportHelper = salesOrderReportHelper;
        this.customerReportHelper = customerReportHelper;
        this.productHelperReport = productHelperReport;
        this.salesAgentReport = salesAgentReport;
        this.supplierDashboardAnalysisReport = supplierDashboardAnalysisReport;
        this.productDistributionReport = productDistributionReport;
        this.purchaseReport = purchaseReport;
        this.purchaseExportReport = purchaseExportReport;
        this.personRepository = personRepository;
        this.supplierRepository = supplierRepository;
        this.productService = productService;
        this.productRepository = productRepository;
        this.salesOrderItemsRepository = salesOrderItemsRepository;
        this.stagingReport = stagingReport;
        this.statisticsTransformer = statisticsTransformer;
        this.productSaleDistributionTransformer = productSaleDistributionTransformer;
        this.organizationRegionsRepository = organizationRegionsRepository;
        this.salesRevenueTransformer = salesRevenueTransformer;
        this.productPerformanceTransformer = productPerformanceTransformer;
        this.productPerformancePerZoneTransformer = productPerformancePerZoneTransformer;
        this.productCustomerPerformanceTransformer = productCustomerPerformanceTransformer;
        this.salesOrdersRepository = salesOrdersRepository;
        this.customerPerformanceExportHelper = customerPerformanceExportHelper;
        this.loanManagementReportExport = loanManagementReportExport;
        this.bnplCustomerListReportExport = bnplCustomerListReportExport;
        this.bnplEligibleCustomerListReportExport = bnplEligibleCustomerListReportExport;
        this.creditReconHelper = creditReconHelper;
        this.creditReconReportExport = creditReconReportExport;
        this.customerCreditItemsReportExport = customerCreditItemsReportExport;
        this.customerCreditPaymentReportExport = customerCreditPaymentReportExport;
        this.bnplSalesOrdersReportExport = bnplSalesOrdersReportExport;
        this.walletReportExport = walletReportExport;
        this.locationApprovalReportExport = locationApprovalReportExport;
        this.backgroundEvent = backgroundEvent;
        this.goodsReceivedNoteRepository = goodsReceivedNoteRepository;
        this.goodsReturnDeliveryNoteRepository = goodsReturnDeliveryNoteRepository;
        this.grnPdf = grnPdf;
        this.grtnPdf = grtnPdf;
        this.leadReportExport = leadReportExport;
        this.cashCreditReconReportExport = cashCreditReconReportExport;
        this.creditLimitAdjustmentsReportExport = creditLimitAdjustmentsReportExport;
    }

    @Override
    public ByteArrayOutputStream generateReturnNoteReport(UUID publicId, String name, StatementExportType exportType) throws DocumentException, IOException {

        Optional<GoodsReturnDeliveryNote> optionalGoodsReturnNote = goodsReturnDeliveryNoteRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalGoodsReturnNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods return note not found")
                    .userMessage("Goods return note not found");
        }

        GoodsReturnDeliveryNote goodsReturnNote = optionalGoodsReturnNote.get();

        if (exportType == StatementExportType.PDF) {
            return grtnPdf.createGoodsReturnedPdf(goodsReturnNote);
        } else {
            return grtnPdf.createGoodsReturnedExcel(goodsReturnNote, name);
        }
    }

    @Override
    public ByteArrayOutputStream generateReceiveNoteReport(UUID publicId, String name, StatementExportType exportType) throws DocumentException, IOException {

//        Optional<GoodsReturnNote> optionalGoodsReturnNote = goodsReturnRepository.findByPublicIdAndDeletedIsFalse(publicId);
        Optional<GoodsReceivedNote> optionalGoodsReceivedNote = goodsReceivedNoteRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalGoodsReceivedNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Goods receive note not found")
                    .userMessage("Goods receive note not found");
        }

        GoodsReceivedNote goodsReceiveNote = optionalGoodsReceivedNote.get();
        if (exportType == StatementExportType.PDF) {
            return grnPdf.createGrnPdf(goodsReceiveNote);

        } else {
            try {
                String documentTitle = "Goods Receive Note";

                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet(documentTitle);

                org.apache.poi.ss.usermodel.Font headerFont = excelUtils.getHeaderFont(workbook);
                org.apache.poi.ss.usermodel.Font font = excelUtils.getContentFont(workbook);

                CellStyle headerCellStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);
                CellStyle headerCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, headerFont);
                CellStyle stringDataCellStyleLeftAlign = excelUtils.getStringDataCellStyleLeftAlign(workbook, font);
                CellStyle numericDataCellStyleRightAlign = excelUtils.getNumericDataCellStyleRightAlign(workbook, font);
                CellStyle stringDataCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, font);

                int rowIndex = 1;

                // Title
                XSSFRow row1 = sheet.createRow(++rowIndex);
                excelUtils.createStringDataCell(row1, 0, documentTitle, headerCellStyleCentreAlign);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

                // Printed By
                XSSFRow row3 = sheet.createRow(++rowIndex);
                excelUtils.createStringDataCell(row3, 0, "Printed By: " + name, stringDataCellStyleCentreAlign);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

                // Printed On
                XSSFRow row4 = sheet.createRow(++rowIndex);
                excelUtils.createStringDataCell(row4, 0, "Printed On: " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

                XSSFRow row5 = sheet.createRow(++rowIndex);
                excelUtils.createStringDataCell(row4, 0, "Note Number: " + goodsReceiveNote.getNoteNumber(), stringDataCellStyleCentreAlign);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

                // Headers
                List<String> contentHeaders = Arrays.asList(
                        "Product Name",
                        "Product Code",
                        "Qty received",
                        "Qty Requested"
                );

                Row headerRow = sheet.createRow(rowIndex += 2);
                for (int i = 0; i < contentHeaders.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(contentHeaders.get(i));
                    cell.setCellStyle(headerCellStyle);
                }

                ++rowIndex;

                // Add parent + child data
                addRecordData(workbook, goodsReceiveNote.getReceivedProductItem(), sheet, rowIndex, stringDataCellStyleLeftAlign, numericDataCellStyleRightAlign);

                for (int i = 0; i < contentHeaders.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                bos.close();
                return bos;

            } catch (IOException e) {
                throw new BadRequestRestApiException()
                        .developerMessage("Error in create file")
                        .userMessage("Sorry. Error in create file");
            }
        }

    }

    public void addRecordData(XSSFWorkbook workbook,
                              List<ReceivedProductItem> resp,
                              XSSFSheet sheet,
                              int rowIndex,
                              CellStyle dataCellStyle,
                              CellStyle dataCellStyleRightAlign) {

        CellStyle parentRowStyle = workbook.createCellStyle();
        parentRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());

        org.apache.poi.ss.usermodel.Font parentFont = workbook.createFont();
        parentFont.setBold(true);
        parentRowStyle.setFont(parentFont);

        for (ReceivedProductItem receivedProductItem : resp) {

            XSSFRow parentRow = sheet.createRow(++rowIndex);
            int colIndex = -1;

            excelUtils.createStringDataCell(parentRow, ++colIndex, receivedProductItem.getProductName(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, receivedProductItem.getProductCode(), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(receivedProductItem.getQuantityRequested()), parentRowStyle);
            excelUtils.createStringDataCell(parentRow, ++colIndex, String.valueOf(receivedProductItem.getQuantityReceived()), parentRowStyle);

        }
    }

    private void addItem(PdfPTable table,
                         String description,
                         String qty,
                         String unitPrice,
                         String totalPrice,
                         Font font,
                         boolean hasBackground) {

        PdfPCell cell;

        cell = new PdfPCell(new Phrase(description, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        if (hasBackground) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        }
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(qty, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        if (hasBackground) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        }
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(unitPrice, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.NO_BORDER);
        if (hasBackground) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        }
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(totalPrice, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.NO_BORDER);
        if (hasBackground) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        }
        table.addCell(cell);
    }

    private void addItem(PdfPTable table, String description, String qty, String reason) {

        PdfPCell cell;

        cell = new PdfPCell(new Phrase(description));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(qty));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(reason));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addItem(PdfPTable table, String productName, String productCode, String quantityReceived, String quantityRequested) {

        PdfPCell cell;

        cell = new PdfPCell(new Phrase(productName));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(productCode));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(quantityReceived));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(quantityRequested));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    @Override
    public ByteArrayOutputStream generatePurchaseOrderReport(UUID purchaseOrderId, String name) throws DocumentException, IOException {
        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderId);

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Purchase order not found");
        }
        PurchaseOrder po = optionalPurchaseOrder.get();
        if (po.getStatus() == PurchaseOrderStatus.PENDING ||
                po.getStatus() == PurchaseOrderStatus.REJECTED ||
                po.getStatus() == PurchaseOrderStatus.CANCELLED ||
                po.getStatus() == PurchaseOrderStatus.SUBMITTED ||
                po.getStatus() == PurchaseOrderStatus.AMIN_REJECTED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order is not ready for printing")
                    .userMessage("Purchase order is not available for printing");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 10f, 10f, 54f, 54f);

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
//        writer.setPageEvent(new PdfUtil());
        document.open();

        Image logo_2 = Image.getInstance(new ClassPathResource("static/images/bigger_log.png").getInputStream().readAllBytes());
        logo_2.scaleToFit(330, 180);  //630  and 200

        float y = document.getPageSize().getHeight()
                - document.topMargin()
                - logo_2.getScaledHeight()
                + 60f;  // whe you increase like 60+  you move it up

        logo_2.setAbsolutePosition(280, y);  // 270
        writer.getDirectContent().addImage(logo_2);

        addHeaderSection(document);
        addTitle(document);
        addSupplierAndPoDetails(document, po);
        addItemsTable(document, po);
        addSummarySection(document, po, writer);
        document.close();

        return outputStream;
    }

    private void addTopLine(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        BaseColor receiptGreen = new BaseColor(22, 199, 55); // #1db63a

        canvas.setColorStroke(receiptGreen);
        canvas.setLineWidth(4f);

        float left = 0;
        float right = document.getPageSize().getWidth() - 0.5f;
        float top = document.getPageSize().getHeight() - 2;

        canvas.moveTo(left, top);
        canvas.lineTo(right, top);
        canvas.stroke();
    }

    private void addBottomLine(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        BaseColor receiptGreen = new BaseColor(22, 199, 55); // #1db63a

        canvas.setColorStroke(receiptGreen);
        canvas.setLineWidth(4f);

        float left = 0;
        float right = document.getPageSize().getWidth() - 0.2f;
        float top = 0;

        canvas.moveTo(left, top);
        canvas.lineTo(right, top);
        canvas.stroke();
    }

    private void addHeaderSection(Document document) throws IOException, DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(10f);

        PdfPCell logoCell = new PdfPCell();
        Image logo = Image.getInstance(new ClassPathResource("static/images/logoImage_1.jpg").getInputStream().readAllBytes());
        logo.scaleToFit(170, 140);
        logo.setAlignment(Image.ALIGN_LEFT);
        logo.setIndentationLeft(-10f);
        document.add(logo);
        logoCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);

        Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptGreen);

        Paragraph companyName = new Paragraph(company, normalFont);
        companyCell.addElement(companyName);

        Paragraph addressInfo = new Paragraph(companyAddress, normalFont);
        companyCell.addElement(addressInfo);

        Paragraph emailInfo = new Paragraph(companyEmail, normalFont);
        companyCell.addElement(emailInfo);

        Paragraph tinInfo = new Paragraph(companyTin, normalFont);
        companyCell.addElement(tinInfo);

        Paragraph vrnInfo = new Paragraph(companyVrn, normalFont);
        companyCell.addElement(vrnInfo);

        headerTable.addCell(companyCell);

        document.add(headerTable);
    }

    public void addRoutePlaningTitle(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(84, 138, 70));
        Paragraph title = new Paragraph("Issuance Report", titleFont);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(20f);
        titleTable.setSpacingAfter(20f);

        PdfPCell titleCell = new PdfPCell(title);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingTop(6);
        titleCell.setPaddingBottom(8);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        titleCell.setCellEvent(backgroundEvent);

        titleTable.addCell(titleCell);
        document.add(titleTable);
    }

    public void addTitle(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(84, 138, 70));
        Paragraph title = new Paragraph("Purchase Order", titleFont);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(20f);
        titleTable.setSpacingAfter(20f);

        PdfPCell titleCell = new PdfPCell(title);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingTop(6);
        titleCell.setPaddingBottom(8);
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        // Use injected background event
        titleCell.setCellEvent(backgroundEvent);

        titleTable.addCell(titleCell);
        document.add(titleTable);
    }


    private void addSupplierAndPoDetails(Document document, PurchaseOrder po) throws DocumentException {

        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);
        detailsTable.setWidths(new float[]{4f, 1.1f});
        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, receiptGreen);
        Font normalFont_2 = FontFactory.getFont(FontFactory.HELVETICA, 9, receiptGreen);

        // Supplier details
        PdfPCell supplierCell = new PdfPCell();
        supplierCell.setBorder(Rectangle.NO_BORDER);

        Paragraph supplierHeader = new Paragraph("Supplier Details:", headerFont);
        supplierCell.addElement(supplierHeader);

        Chunk label = new Chunk("Supplier Name: ", normalFont);
        Chunk value = new Chunk(po.getSupplier().getSupplierName() != null ? po.getSupplier().getSupplierName().toUpperCase() : "-", normalFont_2);
        Paragraph supplierName = new Paragraph();
        supplierName.add(label);
        supplierName.add(value);
        supplierCell.addElement(supplierName);

        Chunk label_Address = new Chunk("Address: ", normalFont);
        Chunk value_Address = new Chunk(po.getSupplier().getPostalAddress() != null ? po.getSupplier().getPostalAddress() : "-", normalFont_2);
        Paragraph address = new Paragraph();
        address.add(label_Address);
        address.add(value_Address);
        supplierCell.addElement(address);

        Chunk label_Phone = new Chunk("Phone: ", normalFont);
        Chunk value_Phone = new Chunk(po.getSupplier().getPhoneNumber() != null ? po.getSupplier().getPhoneNumber() : "-", normalFont_2);
        Paragraph phone = new Paragraph();
        phone.add(label_Phone);
        phone.add(value_Phone);
        supplierCell.addElement(phone);

        Chunk label_Email = new Chunk("Email: ", normalFont);
        Chunk value_Email = new Chunk(po.getSupplier().getEmail() != null ? po.getSupplier().getEmail() : "-", normalFont_2);
        Paragraph email = new Paragraph();
        email.add(label_Email);
        email.add(value_Email);
        supplierCell.addElement(email);

        detailsTable.addCell(supplierCell);

        // PO details
        PdfPCell poCell = new PdfPCell();
        poCell.setBorder(Rectangle.NO_BORDER);

        poCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph poHeader = new Paragraph("Purchase Order Details", headerFont);
        poHeader.setAlignment(Element.ALIGN_LEFT);
        poCell.addElement(poHeader);

        Chunk label_po_no = new Chunk("PO No: ", normalFont);
        Chunk label_po_no_value = new Chunk(po.getPurchaseOrderNumber() != null ? po.getPurchaseOrderNumber() : "-", normalFont_2);
        Paragraph po_no = new Paragraph();
        po_no.setAlignment(Element.ALIGN_LEFT);
        po_no.add(label_po_no);
        po_no.add(label_po_no_value);
        poCell.addElement(po_no);

        Chunk label_date = new Chunk("Date: ", normalFont);
        Chunk label_date_value = new Chunk(po.getCreationTime() != null ? po.getCreationTime().toLocalDate().format(DATE_FORMATTER) : "-", normalFont_2);
        Paragraph po_no_date = new Paragraph();
        po_no_date.setAlignment(Element.ALIGN_LEFT);
        po_no_date.add(label_date);
        po_no_date.add(label_date_value);
        poCell.addElement(po_no_date);

        Chunk label_time = new Chunk("Time : ", normalFont);
        Chunk label_time_value = new Chunk(po.getCreationTime() != null ? po.getCreationTime().format(TIME_FORMATTER) : "-", normalFont_2);
        Paragraph po_no_time = new Paragraph();
        po_no_time.setAlignment(Element.ALIGN_LEFT);
        po_no_time.add(label_time);
        po_no_time.add(label_time_value);
        poCell.addElement(po_no_time);

        detailsTable.addCell(poCell);

        document.add(detailsTable);
    }

    private void addItemsTable(Document document, PurchaseOrder po) throws DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(7);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{1, 4, 1, 2, 2, 2, 2});
        itemsTable.setSpacingAfter(10);
        itemsTable.setSpacingAfter(1f);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptWhite);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        // Table headers

        PdfPCell listNo = new PdfPCell(new Phrase("No", headerFont));
        listNo.setPadding(5);
        listNo.setBorder(Rectangle.NO_BORDER);
        listNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        listNo.setBackgroundColor(receiptGreen);
        itemsTable.addCell(listNo);

        PdfPCell descHeader = new PdfPCell(new Phrase("Product", headerFont));
        descHeader.setPadding(5);
        descHeader.setBorder(Rectangle.NO_BORDER);
        descHeader.setBackgroundColor(receiptGreen);
        descHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(descHeader);


        PdfPCell qtyHeader = new PdfPCell(new Phrase("Qty", headerFont));
        qtyHeader.setPadding(5);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);


        PdfPCell unitPriceHeader = new PdfPCell(new Phrase("Unit Price", headerFont));
        unitPriceHeader.setPadding(5);
        unitPriceHeader.setBorder(Rectangle.NO_BORDER);
        unitPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        unitPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(unitPriceHeader);

        // total tax expected

        PdfPCell totalExpectedHeader = new PdfPCell(new Phrase("Total(VAT exc)", headerFont));
        totalExpectedHeader.setPadding(5);
        totalExpectedHeader.setBorder(Rectangle.NO_BORDER);
        totalExpectedHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalExpectedHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(totalExpectedHeader);

        PdfPCell taxHeader = new PdfPCell(new Phrase("VAT", headerFont));
        taxHeader.setPadding(5);
        taxHeader.setBorder(Rectangle.NO_BORDER);
        taxHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        taxHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(taxHeader);

        PdfPCell totalPriceHeader = new PdfPCell(new Phrase("Total(VAT inc)", headerFont));
        totalPriceHeader.setPadding(5);
        totalPriceHeader.setBorder(Rectangle.NO_BORDER);
        totalPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(totalPriceHeader);

        int rowIndex = 0;

        for (PurchaseOrderItem item : po.getPurchaseOrderItems()) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(item.getProduct().getProductName(), cellFont));
            descCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(item.getQuantity().toString(), cellFont));
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell unitPriceCell = new PdfPCell(new Phrase(String.format("%,.2f", item.getUnitPrice()), cellFont));
            unitPriceCell.setPadding(5);
            unitPriceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            unitPriceCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(unitPriceCell);

            PdfPCell totalExpectedCell = new PdfPCell(new Phrase(String.format("%,.2f", item.getAmountAfterTax()), cellFont));
            totalExpectedCell.setPadding(5);
            totalExpectedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalExpectedCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(totalExpectedCell);

            PdfPCell taxCell = new PdfPCell(new Phrase(String.format("%,.2f", item.getCalculatedTaxAmount()), cellFont));
            taxCell.setPadding(5);
            taxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            taxCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(taxCell);

            PdfPCell totalPriceCell = new PdfPCell(new Phrase(String.format("%,.2f", item.getTotalAmount()), cellFont));
            totalPriceCell.setPadding(5);
            totalPriceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalPriceCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(totalPriceCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }

    private void addSummarySection(Document document, PurchaseOrder po, PdfWriter writer) throws DocumentException {

        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        BaseColor receiptBlack_sub_tt = new BaseColor(30, 30, 30);

        BaseColor receiptGreen_vat = new BaseColor(115, 114, 112);

        Font sub_tt = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptBlack_sub_tt);

        Font valueFont_grant_amount = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, receiptGreen);
        Font valueFont_grant_color = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        BaseColor receiptGreen_2 = new BaseColor(84, 138, 70);

        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.BLACK);
        line.setLineWidth(0.2f);
        line.setPercentage(40);
        line.setAlignment(Element.ALIGN_RIGHT);

        LineSeparator line2 = new LineSeparator();
        line2.setLineColor(BaseColor.BLACK);
        line2.setLineWidth(0.1f);
        line2.setPercentage(40);
        line2.setAlignment(Element.ALIGN_RIGHT);

        LineSeparator line3 = new LineSeparator();
        line3.setLineColor(BaseColor.BLACK);
        line3.setLineWidth(0.3f);
        line3.setPercentage(40);
        line3.setAlignment(Element.ALIGN_RIGHT);

        BaseColor baseColor = new BaseColor(245, 245, 245);

        BigDecimal grandTotal = po.getTotalAmount();
        BigDecimal subTotal = po.getTotalAmountAfterTax().compareTo(BigDecimal.ZERO) > 0 ? po.getTotalAmountAfterTax() : grandTotal;
        BigDecimal vatAmount = po.getTotalTaxAmount();

        PdfPTable table = new PdfPTable(1);
        table.setWidths(new float[]{4f});
        table.setWidthPercentage(27);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell pdfPCell = new PdfPCell();
        pdfPCell.setPadding(3f);
        pdfPCell.setBackgroundColor(new BaseColor(245, 245, 245));
        pdfPCell.setBorder(Rectangle.NO_BORDER);

        Chunk label_text = new Chunk("Sub Total: ", sub_tt);
        Chunk label_value = new Chunk(String.format("TZS %,.2f", subTotal), valueFont_grant_amount);
        Paragraph subtotal = new Paragraph();
        subtotal.add(label_text);
        subtotal.add(label_value);
        subtotal.setPaddingTop(2f);
        pdfPCell.addElement(subtotal);

        Chunk vat_text = new Chunk("VAT(18%): ", sub_tt);
        Chunk vat_value = new Chunk(String.format("TZS %,.2f", vatAmount), valueFont_grant_amount);
        Paragraph vat_total = new Paragraph();
        vat_total.add(vat_text);
        vat_total.add(vat_value);
        vat_total.setPaddingTop(3f);
        pdfPCell.addElement(vat_total);
        pdfPCell.setPaddingBottom(5f);

        PdfPCell pdfPCellGrand = new PdfPCell();
        pdfPCellGrand.setPaddingTop(6f);

        Chunk tt_value = new Chunk(String.format("TOTAL: %,.2f", grandTotal), valueFont_grant_color);
        Paragraph tt_total = new Paragraph();
        tt_total.add(tt_value);
        tt_total.setPaddingTop(3f);
        tt_total.setAlignment(Element.ALIGN_LEFT);
        pdfPCellGrand.addElement(tt_total);
        pdfPCellGrand.setBackgroundColor(receiptGreen_2);
        pdfPCellGrand.setBorderColor(baseColor);
        pdfPCellGrand.setPaddingBottom(6f);
        pdfPCellGrand.setBorderWidth(3f);

        table.addCell(pdfPCell);
        table.addCell(pdfPCellGrand);

        document.add(table);
    }

    private Image getSmoothImage(byte[] imageBytes) throws IOException, BadElementException {

        InputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(inputStream);

        int width = 120; // Desired width
        int height = 60; // Desired height
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();

        return Image.getInstance(scaledImage, null);
    }

    @Override
    public ByteArrayOutputStream generateIssuanceReport(UUID routePublicId, StatementExportType exportType, String name) throws DocumentException, IOException {

        Optional<RoutePlanning> optionalRoute = routePlanningRepository.findByPublicIdAndDeletedIsFalse(routePublicId);

        if (optionalRoute.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route not found")
                    .userMessage("Route not found");
        }

        RoutePlanning route = optionalRoute.get();

        List<ProductItemDTO> productItemDTOList = (List<ProductItemDTO>) logisticService
                .getRoutePlanningOrdersAndVanSalesProducts(routePublicId, PageRequest.of(0, 100), null)
                .getContent();

        if (productItemDTOList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No products found for this route")
                    .userMessage("No products found for this route");
        }

        // sort ascending by productName

        productItemDTOList = productItemDTOList.stream().sorted(Comparator.comparing(ProductItemDTO::getProductName))
                .collect(Collectors.toList());

        if (exportType == StatementExportType.PDF) {
            return createIssuanceReportPDF(route, productItemDTOList);
        } else if (exportType == StatementExportType.EXCEL) {
            return createIssuanceReportExcel(route, productItemDTOList, name);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    private ByteArrayOutputStream createIssuanceReportPDF(RoutePlanning route, List<ProductItemDTO> productItemDTOList) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 10f, 10f, 54f, 54f);

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        Image logo_2 = Image.getInstance(new ClassPathResource("static/images/bigger_log.png").getInputStream().readAllBytes());
        logo_2.scaleToFit(330, 180);  //630  and 200

        float y = document.getPageSize().getHeight()
                - document.topMargin()
                - logo_2.getScaledHeight()
                + 60f;  // whe you increase like 60+  you move it up

        logo_2.setAbsolutePosition(280, y);  // 270
        writer.getDirectContent().addImage(logo_2);

        addHeaderSection(document);
        addRoutePlaningTitle(document);
        addRoutePlanningDetails(document, route);
        addRouteItemsTable(document, productItemDTOList);
        document.close();

        return outputStream;
    }

    private void addRouteItemsTable(Document document, List<ProductItemDTO> productItemDTOList) throws DocumentException {
        BaseColor receiptGreen = new BaseColor(84, 138, 70);
        BaseColor receiptGreen_2 = BaseColor.LIGHT_GRAY;
        BaseColor receiptWhite = BaseColor.WHITE;
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);

        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{1, 5, 5, 5});
        itemsTable.setSpacingAfter(10);
        itemsTable.setSpacingAfter(1f);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, receiptWhite);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPCell listNo = new PdfPCell(new Phrase("No", headerFont));
        listNo.setPadding(5);
        listNo.setBorder(Rectangle.NO_BORDER);
        listNo.setHorizontalAlignment(Element.ALIGN_CENTER);
        listNo.setBackgroundColor(receiptGreen);
        itemsTable.addCell(listNo);

        PdfPCell descHeader = new PdfPCell(new Phrase("Product Name", headerFont));
        descHeader.setPadding(5);
        descHeader.setBorder(Rectangle.NO_BORDER);
        descHeader.setBackgroundColor(receiptGreen);
        descHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(descHeader);


        PdfPCell qtyHeader = new PdfPCell(new Phrase("Product Code", headerFont));
        qtyHeader.setPadding(5);
        qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyHeader.setBorder(Rectangle.NO_BORDER);
        qtyHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(qtyHeader);


        PdfPCell unitPriceHeader = new PdfPCell(new Phrase("Units", headerFont));
        unitPriceHeader.setPadding(5);
        unitPriceHeader.setBorder(Rectangle.NO_BORDER);
        unitPriceHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        unitPriceHeader.setBackgroundColor(receiptGreen);
        itemsTable.addCell(unitPriceHeader);

        int rowIndex = 0;

        for (ProductItemDTO productItemDTO : productItemDTOList) {

            PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), cellFont));
            numberCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numberCell.setBorderColor(receiptGreen);
            itemsTable.addCell(numberCell);

            PdfPCell descCell = new PdfPCell(new Phrase(productItemDTO.getProductName(), cellFont));
            descCell.setPadding(5);
            numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            descCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(productItemDTO.getProductCode(), cellFont));
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(qtyCell);

            PdfPCell unitPriceCell = new PdfPCell(new Phrase(String.valueOf(productItemDTO.getQuantity()), cellFont));
            unitPriceCell.setPadding(5);
            unitPriceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            unitPriceCell.setBorderColor(receiptGreen_2);
            itemsTable.addCell(unitPriceCell);

            rowIndex++;
        }

        document.add(itemsTable);
    }

    private void addRoutePlanningDetails(Document document, RoutePlanning route) throws DocumentException {
        PdfPTable detailsTable = new PdfPTable(1);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);
        detailsTable.setWidths(new float[]{4f});
        BaseColor receiptGreen = new BaseColor(84, 138, 70);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, receiptGreen);
        Font normalFont_2 = FontFactory.getFont(FontFactory.HELVETICA, 9, receiptGreen);

        PdfPCell supplierCell = new PdfPCell();
        supplierCell.setBorder(Rectangle.NO_BORDER);

        Paragraph supplierHeader = new Paragraph("Route Details:", headerFont);
        supplierCell.addElement(supplierHeader);

        Chunk label = new Chunk("Route Id: ", normalFont);
        Chunk value = new Chunk(route.getRouteId() != null ? route.getRouteId().toUpperCase() : "-", normalFont_2);
        Paragraph supplierName = new Paragraph();
        supplierName.add(label);
        supplierName.add(value);
        supplierCell.addElement(supplierName);

        Chunk label_Address = new Chunk("Agent Name: ", normalFont);
        Chunk value_Address = new Chunk(route.getDeliveryAgentName() != null ? route.getDeliveryAgentName() : "-", normalFont_2);
        Paragraph address = new Paragraph();
        address.add(label_Address);
        address.add(value_Address);
        supplierCell.addElement(address);

        Chunk label_Phone = new Chunk("Date: ", normalFont);
        Chunk value_Phone = new Chunk(route.getCreationTime().plusHours(3).format(dateTimeFormatter), normalFont_2);
        Paragraph phone = new Paragraph();
        phone.add(label_Phone);
        phone.add(value_Phone);
        supplierCell.addElement(phone);

        detailsTable.addCell(supplierCell);

        PdfPCell poCell = new PdfPCell();
        poCell.setBorder(Rectangle.NO_BORDER);

        poCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        detailsTable.addCell(poCell);

        document.add(detailsTable);
    }

    private ByteArrayOutputStream createIssuanceReportExcel(RoutePlanning route, List<ProductItemDTO> productItemDTOList, String name) {

        try {

            String documentTitle = "Issuance Report - " + route.getRouteId() + " - for " + route.getDeliveryAgentName();

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(documentTitle);

            org.apache.poi.ss.usermodel.Font headerFont = excelUtils.getHeaderFont(workbook);

            org.apache.poi.ss.usermodel.Font font = excelUtils.getContentFont(workbook);

            CellStyle headerCellStyle = excelUtils.getHeaderCellStyle(workbook, headerFont);

            CellStyle headerCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, headerFont);

            CellStyle stringDataCellStyleLeftAlign = excelUtils.getStringDataCellStyleLeftAlign(workbook, font);

            CellStyle numericDataCellStyleRightAlign = excelUtils.getNumericDataCellStyleRightAlign(workbook, font);

            CellStyle stringDataCellStyleCentreAlign = excelUtils.getStringDataCellStyleCentreAlign(workbook, font);

            int rowIndex = 1;

            XSSFRow row1 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row1, 0, documentTitle, headerCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            XSSFRow row3 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row3,
                    0,
                    "Printed By: \t \t     " + name,
                    stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            XSSFRow row4 = sheet.createRow(++rowIndex);
            excelUtils.createStringDataCell(row4, 0, "Printed On: \t \t     " + LocalDateTime.now(), stringDataCellStyleCentreAlign);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 20));

            List<String> contentHeaders = Arrays.asList(
                    "Product Name",
                    "Product Code",
                    "Quantity Issued"
            );

            int colIndex;
            Row headerRow = sheet.createRow(rowIndex += 2);

            for (int i = 0; i < contentHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(contentHeaders.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            ++rowIndex;

            reportRecordConverter.addRecordData(productItemDTOList,
                    sheet,
                    rowIndex,
                    stringDataCellStyleLeftAlign,
                    numericDataCellStyleRightAlign);

            for (int i = 0; i < contentHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();

            return bos;
        } catch (IOException e) {
            throw new BadRequestRestApiException().developerMessage("Error in create file")
                    .userMessage("Sorry. Error in create file");
        }
    }

    @Override
    public ByteArrayOutputStream generateSalesOrdersReport(StatementExportType exportType, List<SalesOrders> content, String name) {

        if (content.isEmpty()) {
            return null;
        }

        if (exportType == StatementExportType.PDF) {
            return salesOrderReportHelper.createSalesOrdersReportPDF(content, name);
        } else if (exportType == StatementExportType.EXCEL) {
            return salesOrderReportHelper.createSalesOrdersReportExcel(content, name);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateCustomersReport(StatementExportType exportType, List<Customers> content, String name) {

        if (content.isEmpty()) {
            return null;
        }

        if (exportType == StatementExportType.PDF) {
            return customerReportHelper.createCustomersReportPDF(content, name);
        } else if (exportType == StatementExportType.EXCEL) {
            return customerReportHelper.createCustomersReportExcel(content, name);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateProductsReport(List<Product> content, StatementExportType statementExportType, String name) {

        if (content.isEmpty()) {
            return null;
        }

        if (statementExportType == StatementExportType.EXCEL) {
            return productHelperReport.createProductsReportExcel(content, name);
        }
        throw new BadRequestRestApiException()
                .developerMessage("Invalid export type")
                .userMessage("Invalid export type");
    }

    @Override
    public SalesAgentReportPegable getSalesReport(LocalDate startDate,
                                                  LocalDate endDate,
                                                  LocalDate date,
                                                  UUID salesAgentPublicId,
                                                  String name) {

        return salesAgentReport.getSalesReport(startDate, endDate, date, salesAgentPublicId, name, Pageable.unpaged());
    }

    @Override
    public ByteArrayOutputStream generateSalesOrdersReportExport(LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 LocalDate date,
                                                                 UUID salesAgentPublicId,
                                                                 StatementExportType statementExportType,
                                                                 String name) {

        return salesAgentReport.getSalesReportExport(startDate, endDate, date, salesAgentPublicId, statementExportType, name);
    }

    @Override
    public SupplierDashboardAnalysis getSupplierDashboardAnalysis(String sku,
                                                                  UUID regionPublicId,
                                                                  UUID locationPublicId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  String name,
                                                                  Pageable pageable) {

        return supplierDashboardAnalysisReport.getSupplierDashboardAnalysis(sku,
                regionPublicId,
                locationPublicId,
                startDate,
                endDate,
                name,
                pageable);
    }

    @Override
    public ProductDistributionPageableDTO getProductDistributionReport(UUID productId,
                                                                       UUID categoryId,
                                                                       UUID regionId,
                                                                       UUID locationId,
                                                                       UUID customerId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       String name,
                                                                       Pageable pageable) {

        return productDistributionReport.getProductDistributionReport(productId,
                categoryId,
                regionId,
                locationId,
                customerId,
                startDate,
                endDate,
                name,
                pageable);
    }

    @Override
    public Page<CustomerOrderDTORapper> getPurchaseReport(String searchParm, UUID regionId, LocalDate startDate,
                                                          LocalDate endDate, String name,
                                                          Pageable pageable) {

        return getPurchaseReportV2Wrapped(searchParm, regionId, startDate, endDate, name, pageable);
    }

    @Override
    public ByteArrayOutputStream generatePurchaseOrderReportExport(List<CustomerOrderDTORapper> content,
                                                                   StatementExportType exportType,
                                                                   String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }

        Person person = optionalPerson.get();

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));

        if (suppliersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier not found")
                    .userMessage("Sorry! Supplier not found");
        }

        Suppliers supplier = suppliersOptional.get();

        if (exportType == StatementExportType.EXCEL) {
            return purchaseExportReport.generatePurchaseExcel(supplier, person.getFullName(), content);
        } else if (exportType == StatementExportType.PDF) {

            return purchaseExportReport.generatePurchasePdf(supplier, person.getFullName(), content);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ResponseEntity<List<StatisticsDTO>> getSupplierStatisticsReport(UUID regionPublicId,
                                                                           LocalDate start,
                                                                           LocalDate end,
                                                                           String name,
                                                                           Pageable pageable) {

        LocalDate finalStartDate = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate finalEndDate = end != null ? end : LocalDate.now();

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }

        Person person = optionalPerson.get();

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));

        if (suppliersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier not found")
                    .userMessage("Sorry! Supplier not found");
        }

        Suppliers supplier = suppliersOptional.get();

        List<Product> supplierProducts = productRepository.findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier),
                Pageable.unpaged()).getContent();

        List<SalesOrderItems> salesOrderItems =
                salesOrderItemsRepository.findAllByProductInAndDeletedIsFalseAndCreationTimeBetween(supplierProducts, finalStartDate.atStartOfDay(),
                        finalEndDate.atTime(23, 59, 59));
        if (regionPublicId != null) {
            salesOrderItems = salesOrderItems.stream()
                    .filter(soi -> soi.getSalesOrder().getOrganizationRegions() != null && soi.getSalesOrder().getOrganizationRegions().getPublicId().equals(regionPublicId))
                    .toList();
        }

        List<StatisticsDTO> statisticsDTOList = statisticsTransformer.convert(salesOrderItems);


        return ResponseEntity.ok().body(statisticsDTOList);

    }

    @Override
    public Page<ProductSaleDistributionDTO> generateSupplierDistributionReport(
            UUID regionPublicId, LocalDate start, LocalDate end, String email, Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> salesOrderRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> itemsJoin = salesOrderRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> productJoin = itemsJoin.join("product");
        Join<Product, Suppliers> productSuppliersJoin = productJoin.join("suppliers");

        Expression<BigDecimal> orderValueExpr = cb.sum(
                cb.prod(productJoin.get("buyingPrice"), itemsJoin.get("quantityDelivered"))
        );
        Expression<Integer> quantityExpr = cb.sum(itemsJoin.get("quantityDelivered"));

        cq.multiselect(
                productJoin.get("productName").alias("productName"),
                productJoin.get("sku").alias("sku"),
                productJoin.get("unitOfMeasure").alias("uom"),
                orderValueExpr.alias("orderValue"),
                quantityExpr.alias("quantityDelivered")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesOrderRoot.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.equal(productSuppliersJoin.get("publicId"), supplier.getPublicId()));

        if (regionPublicId != null) {
            predicates.add(cb.equal(salesOrderRoot.get("organizationRegions").get("publicId"), regionPublicId));
        }
        if (start != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrderRoot.get("creationTime"), start.atStartOfDay()));
        }
        if (end != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrderRoot.get("creationTime"), end.atTime(23, 59, 59)));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(productJoin.get("productName"), productJoin.get("sku"), productJoin.get("unitOfMeasure"));

        // Fetch paginated results
        List<Tuple> results = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<ProductSaleDistributionDTO> productRevenueList = new ArrayList<>();
        for (Tuple tuple : results) {
            ProductSaleDistributionDTO dto = new ProductSaleDistributionDTO();
            dto.setProductName(tuple.get("productName", String.class));
            dto.setSku(tuple.get("sku", String.class));
            dto.setUom(tuple.get("uom", String.class));
            dto.setOrderValue(tuple.get("orderValue", BigDecimal.class));
            dto.setQuantity(tuple.get("quantityDelivered", Integer.class));
            productRevenueList.add(dto);
        }

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SalesOrders> countRoot = countQuery.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> countItemsJoin = countRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> countProductJoin = countItemsJoin.join("product");
        Join<Product, Suppliers> countSupplierJoin = countProductJoin.join("suppliers");

        countQuery.select(cb.countDistinct(countProductJoin.get("id")));
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("salesOrderStatus"), "DELIVERED"));
        countPredicates.add(cb.equal(countSupplierJoin.get("publicId"), supplier.getPublicId()));

        if (regionPublicId != null) {
            countPredicates.add(cb.equal(countRoot.get("organizationRegions").get("publicId"), regionPublicId));
        }
        if (start != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("creationTime"), start.atStartOfDay()));
        }
        if (end != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("creationTime"), end.atTime(23, 59, 59)));
        }

        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(productRevenueList, pageable, total);
    }

    @Override
    public ResponseEntity<List<QuantitySoldPerRegionDTO>> generateSupplierProductDistributionReport(
            UUID regionId,
            LocalDate startDate,
            LocalDate endDate,
            String email,
            Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> salesOrdersRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> salesOrderItemsJoin = salesOrdersRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> productJoin = salesOrderItemsJoin.join("product");
        Join<Product, Suppliers> suppliersJoin = productJoin.join("suppliers");

        Expression<Long> totalQuantitySoldExpr =
                cb.sum(salesOrderItemsJoin.get("quantityDelivered").as(Long.class));

        Expression<BigDecimal> totalWeightSold =
                cb.sum(cb.prod(productJoin.get("weight"), salesOrderItemsJoin.get("quantityDelivered"))
                        .as(BigDecimal.class));

        cq.multiselect(
                salesOrdersRoot.get("organizationRegions").get("name").alias("regionName"),
                totalQuantitySoldExpr.alias("quantitySold"),
                totalWeightSold.alias("weightSold")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesOrdersRoot.get("salesOrderStatus"), LoadingStatusEnum.DELIVERED));
        predicates.add(cb.equal(suppliersJoin.get("publicId"), supplierPerson.getPublicId()));

        if (regionId != null) {
            predicates.add(cb.equal(salesOrdersRoot.get("organizationRegions").get("publicId"), regionId));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrdersRoot.get("creationTime"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrdersRoot.get("creationTime"), endDate.atTime(23, 59, 59)));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        cq.groupBy(salesOrdersRoot.get("organizationRegions").get("name"));

        List<Tuple> results = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<QuantitySoldPerRegionDTO> quantitySoldPerRegionList = results.stream()
                .map(tuple -> {
                    QuantitySoldPerRegionDTO dto = new QuantitySoldPerRegionDTO();
                    dto.setRegionName(tuple.get("regionName", String.class));
                    dto.setQuantitySold(tuple.get("quantitySold", Long.class));
                    dto.setWeightSold(tuple.get("weightSold", BigDecimal.class));
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(quantitySoldPerRegionList);
    }

    @Override
    public ResponseEntity<List<SalesRevenueDTO>> generateSupplierRevenueReport(UUID regionPublicId, Boolean monthly, Boolean quarterly, Boolean yearly, String name, Pageable pageable) {
        LocalDate now = LocalDate.now();
        LocalDate yearStart = now.withDayOfYear(1);
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        if (quarterly != null && quarterly == Boolean.TRUE) {
            return getSalesRevenueQuarterly(yearStart, now, regionPublicId, name, pageable);
        } else if (yearly != null && yearly == Boolean.TRUE) {
            return getSalesRevenueYearly(yearStart, now, regionPublicId, name, pageable);
        } else {
            return getSalesRevenueMonthly(startOfMonth, now, regionPublicId, name, pageable);
        }
    }

    private ResponseEntity<List<SalesRevenueDTO>> getSalesRevenueMonthly(LocalDate startOfMonth, LocalDate now,
                                                                         UUID regionPublicId, String name,
                                                                         Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<SalesOrders> salesOrder = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> items = salesOrder.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> supplier = product.join("suppliers");

        Expression<BigDecimal> sumBuyingTotal = cb.toBigDecimal(
                cb.sum(cb.prod(product.get("buyingPrice"), items.get("quantityDelivered")))
        );

        // Use DATE_PART for year, month, day
        Expression<Double> yearExpr = cb.function("DATE_PART", Double.class,
                cb.literal("year"), salesOrder.get("creationTime"));
        Expression<Double> monthExpr = cb.function("DATE_PART", Double.class,
                cb.literal("month"), salesOrder.get("creationTime"));
        Expression<Double> dayExpr = cb.function("DATE_PART", Double.class,
                cb.literal("day"), salesOrder.get("creationTime"));

        // Use TO_CHAR for month name
        Expression<String> monthNameExpr = cb.function("TO_CHAR", String.class,
                salesOrder.get("creationTime"), cb.literal("Month"));

        cq.multiselect(sumBuyingTotal, yearExpr, monthExpr, dayExpr, monthNameExpr);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(supplier.get("publicId"), supplierPerson.getPublicId()));
        predicates.add(cb.equal(salesOrder.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.between(salesOrder.get("creationTime"),
                startOfMonth.atStartOfDay(), now.atTime(LocalTime.MAX)));

        if (regionPublicId != null) {
            predicates.add(cb.equal(salesOrder.get("organizationRegions").get("publicId"), regionPublicId));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.groupBy(yearExpr, monthExpr, dayExpr, monthNameExpr);

        List<Object[]> rawResults = entityManager.createQuery(cq).getResultList();

        List<SalesRevenueDTO> results = rawResults.stream().map(r -> {
            BigDecimal total = (BigDecimal) r[0];
            Integer year = ((Double) r[1]).intValue();
            Integer month = ((Double) r[2]).intValue();
            Integer day = ((Double) r[3]).intValue();
            String monthName = (String) r[4];

            SalesRevenueDTO dto = new SalesRevenueDTO();
            dto.setTotalRevenue(total != null ? total : BigDecimal.ZERO);
            dto.setYear(year);
            dto.setMonth(month);
            dto.setQuarter(null);
            dto.setDay(day.toString());
            dto.setLabel(day + " " + monthName.trim() + " " + year); // e.g. "7 February 2026"

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }


    private ResponseEntity<List<SalesRevenueDTO>> getSalesRevenueQuarterly(LocalDate startOfYear, LocalDate now,
                                                                           UUID regionPublicId, String name,
                                                                           Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<SalesOrders> salesOrder = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> items = salesOrder.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> supplier = product.join("suppliers");

        Expression<BigDecimal> sumBuyingTotal = cb.toBigDecimal(
                cb.sum(cb.prod(product.get("buyingPrice"), items.get("quantityDelivered")))
        );

        // Use DATE_PART for year and month (returns Double)
        Expression<Double> yearExpr = cb.function("DATE_PART", Double.class,
                cb.literal("year"), salesOrder.get("creationTime"));
        Expression<Double> monthExpr = cb.function("DATE_PART", Double.class,
                cb.literal("month"), salesOrder.get("creationTime"));

        cq.multiselect(sumBuyingTotal, yearExpr, monthExpr);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(supplier.get("publicId"), supplierPerson.getPublicId()));
        predicates.add(cb.equal(salesOrder.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.between(salesOrder.get("creationTime"),
                startOfYear.atStartOfDay(), now.atTime(LocalTime.MAX)));

        if (regionPublicId != null) {
            predicates.add(cb.equal(salesOrder.get("organizationRegions").get("publicId"), regionPublicId));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.groupBy(yearExpr, monthExpr);

        List<Object[]> rawResults = entityManager.createQuery(cq).getResultList();

        // Aggregate monthly results into quarters
        Map<String, BigDecimal> quarterTotals = new LinkedHashMap<>();

        for (Object[] r : rawResults) {
            BigDecimal total = (BigDecimal) r[0];
            Integer year = ((Double) r[1]).intValue();
            Integer month = ((Double) r[2]).intValue();

            int quarter = ((month - 1) / 3) + 1;
            String key = "Q" + quarter + " " + year;

            quarterTotals.merge(key, total != null ? total : BigDecimal.ZERO, BigDecimal::add);
        }

        List<SalesRevenueDTO> results = quarterTotals.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split(" ");
            int quarter = Integer.parseInt(parts[0].substring(1));
            int year = Integer.parseInt(parts[1]);

            SalesRevenueDTO dto = new SalesRevenueDTO();
            dto.setTotalRevenue(entry.getValue());
            dto.setYear(year);
            dto.setMonth(null);
            dto.setQuarter(quarter);
            dto.setLabel(entry.getKey()); // e.g. "Q1 2026"

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }


    private ResponseEntity<List<SalesRevenueDTO>> getSalesRevenueYearly(LocalDate startOfYear, LocalDate now,
                                                                        UUID regionPublicId, String name,
                                                                        Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<SalesOrders> salesOrder = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> items = salesOrder.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> supplier = product.join("suppliers");

        Expression<BigDecimal> sumBuyingTotal = cb.toBigDecimal(
                cb.sum(cb.prod(product.get("buyingPrice"), items.get("quantityDelivered")))
        );

        // Use DATE_PART for year and month (returns Double in Postgres)
        Expression<Double> yearExpr = cb.function("DATE_PART", Double.class,
                cb.literal("year"), salesOrder.get("creationTime"));
        Expression<Double> monthExpr = cb.function("DATE_PART", Double.class,
                cb.literal("month"), salesOrder.get("creationTime"));

        cq.multiselect(sumBuyingTotal, yearExpr, monthExpr);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(supplier.get("publicId"), supplierPerson.getPublicId()));
        predicates.add(cb.equal(salesOrder.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.between(salesOrder.get("creationTime"),
                startOfYear.atStartOfDay(), now.atTime(LocalTime.MAX)));

        if (regionPublicId != null) {
            predicates.add(cb.equal(salesOrder.get("organizationRegions").get("publicId"), regionPublicId));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.groupBy(yearExpr, monthExpr);

        List<Object[]> rawResults = entityManager.createQuery(cq).getResultList();

        List<SalesRevenueDTO> results = rawResults.stream().map(r -> {
            BigDecimal total = (BigDecimal) r[0];
            Integer year = ((Double) r[1]).intValue();
            Integer month = ((Double) r[2]).intValue();

            SalesRevenueDTO dto = new SalesRevenueDTO();
            dto.setTotalRevenue(total != null ? total : BigDecimal.ZERO);
            dto.setYear(year);
            dto.setMonth(month);
            dto.setQuarter(null);

            // Build label in Java: "FEBRUARY 2026"
            dto.setLabel(Month.of(month).name() + " " + year);

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }


//    @Override
//    public ResponseEntity<List<SalesRevenueDTO>> generateSupplierRevenueReport(UUID regionPublicId, Boolean monthly, Boolean quarterly, Boolean yearly, String name, Pageable pageable) {
//
//        LocalDate finalStartDate;
//        LocalDate finalEndDate;
//
//        if (Validators.anyEqualNull(monthly, quarterly, yearly) || (!monthly && !quarterly && !yearly)) {
//            finalStartDate = LocalDate.now().withDayOfMonth(1);
//            finalEndDate = LocalDate.now();
//        } else {
//            if (monthly) {
//                finalStartDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);
//                finalEndDate = LocalDate.now();
//            } else if (quarterly) {
//                finalStartDate = LocalDate.now().minusMonths(3).withDayOfMonth(1);
//                finalEndDate = LocalDate.now();
//            } else {
//                finalStartDate = LocalDate.now().minusMonths(12).withDayOfMonth(1);
//                finalEndDate = LocalDate.now();
//            }
//        }
//
//        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
//
//        if (optionalPerson.isEmpty()) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("user not found")
//                    .userMessage("Sorry!. user not found");
//        }
//
//        Person person = optionalPerson.get();
//
//        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("no associated supplier")
//                    .userMessage("Sorry!. no associated supplier");
//        }
//
//        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));
//
//        if (suppliersOptional.isEmpty()) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("supplier not found")
//                    .userMessage("Sorry! Supplier not found");
//        }
//
//        Suppliers supplier = suppliersOptional.get();
//
//        List<Product> supplierProducts = productRepository.findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier),
//                Pageable.unpaged()).getContent();
//
//        List<SalesOrderItems> salesOrderItems =
//                salesOrderItemsRepository.findAllByProductInAndDeletedIsFalseAndCreationTimeBetween(supplierProducts, finalStartDate.atStartOfDay(),
//                        finalEndDate.atTime(23, 59, 59));
//        if (regionPublicId != null) {
//            salesOrderItems = salesOrderItems.stream()
//                    .filter(soi -> soi.getSalesOrder().getOrganizationRegions() != null && soi.getSalesOrder().getOrganizationRegions().getPublicId().equals(regionPublicId))
//                    .toList();
//        }
//
//        List<SalesRevenueDTO> salesRevenueDTOList = salesRevenueTransformer.convert(salesOrderItems, monthly, quarterly, yearly);
//
//        return ResponseEntity.ok().body(salesRevenueDTOList);
//    }

    @Override
    public ResponseEntity<List<ProductPerformanceDTO>> getProductPerformanceReport(UUID productId, UUID regionId, List<UUID> zones, LocalDate start, LocalDate end, String name, Pageable pageable) {
        LocalDate finalStartDate = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate finalEndDate = end != null ? end : LocalDate.now();

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }

        Person person = optionalPerson.get();

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));

        if (suppliersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier not found")
                    .userMessage("Sorry! Supplier not found");
        }

        Suppliers supplier = suppliersOptional.get();

        Product supplierProduct = productRepository.findByPublicId(productId);

        if (supplierProduct == null || !supplierProduct.getSuppliers().contains(supplier)) {
            throw new BadRequestRestApiException()
                    .developerMessage("product not found for supplier")
                    .userMessage("Sorry! product not found for supplier");
        }


        List<SalesOrderItems> salesOrderItems =
                salesOrderItemsRepository.findAllByProductInAndDeletedIsFalseAndCreationTimeBetween(Collections.singletonList(supplierProduct), finalStartDate.atStartOfDay(),
                        finalEndDate.atTime(23, 59, 59));

        salesOrderItems = salesOrderItems.stream()
                .filter(soi -> {
                    boolean matchesRegion = true;
                    boolean matchesZone = true;

                    if (regionId != null) {
                        matchesRegion = soi.getSalesOrder().getOrganizationRegions() != null &&
                                soi.getSalesOrder().getOrganizationRegions().getPublicId().equals(regionId);
                    }

                    if (zones != null && !zones.isEmpty()) {
                        matchesZone = soi.getSalesOrder().getCustomer() != null &&
                                soi.getSalesOrder().getCustomer().getOrganizationLocation() != null &&
                                zones.contains(soi.getSalesOrder().getCustomer().getOrganizationLocation().getPublicId());
                    }

                    return matchesRegion && matchesZone;
                })
                .toList();


        List<ProductPerformanceDTO> productPerformanceDTOList = productPerformanceTransformer.convert(salesOrderItems);

        return ResponseEntity.ok().body(productPerformanceDTOList);

    }

    @Override
    public ResponseEntity<List<ProductPerformancePerZone>> getProductPerformancePerZoneReport(UUID productId, UUID regionId, List<UUID> zones, LocalDate startDate, LocalDate endDate, String name, Pageable pageable) {
        LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }

        Person person = optionalPerson.get();

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));

        if (suppliersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier not found")
                    .userMessage("Sorry! Supplier not found");
        }

        Suppliers supplier = suppliersOptional.get();

        Product supplierProduct = productRepository.findByPublicId(productId);

        if (supplierProduct == null || !supplierProduct.getSuppliers().contains(supplier)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<SalesOrderItems> salesOrderItems =
                salesOrderItemsRepository.findAllByProductInAndDeletedIsFalseAndCreationTimeBetween(Collections.singletonList(supplierProduct), finalStartDate.atStartOfDay(),
                        finalEndDate.atTime(23, 59, 59));
        salesOrderItems = salesOrderItems.stream()
                .filter(soi -> {
                    boolean matchesRegion = true;
                    boolean matchesZone = true;
                    if (regionId != null) {
                        matchesRegion = soi.getSalesOrder().getOrganizationRegions() != null &&
                                soi.getSalesOrder().getOrganizationRegions().getPublicId().equals(regionId);
                    }
                    if (zones != null && !zones.isEmpty()) {
                        matchesZone = soi.getSalesOrder().getCustomer() != null &&
                                soi.getSalesOrder().getCustomer().getOrganizationLocation() != null &&
                                zones.contains(soi.getSalesOrder().getCustomer().getOrganizationLocation().getPublicId());
                    }
                    return matchesRegion && matchesZone;
                })
                .toList();

        List<ProductPerformancePerZone> productPerformancePerZoneList = productPerformancePerZoneTransformer.convert(salesOrderItems);

        return ResponseEntity.ok().body(productPerformancePerZoneList);
    }

    @Override
    public Page<ProductCustomerPerformanceDTO> getProductCustomerPerformanceReport(
            UUID productId,
            UUID regionId,
            UUID locationId,
            LocalDate startDate,
            LocalDate endDate,
            String name,
            Pageable pageable) {

        LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductCustomerPerformanceDTO> cq = cb.createQuery(ProductCustomerPerformanceDTO.class);
        Root<SalesOrders> salesOrder = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> items = salesOrder.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> supplier = product.join("suppliers");
        Join<SalesOrders, Customers> customer = salesOrder.join("customer");

        Expression<Long> orderCount = cb.countDistinct(salesOrder.get("id"));
        Expression<BigDecimal> avgBuyingTotal = cb.toBigDecimal(
                cb.avg(cb.prod(product.get("buyingPrice"), items.get("quantityDelivered")))
        );

        cq.select(cb.construct(
                ProductCustomerPerformanceDTO.class,
                customer.get("name"),
                orderCount,
                avgBuyingTotal
        ));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(supplier.get("publicId"), supplierPerson.getPublicId()));
        predicates.add(cb.equal(salesOrder.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.between(salesOrder.get("creationTime"), finalStartDate, finalEndDate));

        if (regionId != null) {
            predicates.add(cb.equal(customer.get("organizationRegions").get("publicId"), regionId));
        }
        if (locationId != null) {
            predicates.add(cb.equal(customer.get("organizationLocation").get("publicId"), locationId));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.groupBy(customer.get("id"), customer.get("name"));

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                Expression<?> sortExpr;
                if (sortOrder.getProperty().equals("customerName")) {
                    sortExpr = customer.get("name");
                } else if (sortOrder.getProperty().equals("numberOfOrders")) {
                    sortExpr = orderCount;
                } else if (sortOrder.getProperty().equals("aov")) {
                    sortExpr = avgBuyingTotal;
                } else {
                    sortExpr = customer.get("name");
                }
                orders.add(sortOrder.isAscending() ? cb.asc(sortExpr) : cb.desc(sortExpr));
            }
            cq.orderBy(orders);
        }

        TypedQuery<ProductCustomerPerformanceDTO> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<ProductCustomerPerformanceDTO> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SalesOrders> countRoot = countQuery.from(SalesOrders.class);
        Join<SalesOrders, Customers> countCustomer = countRoot.join("customer");
        Join<SalesOrders, SalesOrderItems> countItems = countRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> countProduct = countItems.join("product");
        Join<Product, Suppliers> countSupplier = countProduct.join("suppliers");

        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countSupplier.get("publicId"), supplierPerson.getPublicId()));
        countPredicates.add(cb.equal(countRoot.get("salesOrderStatus"), "DELIVERED"));
        countPredicates.add(cb.between(countRoot.get("creationTime"), finalStartDate, finalEndDate));

        if (regionId != null) {
            countPredicates.add(cb.equal(countCustomer.get("organizationRegions").get("publicId"), regionId));
        }
        if (locationId != null) {
            countPredicates.add(cb.equal(countCustomer.get("organizationLocation").get("publicId"), locationId));
        }

        countQuery.select(cb.countDistinct(countCustomer.get("id")));
        countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }


    @Override
    public Map<String, Integer> generateSupplierTotalOrdersReport(
            UUID regionPublicId, LocalDate start, LocalDate end, String email) {

        LocalDateTime finalStartDate = Optional.ofNullable(start)
                .map(LocalDate::atStartOfDay)
                .orElse(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        LocalDateTime finalEndDate = Optional.ofNullable(end)
                .map(d -> d.atTime(23, 59, 59))
                .orElse(LocalDate.now().atTime(23, 59, 59));

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));


        Long totalOrders = salesOrdersRepository.countDeliveredOrdersBySupplierAndRegion(
                supplier.getPublicId(),
                regionPublicId,
                finalStartDate,
                finalEndDate
        );

        return Map.of("totalOrders", totalOrders != null ? totalOrders.intValue() : 0);
    }

    @Override
    public Map<String, Integer> getQuantitySoldReport(UUID regionPublicId, LocalDate start, LocalDate end, String email) {
        LocalDateTime finalStartDate = Optional.ofNullable(start)
                .map(LocalDate::atStartOfDay)
                .orElse(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        LocalDateTime finalEndDate = Optional.ofNullable(end)
                .map(d -> d.atTime(23, 59, 59))
                .orElse(LocalDate.now().atTime(23, 59, 59));

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);

        Root<SalesOrders> so = cq.from(SalesOrders.class);
        Join<SalesOrders, Customers> c = so.join("customer");
        Join<SalesOrders, SalesOrderItems> soi = so.join("salesOrderItems");
        Join<SalesOrderItems, Product> p = soi.join("product");
        Join<Product, Suppliers> s = p.join("suppliers");

        // Expression for weight * quantity
        Expression<BigDecimal> totalExpr = cb.sum(
                cb.prod(p.get("weight"), soi.get("quantityDelivered"))
        );

        cq.select(totalExpr);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(s.get("publicId"), supplier.getPublicId()));
        predicates.add(cb.equal(so.get("salesOrderStatus"), "DELIVERED"));

        if (regionPublicId != null) {
            predicates.add(cb.equal(c.get("organizationRegions").get("publicId"), regionPublicId));
        }
        if (finalStartDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(so.get("creationTime"), finalStartDate));
        }
        if (finalEndDate != null) {
            predicates.add(cb.lessThanOrEqualTo(so.get("creationTime"), finalEndDate));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        BigDecimal totalOrders = entityManager.createQuery(cq).getSingleResult();

        return Map.of("totalQuantitySold", totalOrders != null ? totalOrders.intValue() : 0);
    }


    //    @Override
    public Map<String, Integer> getQuantitySoldReportUpdated(UUID regionPublicId, LocalDate start, LocalDate end,
                                                             String email) {
        LocalDateTime finalStartDate = Optional.ofNullable(start)
                .map(LocalDate::atStartOfDay)
                .orElse(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        LocalDateTime finalEndDate = Optional.ofNullable(end)
                .map(d -> d.atTime(23, 59, 59))
                .orElse(LocalDate.now().atTime(23, 59, 59));

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        BigDecimal totalOrders = salesOrdersRepository.totalWeightDelivered(
                supplier.getPublicId(),
                regionPublicId,
                finalStartDate,
                finalEndDate
        );

        return Map.of("totalQuantitySold", totalOrders != null ? totalOrders.intValue() : 0);
    }

    @Override
    public Map<String, Integer> getTotalActiveCustomers(UUID regionPublicId, LocalDate start, LocalDate end, String email) {
        LocalDateTime finalStartDate = Optional.ofNullable(start)
                .map(LocalDate::atStartOfDay)
                .orElse(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        LocalDateTime finalEndDate = Optional.ofNullable(end)
                .map(d -> d.atTime(23, 59, 59))
                .orElse(LocalDate.now().atTime(23, 59, 59));

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        Long totalOrders = salesOrdersRepository.countActiveCustomers(
                supplier.getPublicId(),
                regionPublicId,
                finalStartDate,
                finalEndDate
        );

        return Map.of("totalActiveCustomers", totalOrders != null ? totalOrders.intValue() : 0);
    }

    @Override
    public Map<String, BigDecimal> runSupplierRevenueReport(
            UUID regionId,
            LocalDate startDate,
            LocalDate endDate,
            String email,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> salesOrderRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> itemsJoin = salesOrderRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> productJoin = itemsJoin.join("product");
        Join<Product, Suppliers> supplierJoin = productJoin.join("suppliers");

        Expression<BigDecimal> costOfGoodsExpr = cb.sum(
                cb.prod(productJoin.get("buyingPrice"), itemsJoin.get("quantityDelivered")));

        cq.multiselect(
                supplierJoin.get("supplierName").alias("supplierName"),
                costOfGoodsExpr.alias("costOfGoods")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesOrderRoot.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.equal(supplierJoin.get("publicId"), supplier.getPublicId()));

        if (regionId != null) {
            predicates.add(cb.equal(salesOrderRoot.get("organizationRegions").get("publicId"), regionId));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrderRoot.get("creationTime"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrderRoot.get("creationTime"), endDate.atTime(23, 59, 59)));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(supplierJoin.get("supplierName"));

        List<Tuple> results = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Map<String, BigDecimal> costOfGoodsBySupplier = new HashMap<>();
        for (Tuple tuple : results) {
            String supplierName = tuple.get("supplierName", String.class);
            BigDecimal costOfGoods = tuple.get("costOfGoods", BigDecimal.class);

            costOfGoodsBySupplier.put("revenue", costOfGoods);
        }

        return costOfGoodsBySupplier;
    }

    @Override
    public ByteArrayOutputStream productCustomerPerformanceExport(UUID productId, UUID regionId, UUID locationId,
                                                                  LocalDate startDate, LocalDate endDate, String name, StatementExportType statementExportType, String name1, Pageable pageable) {
        List<ProductCustomerPerformanceDTO> response = getProductCustomerPerformanceReport(productId,
                regionId,
                locationId, startDate, endDate, name, pageable).getContent();


        return customerPerformanceExportHelper.createExport(response, name);
    }

    @Override
    public ByteArrayOutputStream generateLoanManagementReportExport(List<BnplLoanManagement> content, StatementExportType exportType, String email) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(email);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return loanManagementReportExport.generateLoanManagementExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return loanManagementReportExport.generateLoanManagementPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    public Page<CustomerOrderDTORapper> getPurchaseReportV2Wrapped(
            String searchParm,
            UUID regionId,
            LocalDate startDate,
            LocalDate endDate,
            String email,
            Pageable pageable) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        Suppliers supplierPerson = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> salesOrdersRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, Customers> customersJoin = salesOrdersRoot.join("customer");
        Join<SalesOrders, SalesOrderItems> salesOrderItemsJoin = salesOrdersRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> productJoin = salesOrderItemsJoin.join("product");
        Join<Product, Suppliers> suppliersJoin = productJoin.join("suppliers");

        Expression<Long> totalQuantityDeliveredExpr =
                cb.sum(salesOrderItemsJoin.get("quantityDelivered").as(Long.class));

        cq.multiselect(
                customersJoin.get("name").alias("customerName"),
                customersJoin.get("organizationRegions").get("name").alias("region"),
                customersJoin.get("organizationLocation").get("name").alias("location"),
                customersJoin.get("address").alias("address"),
                productJoin.get("productName").alias("productName"),
                productJoin.get("sku").alias("sku"),
                totalQuantityDeliveredExpr.alias("quantityPurchased"),
                salesOrdersRoot.get("creationTime").alias("purchaseDate"),
                salesOrdersRoot.get("salesOrderNumber").alias("salesOrderNumber")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesOrdersRoot.get("salesOrderStatus"), LoadingStatusEnum.DELIVERED));
        predicates.add(cb.equal(suppliersJoin.get("publicId"), supplierPerson.getPublicId()));

        if (searchParm != null && searchParm.length() > 2) {
            Predicate productNameMatch = cb.like(cb.lower(productJoin.get("productName")), "%" + searchParm.toLowerCase() + "%");
            Predicate orderNumberMatch = cb.like(cb.lower(salesOrdersRoot.get("salesOrderNumber")), "%" + searchParm.toLowerCase() + "%");
            Predicate customerNameMatch = cb.like(cb.lower(customersJoin.get("name")), "%" + searchParm.toLowerCase() + "%");
            Predicate customeAddressMatch = cb.like(cb.lower(customersJoin.get("address")),
                    "%" + searchParm.toLowerCase() + "%");
            predicates.add(cb.or(productNameMatch, orderNumberMatch, customerNameMatch, customeAddressMatch));
        }

        if (regionId != null) {
            predicates.add(cb.equal(customersJoin.get("organizationRegions").get("publicId"), regionId));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrdersRoot.get("creationTime"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrdersRoot.get("creationTime"), endDate.atTime(23, 59, 59)));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(salesOrdersRoot.get("creationTime"),
                customersJoin.get("name"),
                customersJoin.get("organizationLocation").get("name"),
                customersJoin.get("organizationRegions").get("name"),
                customersJoin.get("address"),
                productJoin.get("productName"),
                productJoin.get("sku"),
                salesOrdersRoot.get("salesOrderNumber"));

        List<Tuple> results = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Step 1: Convert to flat DTOs
        List<CustomerOrderDTO> productRevenueList = new ArrayList<>();
        for (Tuple tuple : results) {
            CustomerOrderDTO dto = new CustomerOrderDTO();
            dto.setCustomerName(tuple.get("customerName", String.class));
            dto.setProductName(tuple.get("productName", String.class));
            dto.setPurchaseDate(tuple.get("purchaseDate", LocalDateTime.class));
            dto.setRegion(tuple.get("region", String.class));
            dto.setLocation(tuple.get("location", String.class));
            dto.setSkuCode(tuple.get("sku", String.class));
            dto.setSalesOrderNumber(tuple.get("salesOrderNumber", String.class));
            dto.setAddress(tuple.get("address", String.class));

            Number qty = tuple.get("quantityPurchased", Number.class);
            dto.setQuantityPurchased(qty != null ? qty.longValue() : 0L);

            productRevenueList.add(dto);
        }

        List<CustomerOrderDTO> collectWithMoreThanZeroQuantityPurchased =
                productRevenueList.stream().filter(item -> item.getQuantityPurchased() > 0).toList();

        Map<String, CustomerOrderDTORapper> grouped = collectWithMoreThanZeroQuantityPurchased.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getSalesOrderNumber() + "|" + dto.getCustomerName() + "|" +
                                dto.getRegion() + "|" + dto.getLocation() + "|" + dto.getAddress(),
                        dto -> new CustomerOrderDTORapper(
                                dto.getSalesOrderNumber(), // routeId
                                dto.getCustomerName(),
                                dto.getRegion(),
                                dto.getLocation(),
                                dto.getAddress(),
                                new ArrayList<>(List.of(
                                        new CustomerOrderDTORapper.InnerCustomerDetails(
                                                dto.getProductName(),
                                                dto.getSkuCode(),
                                                dto.getQuantityPurchased(),
                                                dto.getPurchaseDate()
                                        )
                                ))
                        ),
                        (existing, incoming) -> {
                            existing.innerCustomerDetailsList().addAll(incoming.innerCustomerDetailsList());
                            return existing;
                        }
                ));

        List<CustomerOrderDTORapper> wrappedList = new ArrayList<>(grouped.values());

        long total = wrappedList.size();

        return new PageImpl<>(wrappedList, pageable, total);
    }

    @Override
    public InventoryDashboardSummaryDTO getInventoryDashboardSummary(UUID regionId, LocalDate startDate, LocalDate endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<InventoryProducts> root = cq.from(InventoryProducts.class);
        Join<InventoryProducts, Product> productJoin = root.join("product");
        Join<InventoryProducts, Inventory> inventoryJoin = root.join("inventory");
        Join<Inventory, Warehouse> warehouseJoin = inventoryJoin.join("warehouse");

        cq.multiselect(
                cb.sum(root.get("availableStock").as(Long.class)).alias("totalQuantity"),
                cb.countDistinct(productJoin.get("id")).alias("products"),
                cb.countDistinct(warehouseJoin.get("organizationRegion").get("id")).alias("regions")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(productJoin.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(inventoryJoin.get("deleted"), Boolean.FALSE));
        if (regionId != null) {
            predicates.add(cb.equal(warehouseJoin.get("organizationRegion").get("publicId"), regionId));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        Tuple result = entityManager.createQuery(cq).getSingleResult();
        InventoryDashboardSummaryDTO response = new InventoryDashboardSummaryDTO();
        response.setTotalQuantity(Optional.ofNullable(result.get("totalQuantity", Long.class)).orElse(0L));
        response.setProducts(Optional.ofNullable(result.get("products", Long.class)).orElse(0L));
        response.setRegions(Optional.ofNullable(result.get("regions", Long.class)).orElse(0L));
        return response;
    }

    @Override
    public List<InventoryQuantityByProductDTO> getInventoryDashboardQuantityByProduct(UUID regionId, LocalDate startDate, LocalDate endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<InventoryProducts> root = cq.from(InventoryProducts.class);
        Join<InventoryProducts, Product> productJoin = root.join("product");
        Join<InventoryProducts, Inventory> inventoryJoin = root.join("inventory");
        Join<Inventory, Warehouse> warehouseJoin = inventoryJoin.join("warehouse");

        cq.multiselect(
                productJoin.get("sku").alias("code"),
                productJoin.get("productName").alias("product"),
                cb.sum(root.get("availableStock").as(Long.class)).alias("quantity")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(productJoin.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(inventoryJoin.get("deleted"), Boolean.FALSE));
        if (regionId != null) {
            predicates.add(cb.equal(warehouseJoin.get("organizationRegion").get("publicId"), regionId));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(productJoin.get("sku"), productJoin.get("productName"));
        cq.orderBy(cb.desc(cb.sum(root.get("availableStock").as(Long.class))));

        List<Tuple> results = entityManager.createQuery(cq).getResultList();
        List<InventoryQuantityByProductDTO> response = new ArrayList<>();
        for (Tuple tuple : results) {
            InventoryQuantityByProductDTO dto = new InventoryQuantityByProductDTO();
            dto.setCode(tuple.get("code", String.class));
            dto.setProduct(tuple.get("product", String.class));
            dto.setQuantity(Optional.ofNullable(tuple.get("quantity", Long.class)).orElse(0L));
            response.add(dto);
        }
        return response;
    }

    @Override
    public List<InventoryRegionDistributionDTO> getInventoryDashboardDistributionByRegion(UUID regionId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime finalStartDate = Optional.ofNullable(startDate).map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime finalEndDate = Optional.ofNullable(endDate).map(d -> d.atTime(23, 59, 59)).orElse(null);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> salesOrderRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> itemsJoin = salesOrderRoot.join("salesOrderItems");

        cq.multiselect(
                salesOrderRoot.get("organizationRegions").get("name").alias("region"),
                cb.sum(itemsJoin.get("quantityDelivered").as(Long.class)).alias("quantity")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesOrderRoot.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(salesOrderRoot.get("salesOrderStatus"), LoadingStatusEnum.DELIVERED));
        if (finalStartDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrderRoot.get("creationTime"), finalStartDate));
        }
        if (finalEndDate != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrderRoot.get("creationTime"), finalEndDate));
        }
        if (regionId != null) {
            predicates.add(cb.equal(salesOrderRoot.get("organizationRegions").get("publicId"), regionId));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(salesOrderRoot.get("organizationRegions").get("name"));
        cq.orderBy(cb.desc(cb.sum(itemsJoin.get("quantityDelivered").as(Long.class))));

        List<Tuple> results = entityManager.createQuery(cq).getResultList();
        List<InventoryRegionDistributionDTO> response = new ArrayList<>();
        for (Tuple tuple : results) {
            InventoryRegionDistributionDTO dto = new InventoryRegionDistributionDTO();
            dto.setRegion(tuple.get("region", String.class));
            dto.setQuantity(Optional.ofNullable(tuple.get("quantity", Long.class)).orElse(0L));
            response.add(dto);
        }
        return response;
    }

    @Override
    public Page<InventoryDashboardDetailDTO> getInventoryDashboardDetails(UUID regionId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<InventoryProducts> root = cq.from(InventoryProducts.class);
        Join<InventoryProducts, Product> productJoin = root.join("product");
        Join<InventoryProducts, Inventory> inventoryJoin = root.join("inventory");
        Join<Inventory, Warehouse> warehouseJoin = inventoryJoin.join("warehouse");

        cq.multiselect(
                productJoin.get("sku").alias("code"),
                productJoin.get("productName").alias("product"),
                warehouseJoin.get("organizationRegion").get("name").alias("region"),
                cb.sum(root.get("availableStock").as(Long.class)).alias("quantity")
        );

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(productJoin.get("deleted"), Boolean.FALSE));
        predicates.add(cb.equal(inventoryJoin.get("deleted"), Boolean.FALSE));
        if (regionId != null) {
            predicates.add(cb.equal(warehouseJoin.get("organizationRegion").get("publicId"), regionId));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(productJoin.get("sku"), productJoin.get("productName"), warehouseJoin.get("organizationRegion").get("name"));
        cq.orderBy(cb.desc(cb.sum(root.get("availableStock").as(Long.class))));

        List<Tuple> tuples = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<InventoryDashboardDetailDTO> content = new ArrayList<>();
        for (Tuple tuple : tuples) {
            InventoryDashboardDetailDTO dto = new InventoryDashboardDetailDTO();
            dto.setCode(tuple.get("code", String.class));
            dto.setProduct(tuple.get("product", String.class));
            dto.setRegion(tuple.get("region", String.class));
            dto.setQuantity(Optional.ofNullable(tuple.get("quantity", Long.class)).orElse(0L));
            content.add(dto);
        }

        CriteriaQuery<Tuple> countQuery = cb.createTupleQuery();
        Root<InventoryProducts> countRoot = countQuery.from(InventoryProducts.class);
        Join<InventoryProducts, Product> countProductJoin = countRoot.join("product");
        Join<InventoryProducts, Inventory> countInventoryJoin = countRoot.join("inventory");
        Join<Inventory, Warehouse> countWarehouseJoin = countInventoryJoin.join("warehouse");

        countQuery.multiselect(
                countProductJoin.get("sku"),
                countProductJoin.get("productName"),
                countWarehouseJoin.get("organizationRegion").get("name")
        );
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("deleted"), Boolean.FALSE));
        countPredicates.add(cb.equal(countProductJoin.get("deleted"), Boolean.FALSE));
        countPredicates.add(cb.equal(countInventoryJoin.get("deleted"), Boolean.FALSE));
        if (regionId != null) {
            countPredicates.add(cb.equal(countWarehouseJoin.get("organizationRegion").get("publicId"), regionId));
        }
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        countQuery.groupBy(countProductJoin.get("sku"), countProductJoin.get("productName"), countWarehouseJoin.get("organizationRegion").get("name"));
        long total = entityManager.createQuery(countQuery).getResultList().size();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<BestProductBoughtDto> getCustomerMostBoughtProductReport(UUID customerPublicId, String name) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BestProductBoughtDto> cq = cb.createQuery(BestProductBoughtDto.class);

        Root<SalesOrders> salesOrder = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> items = salesOrder.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<SalesOrders, Customers> customer = salesOrder.join("customer");

        cq.select(cb.construct(
                        BestProductBoughtDto.class,
                        product.get("productName"),
                        product.get("sku"),
                        cb.sum(items.get("quantityDelivered").as(Long.class))
                )
        );

        cq.where(
                cb.and(
                        cb.equal(customer.get("publicId"), customerPublicId),
                        cb.equal(salesOrder.get("salesOrderStatus"), "DELIVERED")
                )
        );

        cq.groupBy(product.get("productName"), product.get("sku"));
        cq.orderBy(cb.desc(cb.sum(items.get("quantityDelivered"))));

        TypedQuery<BestProductBoughtDto> query = entityManager.createQuery(cq);
        query.setMaxResults(5);

        return query.getResultList();
    }

    @Override
    public ByteArrayOutputStream generateBNPLCustomerListReportExport(List<BNPLCustomerResponseDTO> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return bnplCustomerListReportExport.generateBNPLCustomerListExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return bnplCustomerListReportExport.generateBNPLCustomerListPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateBNPLEligibleCustomerListReportExport(List<BNPLEligibleCustomerDTO> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return bnplEligibleCustomerListReportExport.generateBNPLEligibleCustomerListExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return bnplEligibleCustomerListReportExport.generateBNPLEligibleCustomerListPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateCreditReconReportExport(List<CreditRecon> content, StatementExportType exportType, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return creditReconReportExport.generateCreditReconExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return creditReconReportExport.generateCreditReconPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateCustomerCreditItemsReportExport(List<CreditLimitItem> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return customerCreditItemsReportExport.generateCustomerCreditItemsExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return customerCreditItemsReportExport.generateCustomerCreditItemsPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateCustomerCreditPaymentReportExport(List<BNPLPaymentResponse> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return customerCreditPaymentReportExport.generateCustomerCreditPaymentExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return customerCreditPaymentReportExport.generateCustomerCreditPaymentPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateBnplSalesOrdersReport(StatementExportType exportType, List<SalesOrders> content, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return bnplSalesOrdersReportExport.generateBnplSalesOrdersExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {

            return bnplSalesOrdersReportExport.generateBnplSalesOrdersPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateWalletReportExport(List<WalletResponseDTO> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return walletReportExport.generateWalletExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {
            return walletReportExport.generateWalletPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream generateLocationApprovalExport(List<LocationApproval> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return locationApprovalReportExport.generateLocationApprovalExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {
            return locationApprovalReportExport.generateLocationApprovalPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream getLeadExportReport(List<BnplLeadResponse> content, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return leadReportExport.generateLeadExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {
            return leadReportExport.generateLeadPdf(content, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream downloadCashCreditReconReport(CashCreditReconReportDTO cashCreditReconReportDTO, StatementExportType exportType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }
        Person person = optionalPerson.get();
        if (exportType == StatementExportType.EXCEL) {
            return cashCreditReconReportExport.generateCashCreditReconExcel(cashCreditReconReportDTO, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {
            return cashCreditReconReportExport.generateCashCreditReconPdf(cashCreditReconReportDTO, person.getFullName());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
        }
    }

    @Override
    public ByteArrayOutputStream getAllSalesOrdersForSAExport(SalesAgentOrderPageable salesAgentOrderPageable, StatementExportType exportType) {
        if (exportType == StatementExportType.PDF) {
            return salesOrderReportHelper.generateSalesAgentPdf(salesAgentOrderPageable);
        } else
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid export type")
                    .userMessage("Invalid export type");
    }

    @Override
    public ByteArrayOutputStream generateCreditLimitAdjustmentsReportExport(List<BnplCreditLimitAdjustmentResponse> content, StatementExportType exportType, String name) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found").userMessage("Sorry!. user not found"));
        if (exportType == StatementExportType.EXCEL) {
            return creditLimitAdjustmentsReportExport.generateExcel(content, person.getFullName());
        } else if (exportType == StatementExportType.PDF) {
            return creditLimitAdjustmentsReportExport.generatePdf(content, person.getFullName());
        }
        throw new BadRequestRestApiException()
                .developerMessage("Invalid export type").userMessage("Invalid export type");
    }
}
