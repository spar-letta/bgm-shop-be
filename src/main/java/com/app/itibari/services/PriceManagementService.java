package com.app.itibari.services;

import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.models.entities.AvcoItems;
import com.app.itibari.models.request.PriceManagementRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

public interface PriceManagementService {

    void createPriceManagement(@Valid PriceManagementRequest priceManagementRequest, String name);

    void getPriceManagementTemplate(HttpServletResponse response, String name);

    void uploadPriceManagement(MultipartFile file, String name) throws IOException;

    Page<AvcoItems> getAllAvco(String searchParam,
                               LocalDate fromDate,
                               LocalDate toDate,
                               String orderNumber,
                               UUID productPublicId,
                               TransactionType transactionType,
                               Pageable pageable);
}
