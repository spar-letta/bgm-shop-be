package com.app.itibari.services;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.app.itibari.models.entities.CustomerAppDownloadStats;
import com.app.itibari.models.request.CustomerAppDownloadStatRequest;
import com.app.itibari.repository.CustomerAppDownloadStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
@Transactional
public class CustomerAppDownloadStatServiceImpl implements CustomerAppDownloadStatService {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Africa/Nairobi");
    private static final DateTimeFormatter REPORT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_TOKEN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final List<String> PACKAGE_HEADER_ALIASES = List.of("package", "package name", "package_name", "application id", "application_id", "packagename", "applicationid");
    private static final List<String> STORE_LISTING_ACQUISITIONS_ALIASES = List.of("store listing acquisitions", "storelistingacquisitions", "listing acquisitions", "store acquisitions");
    private static final List<String> STORE_LISTING_VISITORS_ALIASES = List.of("store listing visitors", "storelistingvisitors", "listing visitors", "store visitors");
    private static final List<String> TOTAL_INSTALLS_ALIASES = List.of("total installs", "totalinstalls", "active device installs", "activedeviceinstalls", "install events", "installevents");
    private static final List<String> USER_ACQUISITIONS_ALIASES = List.of("user acquisitions", "useracquisitions", "daily user installs", "dailyuserinstalls", "user installs", "userinstalls");
    private static final List<String> DEVICE_ACQUISITIONS_ALIASES = List.of("device acquisitions", "deviceacquisitions", "daily device installs", "dailydeviceinstalls", "device installs", "deviceinstalls");
    private static final List<String> FIRST_OPENS_ALIASES = List.of("first opens", "firstopens", "first open", "firstopen");

    private final CustomerAppDownloadStatRepository customerAppDownloadStatRepository;

    @Value("${customer-app-download-stats.google-play.enabled:false}")
    private boolean importEnabled;

    @Value("${customer-app-download-stats.google-play.bucket:pubsite_prod_7360692091076360515}")
    private String reportBucket;

    @Value("${customer-app-download-stats.google-play.installs-prefix:stats/installs/}")
    private String installsPrefix;

    @Value("${customer-app-download-stats.google-play.package-name:}")
    private String packageName;

    @Value("${customer-app-download-stats.google-play.start-month:}")
    private String startMonth;

    @Value("${spring.cloud.gc.storage.credentials.location}")
    private Resource credentialsResource;

    public CustomerAppDownloadStatServiceImpl(CustomerAppDownloadStatRepository customerAppDownloadStatRepository) {
        this.customerAppDownloadStatRepository = customerAppDownloadStatRepository;
    }

    @Override
    public void populateCustomerAppDownloads(CustomerAppDownloadStatRequest customerAppDownloadStatRequest) {
        LocalDate reportMonth = Optional.ofNullable(customerAppDownloadStatRequest.reportMonth())
                .orElseThrow(() -> new IllegalArgumentException("reportMonth is required"));

        CustomerAppDownloadStats customerAppDownloadStats = customerAppDownloadStatRepository.findByReportMonth(reportMonth)
                .orElse(new CustomerAppDownloadStats());

        customerAppDownloadStats.setReportMonth(reportMonth);
        customerAppDownloadStats.setDeviceAcquisitions(customerAppDownloadStatRequest.deviceAcquisitions());
        customerAppDownloadStats.setFirstOpens(customerAppDownloadStatRequest.firstOpens());
        customerAppDownloadStats.setStoreListingAcquisitions(customerAppDownloadStatRequest.storeListingAcquisitions());
        customerAppDownloadStats.setStoreListingVisitors(customerAppDownloadStatRequest.storeListingVisitors());
        customerAppDownloadStats.setTotalInstalls(customerAppDownloadStatRequest.totalInstalls());
        customerAppDownloadStats.setUserAcquisitions(customerAppDownloadStatRequest.userAcquisitions());
        customerAppDownloadStats.setSourceObjectPath(customerAppDownloadStatRequest.sourceObjectPath());
        customerAppDownloadStats.setImportedAt(LocalDateTime.now(REPORT_ZONE));

        customerAppDownloadStatRepository.save(customerAppDownloadStats);
    }

    @Override
    public void importMonthlyCustomerAppDownloads(YearMonth reportMonth) {
        if (!importEnabled) {
            log.debug("Customer app download stats import is disabled");
            return;
        }

        Blob reportBlob = findMonthlyReportBlob(reportMonth)
                .orElseThrow(() -> new IllegalStateException("No Google Play installs report found for " + reportMonth));

        CustomerAppDownloadStatRequest request = buildRequestFromBlob(reportBlob, reportMonth);
        populateCustomerAppDownloads(request);
        log.info("Customer app download stats imported for {} from gs://{}/{}", reportMonth, reportBucket, reportBlob.getName());
    }

    @Override
    public void importMissingCustomerAppDownloads() {
        if (!importEnabled) {
            log.debug("Customer app download stats import is disabled");
            return;
        }

        YearMonth latestClosedMonth = YearMonth.from(LocalDate.now(REPORT_ZONE)).minusMonths(1);
        YearMonth nextMonth = customerAppDownloadStatRepository.findTopByOrderByReportMonthDesc()
                .map(CustomerAppDownloadStats::getReportMonth)
                .map(YearMonth::from)
                .map(month -> month.plusMonths(1))
                .orElseGet(() -> resolveInitialMonth(latestClosedMonth));

        while (!nextMonth.isAfter(latestClosedMonth)) {
            try {
                importMonthlyCustomerAppDownloads(nextMonth);
            } catch (IllegalStateException exception) {
                log.warn("Customer app download stats import paused at {}: {}", nextMonth, exception.getMessage());
                break;
            }
            nextMonth = nextMonth.plusMonths(1);
        }
    }

    private YearMonth resolveInitialMonth(YearMonth latestClosedMonth) {
        if (!StringUtils.hasText(startMonth)) {
            return latestClosedMonth;
        }

        try {
            return YearMonth.parse(startMonth.trim(), REPORT_MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("Invalid customer-app-download-stats.google-play.start-month value: " + startMonth, exception);
        }
    }

    private Optional<Blob> findMonthlyReportBlob(YearMonth reportMonth) {
        String normalizedPrefix = normalizePrefix(installsPrefix);
        String monthToken = reportMonth.format(MONTH_TOKEN_FORMATTER);
        List<String> candidateTokens = List.of(monthToken, reportMonth.toString(), reportMonth.toString().replace("-", ""), reportMonth.toString().replace("-", "_"));
        List<Blob> matches = new ArrayList<>();

        for (Blob blob : buildStorageClient().list(reportBucket, Storage.BlobListOption.prefix(normalizedPrefix)).iterateAll()) {
            if (blob == null || blob.isDirectory()) {
                continue;
            }

            String objectName = blob.getName();
            String normalizedObjectName = objectName.toLowerCase(Locale.ROOT);
            if (!normalizedObjectName.contains("installs")) {
                continue;
            }
            if (!containsAnyToken(normalizedObjectName, candidateTokens)) {
                continue;
            }
            if (StringUtils.hasText(packageName) && !normalizedObjectName.contains(packageName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!(normalizedObjectName.endsWith(".csv") || normalizedObjectName.endsWith(".csv.gz") || normalizedObjectName.endsWith(".txt") || normalizedObjectName.endsWith(".txt.gz"))) {
                continue;
            }
            matches.add(blob);
        }

        return matches.stream()
                .sorted(Comparator.comparing(Blob::getName).reversed())
                .findFirst();
    }

    private CustomerAppDownloadStatRequest buildRequestFromBlob(Blob reportBlob, YearMonth reportMonth) {
        byte[] content = readBlobContent(reportBlob);
        char delimiter = detectDelimiter(content, reportBlob.getName());
        Charset charset = detectCharset(content);
        String decodedContent = stripBom(new String(content, charset));

        CSVFormat csvFormat = CSVFormat.DEFAULT
            .withDelimiter(delimiter)
            .withFirstRecordAsHeader()
            .withIgnoreSurroundingSpaces()
            .withTrim();

        try (Reader reader = new StringReader(decodedContent);
             CSVParser parser = csvFormat.parse(reader)) {

            if (parser.getHeaderMap().isEmpty()) {
                throw new IllegalStateException("Google Play installs report header is empty for " + reportBlob.getName());
            }

            Map<String, String> headersByAlias = new HashMap<>();
            for (String header : parser.getHeaderMap().keySet()) {
                headersByAlias.put(normalizeHeader(header), header);
            }

            MetricAccumulator accumulator = new MetricAccumulator();
            for (CSVRecord record : parser) {
                if (!matchesConfiguredPackage(record, headersByAlias)) {
                    continue;
                }

                accumulator.addStoreListingAcquisitions(readInteger(record, headersByAlias, STORE_LISTING_ACQUISITIONS_ALIASES));
                accumulator.addStoreListingVisitors(readInteger(record, headersByAlias, STORE_LISTING_VISITORS_ALIASES));
                accumulator.setTotalInstallsMax(readInteger(record, headersByAlias, TOTAL_INSTALLS_ALIASES));
                accumulator.addUserAcquisitions(readInteger(record, headersByAlias, USER_ACQUISITIONS_ALIASES));
                accumulator.addDeviceAcquisitions(readInteger(record, headersByAlias, DEVICE_ACQUISITIONS_ALIASES));
                accumulator.addFirstOpens(readInteger(record, headersByAlias, FIRST_OPENS_ALIASES));
            }

            if (!accumulator.hasObservedMetrics()) {
                throw new IllegalStateException("No matching customer app download metrics found in Google Play installs report " + reportBlob.getName());
            }

            return new CustomerAppDownloadStatRequest(
                    reportMonth.atDay(1),
                    "gs://" + reportBucket + "/" + reportBlob.getName(),
                    accumulator.storeListingAcquisitions(),
                    accumulator.storeListingVisitors(),
                    accumulator.totalInstalls(),
                    accumulator.userAcquisitions(),
                    accumulator.deviceAcquisitions(),
                    accumulator.firstOpens()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse Google Play installs report " + reportBlob.getName(), exception);
        }
    }

    private boolean matchesConfiguredPackage(CSVRecord record, Map<String, String> headersByAlias) {
        if (!StringUtils.hasText(packageName)) {
            return true;
        }

        String header = resolveHeader(headersByAlias, PACKAGE_HEADER_ALIASES);
        if (header == null || !record.isMapped(header)) {
            return true;
        }

        String candidate = record.get(header);
        return StringUtils.hasText(candidate) && packageName.equalsIgnoreCase(candidate.trim());
    }

    private Integer readInteger(CSVRecord record, Map<String, String> headersByAlias, List<String> aliases) {
        String header = resolveHeader(headersByAlias, aliases);
        if (header == null || !record.isMapped(header)) {
            return null;
        }

        String rawValue = record.get(header);
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        try {
            String normalized = rawValue.replace(",", "").trim();
            if (normalized.isEmpty() || "-".equals(normalized)) {
                return null;
            }
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            log.debug("Skipping non-numeric Google Play metric value '{}' for header '{}'", rawValue, header);
            return null;
        }
    }

    private String resolveHeader(Map<String, String> headersByAlias, List<String> aliases) {
        for (String alias : aliases) {
            String header = headersByAlias.get(normalizeHeader(alias));
            if (header != null) {
                return header;
            }
        }
        return null;
    }

    private Storage buildStorageClient() {
        try {
            return StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(credentialsResource.getInputStream()))
                    .build()
                    .getService();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize Google Cloud Storage client for customer app download stats", exception);
        }
    }

    private byte[] readBlobContent(Blob reportBlob) {
        byte[] bytes = reportBlob.getContent();
        if (reportBlob.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return gzipInputStream.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to unzip Google Play installs report " + reportBlob.getName(), exception);
            }
        }
        return bytes;
    }

    private Charset detectCharset(byte[] content) {
        if (content.length >= 2) {
            int first = Byte.toUnsignedInt(content[0]);
            int second = Byte.toUnsignedInt(content[1]);
            if ((first == 0xFE && second == 0xFF) || (first == 0xFF && second == 0xFE)) {
                return StandardCharsets.UTF_16;
            }
        }

        int zeroCount = 0;
        int inspected = Math.min(content.length, 256);
        for (int index = 0; index < inspected; index++) {
            if (content[index] == 0) {
                zeroCount++;
            }
        }

        return zeroCount > inspected / 8 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8;
    }

    private char detectDelimiter(byte[] content, String objectName) {
        Charset charset = detectCharset(content);
        String preview = stripBom(new String(content, charset));
        String firstLine = preview.lines().findFirst().orElse("");

        long tabCount = firstLine.chars().filter(character -> character == '\t').count();
        long semicolonCount = firstLine.chars().filter(character -> character == ';').count();
        long commaCount = firstLine.chars().filter(character -> character == ',').count();

        if (tabCount >= semicolonCount && tabCount >= commaCount && tabCount > 0) {
            return '\t';
        }
        if (semicolonCount >= commaCount && semicolonCount > 0) {
            return ';';
        }
        if (commaCount > 0) {
            return ',';
        }

        throw new IllegalStateException("Unable to detect delimiter for Google Play installs report " + objectName);
    }

    private String stripBom(String content) {
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "stats/installs/";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private boolean containsAnyToken(String objectName, List<String> tokens) {
        for (String token : tokens) {
            if (objectName.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static final class MetricAccumulator {
        private long storeListingAcquisitions;
        private long storeListingVisitors;
        private Integer totalInstalls;
        private long userAcquisitions;
        private long deviceAcquisitions;
        private long firstOpens;
        private boolean observedMetrics;

        void addStoreListingAcquisitions(Integer value) {
            if (value != null) {
                storeListingAcquisitions += value;
                observedMetrics = true;
            }
        }

        void addStoreListingVisitors(Integer value) {
            if (value != null) {
                storeListingVisitors += value;
                observedMetrics = true;
            }
        }

        void setTotalInstallsMax(Integer value) {
            if (value != null) {
                totalInstalls = totalInstalls == null ? value : Math.max(totalInstalls, value);
                observedMetrics = true;
            }
        }

        void addUserAcquisitions(Integer value) {
            if (value != null) {
                userAcquisitions += value;
                observedMetrics = true;
            }
        }

        void addDeviceAcquisitions(Integer value) {
            if (value != null) {
                deviceAcquisitions += value;
                observedMetrics = true;
            }
        }

        void addFirstOpens(Integer value) {
            if (value != null) {
                firstOpens += value;
                observedMetrics = true;
            }
        }

        boolean hasObservedMetrics() {
            return observedMetrics;
        }

        Integer storeListingAcquisitions() {
            return Math.toIntExact(storeListingAcquisitions);
        }

        Integer storeListingVisitors() {
            return Math.toIntExact(storeListingVisitors);
        }

        Integer totalInstalls() {
            return totalInstalls;
        }

        Integer userAcquisitions() {
            return Math.toIntExact(userAcquisitions);
        }

        Integer deviceAcquisitions() {
            return Math.toIntExact(deviceAcquisitions);
        }

        Integer firstOpens() {
            return Math.toIntExact(firstOpens);
        }
    }
}
