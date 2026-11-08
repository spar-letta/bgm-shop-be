package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.Quarantine;
import com.app.itibari.models.entities.QuarantineProducts;
import com.app.itibari.models.request.DisposalRequest;
import com.app.itibari.models.request.QuarantineRequest;
import com.app.itibari.models.response.DisposedQuarantineProducts;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

public interface QuarantineService {

    Quarantine createQuarantine(@Valid QuarantineRequest quarantineRequest, String name);

    void addQuarantineProducts(UUID publicId, @Valid QuarantineRequest quarantineRequest, String name);

    void removeQuarantineProducts(UUID publicId, UUID quarantineProductId, String name);

    Quarantine getQuarantineByPublicId(UUID publicId);

    Page<Quarantine> getAllQuarantines(String searchParam,
                                       UUID mainWarehousePublicId,
                                       UUID quarantineWarehousePublicId,
                                       QuarantineStatus status,
                                       TransferType transferType,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       Pageable pageable);

    Page<QuarantineProducts> getQuarantineProducts(UUID publicId, String searchParam, Pageable pageable);

    void deleteQuarantine(UUID publicId, String name);

    void approveOrRejectQuarantine(UUID publicId, QuarantineAction action, String name);

    void returnProductToMainWarehouse(UUID publicId, UUID quarantineProductId, int quantity, String name);

    void disposeQuarantineProduct(UUID publicId, UUID quarantineProductId, UUID reasonCodePublicId, ProductDisposalType disposalType, DisposalRequest disposalRequest, String name);

    Page<QuarantineProducts> getPendingDisposedQuarantineProducts(String searchParam, Pageable pageable);

    Page<DisposedQuarantineProducts> getApprovedDisposedQuarantineProducts(String searchParam, LocalDate startDate, LocalDate endDate, Pageable pageable);

    ByteArrayOutputStream runExportQuarantineDisposedProducts(String searchParam, LocalDate startDate, LocalDate endDate, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException;
}
