package com.app.itibari.services;

import com.app.itibari.dataTypes.BannerAction;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.FileData;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.request.BannerRequest;
import com.app.itibari.models.request.FileReceiptDTO;
import com.app.itibari.models.response.BNPLFileResponseDTO;
import com.app.itibari.models.response.BannerResponseDTO;
import com.app.itibari.models.response.DuplicateReceiptResponse;
import com.app.itibari.models.response.FileResponse;
import com.app.itibari.models.response.FileResponseDTO;
import com.itextpdf.io.source.ByteArrayOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FileService {

    FileData uploadFile(MultipartFile file,
                        UUID referenceId,
                        ReferenceType referenceType,
                        String description,
                        String receipt,
                        String amount,
                        UUID documentRulePublicId,
                        String expiryDate,
                        boolean uploadedFromWeb,
                        String userName, String supplierDocumentInfo);

    FileResponse downloadFile(UUID referenceId) throws IOException;

    void swapFile(UUID publicId, MultipartFile file, String name) throws IOException;

    void deleteFile(UUID referenceId) throws IOException;

    Page<FileData> getFile(UUID referenceId, ReferenceType referenceType, String receipt,String orderNumber, Pageable pageable);

    ByteArrayOutputStream exportFiles(UUID referenceId,
                                      ReferenceType referenceType,
                                      String receipt,
                                      String orderNumber,
                                      StatementExportType exportType,
                                      String principalName);

    FileResponseDTO getImages(UUID referenceId, ReferenceType referenceType) throws IOException;

    DuplicateReceiptResponse isDuplicate(String receipt, UUID referenceId, ReferenceType referenceType);

    void deleteReceipt(UUID publicId, String comment, String name);

    void deleteReceiptByAgent(UUID publicId, String comment, String name);

    List<BNPLFileResponseDTO> getBNPLKYCDocuments(UUID customerPublicId,UUID documentRulePublicId);

    void approveFile(UUID publicId, String name);

    void rejectFile(UUID publicId, String reason, String name);

    FileData uploadMobileBannerImage(MultipartFile file, BannerRequest bannerRequest, String description, String name);

    List<BannerResponseDTO> getMobileBanner(boolean fromMobileApp, Boolean isActive);

    List<FileReceiptDTO> getCustomerReceiptFiles(String name);

    void approveOrRejectBannerImage(UUID publicId, BannerAction action, String name);

    BigDecimal getDepositedAmount(UUID routePublicId);

    void runDocumentExpiry();

    void checkIfProofFileIsSubmited(UUID publicId, ReferenceType referenceType);

    void approveRefundFile(UUID publicId, Person person);

    FileData uploadWhatsAppMedia(byte[] content, String mimeType, String fileName, UUID referenceId);
}
