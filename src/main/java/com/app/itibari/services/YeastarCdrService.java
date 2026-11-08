package com.app.itibari.services;

import com.app.itibari.models.request.YeastarCdrRequest;
import com.app.itibari.models.response.YeastarCallRecord;
import com.app.itibari.models.response.YeastarCdrResponse;
import com.app.itibari.utils.feign.YeastarApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeastarCdrService {

    private static final DateTimeFormatter CDR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final YeastarApiClient yeastarApiClient;
    private final YeastarTokenService yeastarTokenService;

    public List<YeastarCallRecord> getCallHistoryByPhone(String phoneNumber, LocalDate from, LocalDate to) {
        YeastarCdrRequest request = YeastarCdrRequest.builder()
                .callee(phoneNumber)
                .startTime(from.atStartOfDay().format(CDR_DATE_FORMAT))
                .endTime(to.atTime(23, 59, 59).format(CDR_DATE_FORMAT))
                .page(1)
                .limit(50)
                .build();

        return fetchCdr(request);
    }

    public List<YeastarCallRecord> getCallHistoryByExtension(String extension, LocalDate from, LocalDate to) {
        YeastarCdrRequest request = YeastarCdrRequest.builder()
                .caller(extension)
                .startTime(from.atStartOfDay().format(CDR_DATE_FORMAT))
                .endTime(to.atTime(23, 59, 59).format(CDR_DATE_FORMAT))
                .page(1)
                .limit(50)
                .build();

        return fetchCdr(request);
    }

    private List<YeastarCallRecord> fetchCdr(YeastarCdrRequest request) {
        YeastarCdrResponse response = yeastarApiClient.searchCdr(
                yeastarTokenService.getValidToken(),
                request
        );
        if (!response.isSuccess()) {
            log.warn("CDR search failed");
            return List.of();
        }
        return response.getCdrList() != null ? response.getCdrList() : List.of();
    }
}