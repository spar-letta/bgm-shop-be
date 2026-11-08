package com.app.itibari.services;

import com.app.itibari.dataTypes.BannerAction;
import com.app.itibari.dataTypes.CustomerBnplVerificationStatus;
import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.BannerRequest;
import com.app.itibari.models.request.FileReceiptDTO;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.services.helper.ObjectStorageHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.EmailBodyCrafter;
import com.app.itibari.utils.reportHelper.ReceiptReportExport;
import com.app.itibari.utils.specification.FileDataSpecification;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final String THIRTY_DAYS = "thirty";
    private static final String FIFTEEN_DAYS = "fifteen";
    private final FileDataRepository fileDataRepository;
    private final PersonRepository personRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final EventsRepository eventsRepository;
    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final RoutePlanningRepository routePlanningRepository;
    private final DocumentRuleRepository documentRuleRepository;
    private final CustomersRepository customersRepository;
    private final BnplHelper bnplHelper;
    private final ItibariSmsService itibariSmsService;
    private final ProductReconRepository productReconRepository;
    private final SupplierRepository supplierRepository;
    private final EmailBodyCrafter emailBodyCrafter;
    private final EmailService emailService;
    private final CategoryRepository categoryRepository;
    private final ProductClassRepository productClassRepository;
    private final ProductSubClassRepository productSubClassRepository;
    private final ProductRepository productRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final ReceiptReportExport receiptReportExport;
    private final ReconciliationService reconciliationService;
    private final ObjectStorageHelper objectStorageHelper;

    @Value("${spring.cloud.gc.bucket.name}")
    private String bucketName;

    @Value("${spring.cloud.gc.storage.credentials.location}")
    private Resource resource;

    public FileServiceImpl(FileDataRepository fileDataRepository,
                           PersonRepository personRepository,
                           ReconciliationRepository reconciliationRepository,
                           EventsRepository eventsRepository,
                           DeliveryAgentsRepository deliveryAgentsRepository,
                           RoutePlanningRepository routePlanningRepository,
                           DocumentRuleRepository documentRuleRepository,
                           CustomersRepository customersRepository,
                           BnplHelper bnplHelper,
                           ItibariSmsService itibariSmsService,
                           ProductReconRepository productReconRepository,
                           SupplierRepository supplierRepository,
                           EmailBodyCrafter emailBodyCrafter,
                           EmailService emailService,
                           CategoryRepository categoryRepository,
                           ProductClassRepository productClassRepository,
                           ProductSubClassRepository productSubClassRepository,
                           ProductRepository productRepository,
                           CreditLimitItemRepository creditLimitItemRepository,
                           ReceiptReportExport receiptReportExport,
                           ReconciliationService reconciliationService,
                           ObjectStorageHelper objectStorageHelper) {

        this.fileDataRepository = fileDataRepository;
        this.personRepository = personRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.eventsRepository = eventsRepository;
        this.deliveryAgentsRepository = deliveryAgentsRepository;
        this.routePlanningRepository = routePlanningRepository;
        this.documentRuleRepository = documentRuleRepository;
        this.customersRepository = customersRepository;
        this.bnplHelper = bnplHelper;
        this.itibariSmsService = itibariSmsService;
        this.productReconRepository = productReconRepository;
        this.supplierRepository = supplierRepository;
        this.emailBodyCrafter = emailBodyCrafter;
        this.emailService = emailService;
        this.categoryRepository = categoryRepository;
        this.productClassRepository = productClassRepository;
        this.productSubClassRepository = productSubClassRepository;
        this.productRepository = productRepository;
        this.creditLimitItemRepository = creditLimitItemRepository;
        this.receiptReportExport = receiptReportExport;
        this.reconciliationService = reconciliationService;
        this.objectStorageHelper = objectStorageHelper;
    }

    @Override
    @Transactional
    public FileData uploadFile(
            MultipartFile file,
            UUID referenceId,
            ReferenceType referenceType,
            String description,
            String receipt,
            String amount,
            UUID documentRulePublicId,
            String expiryDate,
            boolean uploadedFromWeb,
            String username, String supplierDocumentInfo) {

        if (receipt != null && !receipt.isEmpty()) {
            // unique receipt number across the system
           // boolean receiptAlreadyUsed = fileDataRepository.existsByReceiptIgnoreCaseAndReferenceTypeAndDeletedIsFalse(receipt, referenceType.name());
            boolean receiptAlreadyUsed = fileDataRepository.existsByReceiptIgnoreCaseAndDeletedIsFalse(receipt);

            if (receiptAlreadyUsed) {
                throw new BadRequestRestApiException()
                        .developerMessage("Receipt already used for another file")
                        .userMessage("Sorry!. The receipt already exists for another file. Please provide a unique receipt for this file.");
            }
        }

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(username);

        if (referenceType != null && referenceType.equals(ReferenceType.RECEIPT)) {

            if (receipt == null || receipt.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Receipt cannot be null or empty for reference type RECEIPT")
                        .userMessage("Receipt cannot be null or empty for this reference type.");
            }

            List<FileData> existingFiles = fileDataRepository.findAllByReceiptIgnoreCaseAndReferenceTypeAndDeletedIsFalse(
                    receipt, referenceType.name());

            if (!existingFiles.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("File with the same receipt already exists")
                        .userMessage("A file with the same receipt already exists for this reference.");
            }
        }

        if (referenceType == ReferenceType.CREDIT_PAYMENT) {
            if (amount == null || amount.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Amount cannot be null or empty for reference type CREDIT_PAYMENT")
                        .userMessage("Amount cannot be null or empty for this reference type.");
            }

            if (description == null || ObjectUtils.isEmpty(description)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Description is required for credit payment files")
                        .userMessage("Description is required for credit payment files");
            }
            if (description.trim().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Description cannot be blank for credit payment files")
                        .userMessage("Description cannot be blank for credit payment files");

            }
            // ORD100950
            // starts with ORD
            if (!description.trim().toUpperCase().startsWith("ORD")) {
                throw new BadRequestRestApiException()
                        .developerMessage("Description must start with 'ORD' for credit payment files")
                        .userMessage("Sorry!. Description should be a valid order Number");
            }

            if (receipt == null || receipt.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Receipt is required for credit payment files")
                        .userMessage("Receipt is required for credit payment files");
            }

            if (receipt.trim().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Receipt cannot be blank for credit payment files")
                        .userMessage("Receipt cannot be blank for credit payment files");
            }

            bnplHelper.validateAmount(amount, referenceId, description);
        }

        DocumentRule rule = null;
        LocalDate expiry = null;

        if (documentRulePublicId != null) {
            validateCustomer(referenceId, referenceType);
            Optional<DocumentRule> optionalDocumentRule = documentRuleRepository.findByPublicIdAndDeletedIsFalse(documentRulePublicId);
            if (optionalDocumentRule.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Document Rule not found")
                        .userMessage("Document Rule not found");
            }
            DocumentRule documentRule = optionalDocumentRule.get();

            if (documentRule.isDocumentExpiryCheck() && (expiryDate == null || expiryDate.isEmpty())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Expiry date is required for this document")
                        .userMessage("Sorry!. Document expiry date is required for this document. Please provide a valid expiry date.");
            }


            if (documentRule.isDocumentExpiryCheck()) {
                try {
                    expiry = DateFormatter.parseDate(expiryDate);
                } catch (Exception e) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Invalid expiry date format")
                            .userMessage("Sorry!. The provided expiry date format is invalid. Please provide a valid date in the format YYYY-MM-DD.");
                }
            }

            rule = documentRule;


        }

        if (referenceType == ReferenceType.DELIVERY_AGENT_DEBIT_NOTE && amount != null && !amount.isBlank()) {
            BigDecimal payment;
            try {
                payment = new BigDecimal(amount);
            } catch (NumberFormatException e) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid debit note payment amount")
                        .userMessage("Please provide a valid debit note payment amount");
            }
            // The payment and file metadata share a transaction; failed uploads roll back both.
            reconciliationService.updateDebitNote(referenceId, payment, username);
        }

        try {
            FileData fileData = new FileData();
            Tika tika = new Tika();
            try {
                fileData.setFileType(tika.detect(file.getBytes()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to detect file type", e);
            }
            fileData.setReferenceId(referenceId.toString());
            fileData.setFileSize(file.getSize());
            fileData.setFileName(file.getName());
            fileData.setReferenceType(String.valueOf(referenceType));
            fileData.setDescription(description);
            optionalPerson.ifPresent(fileData::setCreatedBy);
            fileData.setCreationTime(LocalDateTime.now());
            fileData.setDocumentRule(rule);
            fileData.setExpiry(expiry);
            fileData.setAmount(amount != null && !amount.isBlank() ? new BigDecimal(amount) : BigDecimal.ZERO);
            fileData.setSupplierDocumentInfo(supplierDocumentInfo);

            if (receipt != null && !receipt.isEmpty()) {
                fileData.setReceipt(receipt);
                fileData.setAmount(amount != null && !amount.isBlank() ? new BigDecimal(amount) : BigDecimal.ZERO);
            }

            // Save FileData with publicId but without filePath initially
            FileData savedFileData = fileDataRepository.save(fileData);

            try {
                String storedPath = objectStorageHelper.store(
                        savedFileData.getPublicId(), fileData.getFileType(), file.getBytes());
                savedFileData.setFilePath(storedPath);
            } catch (BadRequestRestApiException e) {
                fileDataRepository.delete(savedFileData);
                throw e;
            } catch (IOException e) {
                log.error("Error while reading upload bytes", e);
                fileDataRepository.delete(savedFileData);
                throw new BadRequestRestApiException()
                        .developerMessage("Error while uploading file to storage")
                        .userMessage("Sorry. We are unable to upload the file. Please try again later.");
            }

            // Save the updated FileData with filePath
            FileData fileData1 = fileDataRepository.save(savedFileData);

            if (fileData1.getReferenceType().equalsIgnoreCase(ReferenceType.CREDIT_PAYMENT.name())) {
                addAuditEvent(fileData1, false);

            }

            if (fileData1.getReferenceType().equalsIgnoreCase(ReferenceType.RECEIPT.name())) {
                Optional<Reconciliation> reconciliationOptional = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(referenceId.toString());

                if (reconciliationOptional.isPresent()) {
                    Reconciliation reconciliation = reconciliationOptional.get();
                    reconciliation.addDepositAmount(fileData1.getAmount());
                    reconciliation.setUpdateDate(LocalDateTime.now());
                    reconciliation.setUpdatedBy(optionalPerson.orElse(null));
                    Reconciliation reconciliation1 = reconciliationRepository.save(reconciliation);

                    Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(reconciliation1.getDeliveryId());

                    if (optionalRoutePlanning.isPresent()) {
                        RoutePlanning routePlanning = optionalRoutePlanning.get();
                        BigDecimal credit = reconciliation1.getCreditAmount() != null ? reconciliation1.getCreditAmount() : BigDecimal.ZERO;
                        routePlanning.setBalance(reconciliation1.getDeliveryAmount().subtract(reconciliation1.getDepositAmount().add(credit)));
                        routePlanningRepository.save(routePlanning);
                    }
                } else {
                    log.info("Reconciliation not found for referenceId: {}", referenceId);
                }
            }

            log.info("======file description is {}====", fileData1.getDescription());


            return fileData1;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process file", e);
        }
    }

    private void addAuditEvent(FileData fileData1, boolean delete) {
        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(fileData1.getDescription());
        optionalCreditLimitItem.ifPresent(creditLimitItem -> {
            String description = delete ? String.format("Credit payment receipt deleted with receipt number %s for order %s", fileData1.getReceipt(), fileData1.getDescription())
                    : String.format("Credit payment receipt uploaded with receipt number %s for order %s", fileData1.getReceipt(), fileData1.getDescription());

            String eventAction = delete ? "CREDIT-PAYMENT-RECEIPT-DELETION" : "CREDIT-PAYMENT-RECEIPT-UPLOAD";
            String eventType = delete ? "Credit Payment Receipt Deletion" : "Credit Payment Receipt Upload";

            eventsRepository.save(new Events(creditLimitItem.getPublicId().toString(), eventType, LocalDateTime.now(),
                    description, eventAction, fileData1.getCreatedBy()));
        });
    }

    private void validateCustomer(UUID referenceId, ReferenceType referenceType) {
        if (referenceType == ReferenceType.KYC_DOCUMENT) {
            Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(referenceId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Customer not found for the provided reference ID")
                            .userMessage("Customer not found for the provided reference ID"));
//            if (customer.getLimitedOfferEligibility() == null || !customer.getLimitedOfferEligibility()) {
//                throw new BadRequestRestApiExcept
//                ion()
//                        .developerMessage("Customer is not eligible for uploading this document")
//                        .userMessage("Sorry!. You are not eligible for uploading KYC document.");
//            }
        }
    }

    @Override
    public FileResponse downloadFile(UUID publicId) throws IOException {

        FileData fileData = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElse(null);

        if (fileData != null) {
            Optional<byte[]> content = objectStorageHelper.load(publicId);
            if (content.isPresent()) {
                FileResponse response = new FileResponse(content.get(), fileData.getFileType());
                objectStorageHelper.signedUrl(publicId, 7, TimeUnit.DAYS).ifPresent(response::setUrl);
                return response;
            }
        }
        return null;
    }

    @Override
    public void swapFile(UUID publicId, MultipartFile file, String name) throws IOException {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        FileData fileData = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("File not found")
                        .userMessage("File not found"));

        String storedPath = objectStorageHelper.store(fileData.getPublicId(), fileData.getFileType(), file.getBytes());
        fileData.setFileName(file.getName());
        fileData.setFilePath(storedPath);
        fileData.setFileSize(file.getSize());
        optionalPerson.ifPresent(fileData::setCreatedBy);
        fileDataRepository.save(fileData);
    }

    @Override
    public void deleteFile(UUID publicId) throws IOException {
        FileData fileData = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("File not found")
                        .userMessage("File not found"));

        objectStorageHelper.delete(fileData.getPublicId());
        fileDataRepository.delete(fileData);

        if (fileData.getReferenceType().equalsIgnoreCase(ReferenceType.CREDIT_PAYMENT.name())) {
            addAuditEvent(fileData, true);
        }
    }

    @Override
    public Page<FileData> getFile(UUID referenceId, ReferenceType referenceType, String receipt, String orderNumber, Pageable pageable) {

        Specification<FileData> spec = Specification
                .where(FileDataSpecification.isNotDeleted())
                .and(FileDataSpecification.hasReferenceId(referenceId.toString()))
                .and(FileDataSpecification.hasReferenceType(referenceType))
                .and(FileDataSpecification.hasReceipt(receipt))
                .and(FileDataSpecification.hasDescription(orderNumber));

        return fileDataRepository.findAll(spec, pageable);
    }

    @Override
    public ByteArrayOutputStream exportFiles(UUID referenceId,
                                             ReferenceType referenceType,
                                             String receipt,
                                             String orderNumber,
                                             StatementExportType exportType,
                                             String principalName) {
        if (exportType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("exportType is required")
                    .userMessage("Export type is required");
        }

        Person person = personRepository.findByEmailAndDeletedIsFalse(principalName)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        Sort sort = Sort.by(Sort.Direction.DESC, "creationTime");
        Pageable exportPageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        List<FileData> files = getFile(referenceId, referenceType, receipt, orderNumber, exportPageable).getContent();

        String deliveryAgent = "";
        String deliveryRoute = "";
        Optional<Reconciliation> reconciliation =
                reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(referenceId.toString());
        if (reconciliation.isPresent()) {
            deliveryAgent = reconciliation.get().getDeliveryAgent();
            deliveryRoute = reconciliation.get().getDeliveryId();
        } else {
            Optional<RoutePlanning> routePlanning =
                    routePlanningRepository.findByPublicIdAndDeletedIsFalse(referenceId);
            if (routePlanning.isPresent()) {
                deliveryAgent = routePlanning.get().getDeliveryAgentName();
                deliveryRoute = routePlanning.get().getRouteId();
            }
        }

        if (exportType == StatementExportType.EXCEL) {
            return receiptReportExport.generateExcel(files, person.getFullName(), deliveryAgent, deliveryRoute);
        }
        if (exportType == StatementExportType.PDF) {
            return receiptReportExport.generatePdf(files, person.getFullName(), deliveryAgent, deliveryRoute);
        }
        throw new BadRequestRestApiException()
                .developerMessage("Invalid export type")
                .userMessage("Invalid export type");
    }

    @Override
    public FileResponseDTO getImages(UUID referenceId, ReferenceType referenceType) throws IOException {

        List<FileData> fileDataList = fileDataRepository
                .findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(
                        referenceId.toString(),
                        referenceType.name(), Pageable.unpaged())
                .getContent();

        if (fileDataList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }

        // get latest file created
        FileData fileData = fileDataList.stream()
                .max(Comparator.comparing(BaseEntity::getCreationTime))
                .orElse(null);

        Storage storage = StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                .build()
                .getService();

        Blob blob = storage.get(bucketName, fileData.getPublicId().toString());
        if (blob != null && blob.exists()) {

            String signedUrl = storage.signUrl(
                    BlobInfo.newBuilder(blob.getBlobId()).build(),
                    7,
                    TimeUnit.DAYS,
                    Storage.SignUrlOption.withV4Signature()
            ).toString();

            return new FileResponseDTO(signedUrl, fileData.getFileType(), fileData.getDescription(), fileData.getPublicId(), true);
        }
        return null;
    }

    @Override
    public DuplicateReceiptResponse isDuplicate(String receipt, UUID referenceId, ReferenceType referenceType) {

        List<FileData> existingFiles = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndReceiptIgnoreCaseAndDeletedIsFalse(
                referenceId.toString(), referenceType.name(), receipt);

        if (!existingFiles.isEmpty()) {
            return new DuplicateReceiptResponse("A file with the same receipt already exists for this reference.", true);
        } else {
            return new DuplicateReceiptResponse("No duplicate found for the given receipt.", false);
        }
    }

    @Override
    public void deleteReceipt(UUID publicId, String comment, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (comment == null || comment.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Comment cannot be null or empty")
                    .userMessage("Comment cannot be null or empty");
        }

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Person not found");
        }
        Optional<FileData> fileDataOptional = fileDataRepository.findByPublicIdAndReferenceTypeIgnoreCaseAndDeletedIsFalse(publicId,
                ReferenceType.RECEIPT.name());

        if (fileDataOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }

        FileData fileData = fileDataOptional.get();

        try {
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                    .build().getService();

            BlobId blobId = BlobId.of(bucketName, fileData.getPublicId().toString());
            storage.delete(blobId);

            fileData.setDeleted(true);
            fileData.setUpdatedBy(optionalPerson.get());
            fileData.setUpdateDate(LocalDateTime.now());
            FileData saved = fileDataRepository.save(fileData);

            Optional<Reconciliation> reconciliationOptional = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(fileData.getReferenceId());
            if (reconciliationOptional.isPresent()) {
                Reconciliation reconciliation = reconciliationOptional.get();
                reconciliation.setDepositAmount(reconciliation.getDepositAmount().subtract(saved.getAmount()));
                reconciliation.setUpdateDate(LocalDateTime.now());
                reconciliation.setUpdatedBy(optionalPerson.get());
                Reconciliation recon = reconciliationRepository.save(reconciliation);

                eventsRepository.save(new Events(recon.getPublicId().toString(),
                        "Recon Receipt Delete",
                        LocalDateTime.now(),
                        comment,
                        "Receipt delete",
                        optionalPerson.get()));

                Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(recon.getDeliveryId());

                if (optionalRoutePlanning.isPresent()) {
                    RoutePlanning routePlanning = optionalRoutePlanning.get();
                    BigDecimal amount = recon.getDeliveryAmount().subtract(recon.getDepositAmount());
                    routePlanning.setBalance(amount);
                    routePlanningRepository.save(routePlanning);
                }
            } else {
                log.info("Reconciliation not found for referenceId: {}", fileData.getReferenceId());
            }
        } catch (IOException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Error while deleting file from storage")
                    .userMessage("Sorry. We are unable to delete the file. Please try again later.");
        }
    }

    @Override
    public void deleteReceiptByAgent(UUID publicId, String comment, String name) {

        Optional<DeliveryAgents> optionalAgent = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (comment == null || comment.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Comment cannot be null or empty")
                    .userMessage("Comment cannot be null or empty");
        }

        if (optionalAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent not found")
                    .userMessage("Delivery Agent not found");
        }
        Optional<FileData> fileDataOptional = fileDataRepository.findByPublicIdAndReferenceTypeIgnoreCaseAndDeletedIsFalse(publicId,
                ReferenceType.RECEIPT.name());

        if (fileDataOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }

        FileData fileData = fileDataOptional.get();

        try {
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                    .build().getService();

            BlobId blobId = BlobId.of(bucketName, fileData.getPublicId().toString());
            storage.delete(blobId);

            fileData.setDeleted(true);
            fileData.setUpdateDate(LocalDateTime.now());
            FileData saved = fileDataRepository.save(fileData);

            Optional<Reconciliation> reconciliationOptional = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(fileData.getReferenceId());
            if (reconciliationOptional.isPresent()) {
                Reconciliation reconciliation = reconciliationOptional.get();
                reconciliation.setDepositAmount(reconciliation.getDepositAmount().subtract(saved.getAmount()));
                reconciliation.setUpdateDate(LocalDateTime.now());
                Reconciliation recon = reconciliationRepository.save(reconciliation);

                String eventType = String.format("Recon Receipt Delete By Agent %s", optionalAgent.get().getFullName());

                eventsRepository.save(new Events(recon.getPublicId().toString(),
                        eventType,
                        LocalDateTime.now(),
                        comment,
                        "Receipt delete",
                        null));

                Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(recon.getDeliveryId());

                if (optionalRoutePlanning.isPresent()) {
                    RoutePlanning routePlanning = optionalRoutePlanning.get();
                    BigDecimal amount = recon.getDeliveryAmount().subtract(recon.getDepositAmount());
                    routePlanning.setBalance(amount);
                    routePlanningRepository.save(routePlanning);
                }
            } else {
                log.info("Reconciliation not found for");
            }
        } catch (IOException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Error while deleting file from storage")
                    .userMessage("Sorry. We are unable to delete the file. Please try again later.");
        }
    }

    @Override
    public List<BNPLFileResponseDTO> getBNPLKYCDocuments(UUID customerPublicId, UUID documentRulePublicId) {
        Optional<DocumentRule> optionalDocumentRule = documentRuleRepository.findByPublicIdAndDeletedIsFalse(documentRulePublicId);
        if (optionalDocumentRule.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Document Rule not found")
                    .userMessage("Document Rule not found");
        }
        DocumentRule documentRule = optionalDocumentRule.get();

        Optional<Customers> optionalCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found")
                    .userMessage("Customer not found");
        }

        Customers customer = optionalCustomer.get();

        List<FileData> fileDataList = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDocumentRuleAndDeletedIsFalse(
                customer.getPublicId().toString(),
                ReferenceType.KYC_DOCUMENT.name(),
                documentRule);

        if (fileDataList.isEmpty()) {
            return null;
        }

        return fileDataList.stream().map(fileData -> {
            String fileUrl = null;
            try {
                Storage storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                        .build()
                        .getService();

                Blob blob = storage.get(bucketName, fileData.getPublicId().toString());
                if (blob != null && blob.exists()) {

                    fileUrl = storage.signUrl(
                            BlobInfo.newBuilder(blob.getBlobId()).build(),
                            7,
                            TimeUnit.DAYS,
                            Storage.SignUrlOption.withV4Signature()
                    ).toString();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BNPLFileResponseDTO bnplFileResponseDTO = new BNPLFileResponseDTO();
            bnplFileResponseDTO.setUrl(fileUrl);
            bnplFileResponseDTO.setContentType(fileData.getFileType());
            bnplFileResponseDTO.setDescription(documentRule.getDocumentDescription());
            bnplFileResponseDTO.setDocumentName(documentRule.getDocumentName());
            bnplFileResponseDTO.setDocumentType(documentRule.getDocumentType().name());
            bnplFileResponseDTO.setExpiryDate(fileData.getExpiry());
            bnplFileResponseDTO.setExpired(fileData.isExpired());

            if (!fileData.isApproved() && fileData.getRejectionReason() != null && !fileData.getRejectionReason().isEmpty()) {
                bnplFileResponseDTO.setRejectionReason(fileData.getRejectionReason());
                bnplFileResponseDTO.setRejected(true);
            }
            return bnplFileResponseDTO;
        }).toList();
    }

    @Override
    public void approveFile(UUID publicId, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Person not found");
        }

        Person approver = optionalPerson.get();
        Optional<FileData> fileDataOptional = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (fileDataOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }
        FileData fileData = fileDataOptional.get();
        fileData.setUpdatedBy(approver);
        fileData.setUpdateDate(LocalDateTime.now());
        fileData.setApproved(true);
        fileDataRepository.save(fileData);
    }

    @Override
    public void rejectFile(UUID publicId, String reason, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Person not found");
        }

        Person approver = optionalPerson.get();
        Optional<FileData> fileDataOptional = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (fileDataOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }
        FileData fileData = fileDataOptional.get();
        fileData.setUpdatedBy(approver);
        fileData.setUpdateDate(LocalDateTime.now());
        fileData.setApproved(false);
        fileData.setRejectionReason(reason);
        fileDataRepository.save(fileData);

        if (fileData.getReferenceType().equalsIgnoreCase(ReferenceType.KYC_DOCUMENT.name())) {
            sendNotificationToCustomer(fileData);
        }
    }

    private void sendNotificationToCustomer(FileData fileData) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(
                        UUID.fromString(fileData.getReferenceId()))
                .orElse(null);

        if (customer != null) {
            String message = String.format("Dear %s, your document '%s' has been rejected. Reason: %s. Please upload a valid document.",
                    customer.getName(),
                    fileData.getDocumentRule() != null ? fileData.getDocumentRule().getDocumentName() : "Document",
                    fileData.getRejectionReason());

            log.info("Sending notification to customer {}: {}", customer.getPhoneNumber(), message);

            itibariSmsService.send(customer.getPhoneNumber(), message);

            if (customer.isSelfOnboarded()) {
                customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.DOCUMENTS_REJECTED);
                customersRepository.save(customer);
            }
        }
    }

    @Override
    public FileData uploadWhatsAppMedia(byte[] content, String mimeType, String fileName, UUID referenceId) {
        try {
            FileData fileData = new FileData();
            Tika tika = new Tika();
            String detectedType = mimeType;
            try {
                detectedType = tika.detect(content);
            } catch (Exception e) {
                log.warn("Could not detect WhatsApp media type, using provided mime type {}", mimeType);
            }
            fileData.setFileType(detectedType != null ? detectedType : mimeType);
            fileData.setFileSize((long) content.length);
            fileData.setFileName(fileName);
            fileData.setReferenceId(referenceId.toString());
            fileData.setReferenceType(ReferenceType.WHATSAPP_MEDIA.name());
            fileData.setDescription("WhatsApp inbound media");
            fileData.setCreationTime(LocalDateTime.now());

            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                    .build().getService();

            FileData savedFileData = fileDataRepository.save(fileData);

            BlobId blobId = BlobId.of(bucketName, savedFileData.getPublicId().toString());
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(fileData.getFileType()).build();
            try {
                Blob blob = storage.create(blobInfo, content);
                savedFileData.setFilePath(blob.getMediaLink());
            } catch (StorageException e) {
                log.error("Error while uploading WhatsApp media to storage", e);
                fileDataRepository.delete(savedFileData);
                throw new BadRequestRestApiException()
                        .developerMessage("Error while uploading WhatsApp media to storage")
                        .userMessage("Sorry. We are unable to store the WhatsApp media.");
            }

            return fileDataRepository.save(savedFileData);
        } catch (IOException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Error while uploading WhatsApp media: " + e.getMessage())
                    .userMessage("Sorry. We are unable to store the WhatsApp media.");
        }
    }

    @Override
    public FileData uploadMobileBannerImage(MultipartFile file, BannerRequest bannerRequest, String description, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (bannerRequest != null) {
            validateBannerRequest(bannerRequest);
        }

        try {
            FileData fileData = new FileData();
            Tika tika = new Tika();
            try {
                fileData.setFileType(tika.detect(file.getBytes()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to detect file type", e);
            }
            fileData.setFileSize(file.getSize());
            fileData.setFileName(file.getName());
            fileData.setReferenceType(String.valueOf(ReferenceType.MOBILE_BANNER));
            fileData.setDescription(description);
            optionalPerson.ifPresent(fileData::setCreatedBy);
            fileData.setCreationTime(LocalDateTime.now());


            if (bannerRequest != null) {
                fileData.setCategoryPublicId(String.valueOf(bannerRequest.getCategoryPublicId()));
                fileData.setClassPublicId(String.valueOf(bannerRequest.getClassPublicId()));
                fileData.setSubClassPublicId(String.valueOf(bannerRequest.getSubClassPublicId()));
                fileData.setProductPublicIds(bannerRequest.getHasProducts() ? String.valueOf(bannerRequest.getProducts()) : null);
                fileData.setBrand(bannerRequest.getBrand());

            }

            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                    .build().getService();

            FileData savedFileData = fileDataRepository.save(fileData);

            BlobId blobId = BlobId.of(bucketName, savedFileData.getPublicId().toString());
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(fileData.getFileType()).build();
            try {
                Blob blob = storage.create(blobInfo, file.getBytes());
                savedFileData.setFilePath(blob.getMediaLink());
                savedFileData.setActiveFile(true);
            } catch (StorageException e) {
                log.error("Error while uploading file to storage", e);
                fileDataRepository.delete(savedFileData);
                throw new BadRequestRestApiException()
                        .developerMessage("Error while uploading file to storage")
                        .userMessage("Sorry. We are unable to upload the file. Please try again later.");
            }

            return fileDataRepository.save(savedFileData);

        } catch (IOException e) {
            throw new RuntimeException("Failed to process file", e);
        }
    }

    @Override
    public List<BannerResponseDTO> getMobileBanner(boolean fromMobileApp, Boolean isActive) {
        List<FileData> activeBanner;
        if (fromMobileApp) {
            activeBanner = fileDataRepository
                    .findByReferenceTypeAndActiveFileIsTrueAndDeletedIsFalse(ReferenceType.MOBILE_BANNER.name());
        } else {
            activeBanner = fileDataRepository.findByReferenceTypeAndActiveFileAndDeletedIsFalse(ReferenceType.MOBILE_BANNER.name(), isActive);
        }

        return activeBanner.stream().map(banner -> {
            try {
                Storage storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                        .build()
                        .getService();

                Blob blob = storage.get(bucketName, banner.getPublicId().toString());
                if (blob != null && blob.exists()) {

                    String signedUrl = storage.signUrl(
                            BlobInfo.newBuilder(blob.getBlobId()).build(),
                            7,
                            TimeUnit.DAYS,
                            Storage.SignUrlOption.withV4Signature()
                    ).toString();

                    BannerResponseDTO dto = new BannerResponseDTO();
                    dto.setUrl(signedUrl);
                    dto.setContentType(banner.getFileType());
                    dto.setDescription(banner.getDescription());
                    dto.setPublicId(banner.getPublicId());
                    dto.setActive(banner.isActiveFile());
                    dto.setProductSubClass(banner.getSubClassPublicId());
                    dto.setProductClass(banner.getClassPublicId());
                    dto.setCategory(banner.getCategoryPublicId());
                    dto.setProducts(getProductInfo(banner.getProductPublicIds()));
                    dto.setCategoryName(getCategoryName(banner.getCategoryPublicId()));
                    dto.setProductClassName(getProductClassName(banner.getClassPublicId()));
                    dto.setProductSubClassName(getSubClassName(banner.getSubClassPublicId()));
                    dto.setBrand(banner.getBrand());

                    return dto;
                } else {
                    log.info("No active file found for banner id: {}", banner.getPublicId());
                }
            } catch (IOException e) {
                log.error("Error while reading file from storage", e);
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    private String getSubClassName(String subClassPublicId) {
        if (subClassPublicId == null || "null".equalsIgnoreCase(subClassPublicId)) {
            return null;
        }
        Optional<ProductSubClass> optionalProductSubClass = productSubClassRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(subClassPublicId));
        if (optionalProductSubClass.isPresent()) {
            ProductSubClass productSubClass = optionalProductSubClass.get();
            return productSubClass.getName();
        }
        return null;
    }

    private String getProductClassName(String classPublicId) {
        if (classPublicId == null || "null".equalsIgnoreCase(classPublicId)) {
            return null;
        }

        Optional<ProductClass> optionalProductClass = productClassRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(classPublicId));

        if (optionalProductClass.isPresent()) {
            ProductClass productClass = optionalProductClass.get();
            return productClass.getName();
        }
        return null;
    }

    private String getCategoryName(String categoryPublicId) {
        if (categoryPublicId == null || "null".equalsIgnoreCase(categoryPublicId)) {
            return null;
        }

        Optional<Category> optionalCategory = categoryRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(categoryPublicId));

        return optionalCategory.map(Category::getName).orElse(null);
    }


    @Override
    public List<FileReceiptDTO> getCustomerReceiptFiles(String name) {
        Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(name);
        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found")
                    .userMessage("Customer not found");
        }
        Customers customer = optionalCustomer.get();

        List<FileData> fileDataList = fileDataRepository
                .findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(
                        customer.getPublicId().toString(),
                        ReferenceType.CREDIT_PAYMENT.name(), Pageable.unpaged())
                .getContent();

        List<FileReceiptDTO> receiptDTOList = new ArrayList<>();

        for (FileData fileData : fileDataList) {

            try {
                FileReceiptDTO receiptDTO = new FileReceiptDTO();
                receiptDTO.setReceiptNumber(fileData.getReceipt());
                receiptDTO.setOrderNumber(fileData.getDescription());
                receiptDTO.setRejectionReason(fileData.getRejectionReason());
                receiptDTO.setContentType(fileData.getFileType());
                receiptDTO.setStatus(setStatus(fileData));
                receiptDTO.setAmount(fileData.getAmount());
                receiptDTO.setDate(fileData.getCreationTime().toLocalDate().toString());
                Storage storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(resource.getInputStream()))
                        .build()
                        .getService();

                Blob blob = storage.get(bucketName, fileData.getPublicId().toString());
                if (blob != null && blob.exists()) {

                    String signedUrl = storage.signUrl(
                            BlobInfo.newBuilder(blob.getBlobId()).build(),
                            7,
                            TimeUnit.DAYS,
                            Storage.SignUrlOption.withV4Signature()
                    ).toString();

                    receiptDTO.setUrl(signedUrl);
                    receiptDTOList.add(receiptDTO);
                } else {
                    log.info("No actived file found for banner id: {}", fileData.getPublicId());
                }
            } catch (IOException e) {
                log.error("Error while reading file from storage", e);
            }
        }

        return receiptDTOList;
    }

    private String setStatus(FileData fileData) {
        if (fileData.isApproved()) {
            return "APPROVED";
        } else if (fileData.getRejectionReason() != null && !fileData.getRejectionReason().isEmpty()) {
            return "REJECTED";
        } else {
            return "PENDING APPROVAL";
        }
    }

    @Override
    public void approveOrRejectBannerImage(UUID publicId, BannerAction action, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Person not found");
        }

        Person approver = optionalPerson.get();
        Optional<FileData> fileDataOptional = fileDataRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (fileDataOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("File not found")
                    .userMessage("File not found");
        }
        FileData fileData = fileDataOptional.get();

        if (action == BannerAction.ACTIVATE) {
            fileData.setActiveFile(true);
        } else if (action == BannerAction.DEACTIVATE) {
            fileData.setActiveFile(false);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid action")
                    .userMessage("Invalid action");
        }

        fileData.setUpdatedBy(approver);
        fileData.setUpdateDate(LocalDateTime.now());
        fileDataRepository.save(fileData);
    }

    @Override
    public BigDecimal getDepositedAmount(UUID routePublicId) {


        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByPublicIdAndDeletedIsFalse(routePublicId);
        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route planning not found")
                    .userMessage("Route planning not found");
        }
        RoutePlanning routePlanning = optionalRoutePlanning.get();
        Reconciliation reconciliation = reconciliationRepository.findByDeliveryIdIgnoreCase(routePlanning.getRouteId());
        if (reconciliation == null) {
            return BigDecimal.ZERO;
        }

        List<FileData> files = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(
                reconciliation.getRoutePublicId(),
                ReferenceType.RECEIPT.name(), Pageable.unpaged()).getContent();

        BigDecimal depositedAmount = BigDecimal.ZERO;

        for (FileData fileData : files) {
            if (fileData.getAmount() != null) {
                depositedAmount = depositedAmount.add(fileData.getAmount());
            }
        }

        return depositedAmount;
    }

    @Override
    public void runDocumentExpiry() {
        runDocumentExpiryAlerts();
    }

    public void runDocumentExpiryAlerts() {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        Instant nowUtc = Instant.now();
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        LocalDate alert15DayToExpiry = nairobiDate.plusDays(15);
        LocalDate alert30DayToExpiry = nairobiDate.plusDays(30);

        Map<String, LocalDate> dates = Map.of(FIFTEEN_DAYS, alert15DayToExpiry, THIRTY_DAYS, alert30DayToExpiry);

        dates.forEach((key, value) -> {
            if (key.equals(FIFTEEN_DAYS)) {
                List<FileData> fileDataList = fileDataRepository.findAllFifteenDayByDocumentRuleAndDeletedIsFalse(alert15DayToExpiry);
                if (!fileDataList.isEmpty()) {
                    for (FileData fileData : fileDataList) {
                        Suppliers suppliers = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(fileData.getReferenceId())).orElse(null);
                        if (suppliers != null) {
                            String fileName = fileData.getDocumentRule() != null ? fileData.getDocumentRule().getDocumentName() : "--";
                            sendAlertToPerson(fileName, suppliers, FIFTEEN_DAYS);
                            fileData.setFifteenDayProcessed(true);
                            fileDataRepository.save(fileData);
                        }
                    }
                }
            }
            if (key.equals(THIRTY_DAYS)) {
                List<FileData> fileDataList = fileDataRepository.findAllThirtyDayByDocumentRuleAndDeletedIsFalse(alert30DayToExpiry);
                if (!fileDataList.isEmpty()) {
                    for (FileData fileData : fileDataList) {
                        Suppliers suppliers = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(fileData.getReferenceId())).orElse(null);
                        if (suppliers != null) {
                            String fileName = fileData.getDocumentRule() != null ? fileData.getDocumentRule().getDocumentName() : "--";
                            sendAlertToPerson(fileName, suppliers, THIRTY_DAYS);
                            fileData.setThirtyDayProcessed(true);
                            fileDataRepository.save(fileData);
                        }
                    }
                }
            }
        });
    }

    private void sendAlertToPerson(String fileName, Suppliers suppliers, String days) {

        String emailBody = emailBodyCrafter.craftDocumentExpiryEmail(suppliers.getSupplierName(), fileName, days);
        emailService.sendMessage(suppliers.getEmail(), "Document Expiry", null, emailBody, Collections.EMPTY_LIST);
    }

    @Override
    public void checkIfProofFileIsSubmited(UUID publicId, ReferenceType referenceType) {
        List<FileData> fileDataList = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(
                        publicId.toString(),
                        referenceType.name(), Pageable.unpaged())
                .getContent();

        if (fileDataList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Required proof document not submitted")
                    .userMessage("Required proof document not submitted. Please upload the required document to proceed.");
        }
    }

    @Override
    public void approveRefundFile(UUID publicId, Person person) {
        List<FileData> fileDataList = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(
                        publicId.toString(),
                        ReferenceType.PURCHASE_ORDER_REFUND.name(), Pageable.unpaged())
                .getContent();

        if (fileDataList.isEmpty()) {
            return;
        }

        fileDataList.forEach(fileData -> {
            if (!fileData.isApproved()) {
                fileData.setApproved(true);
                fileData.setUpdatedBy(person);
                fileData.setUpdateDate(LocalDateTime.now());
                fileDataRepository.save(fileData);
            }
        });
    }

    private void validateBannerRequest(BannerRequest bannerRequest) {

        if (bannerRequest.getHasProducts() && !bannerRequest.getProducts().isEmpty()) {
            bannerRequest.getProducts().forEach(bannerProduct -> {
                productRepository.findByPublicIdAndDeletedIsFalse(bannerProduct).orElseThrow(
                        () -> new BadRequestRestApiException()
                                .userMessage("Sorry!. Product not found.")
                                .developerMessage("Sorry!. Product not found.")
                );
            });
        }

        if (bannerRequest.getCategoryPublicId() != null) {

            categoryRepository.findByPublicIdAndDeletedIsFalse(bannerRequest.getCategoryPublicId()).orElseThrow(
                    () -> new BadRequestRestApiException()
                            .userMessage("Sorry!. Category not found.")
                            .developerMessage("Sorry!. Category not found.")
            );

        }

        if (bannerRequest.getSubClassPublicId() != null) {
            productSubClassRepository.findByPublicIdAndDeletedIsFalse(bannerRequest.getSubClassPublicId()).orElseThrow(
                    () -> new BadRequestRestApiException()
                            .userMessage("Sorry!. Sub-class not found.")
                            .developerMessage("Sorry!. Sub-class not found.")
            );
        }

        if (bannerRequest.getClassPublicId() != null) {
            productClassRepository.findByPublicIdAndDeletedIsFalse(bannerRequest.getClassPublicId()).orElseThrow(
                    () -> new BadRequestRestApiException()
                            .userMessage("Sorry!. Class not found.")
                            .developerMessage("Sorry!. Class not found.")
            );
        }

    }

    private List<ProductInfo> getProductInfo(String productPublicIds) {

        if (productPublicIds == null || productPublicIds.isEmpty()) {
            return null;
        }

        String cleaned = productPublicIds.replaceAll("[\\[\\]\\s]", "");
        List<String> productIds = Arrays.stream(cleaned.split(","))
                .filter(s -> !s.isBlank())
                .toList();
        List<ProductInfo> productInfoList = new ArrayList<>();

        if (productIds.isEmpty()) {
            return null;
        }

        productIds.forEach(productId -> {
            ProductInfo productInfo = new ProductInfo();
            var product = productRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productId)).orElse(null);

            if (product != null) {

                productInfo.setProductName(product.getProductName());
                productInfo.setSku(product.getSku());
                productInfo.setPublicId(product.getPublicId());
                productInfoList.add(productInfo);
            }
        });

        return productInfoList;
    }
}
