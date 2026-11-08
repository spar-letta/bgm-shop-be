package com.app.itibari.services;

import com.app.itibari.models.request.CustomerAppDownloadStatRequest;

import java.time.YearMonth;

public interface CustomerAppDownloadStatService {

    void populateCustomerAppDownloads(CustomerAppDownloadStatRequest customerAppDownloadStatRequest);

    void importMonthlyCustomerAppDownloads(YearMonth reportMonth);

    void importMissingCustomerAppDownloads();
}
