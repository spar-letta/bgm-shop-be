package com.app.itibari.models.request;

import java.time.LocalDate;

public record CustomerAppDownloadStatRequest(
        LocalDate reportMonth,
        String sourceObjectPath,
        Integer storeListingAcquisitions,
        Integer storeListingVisitors,
        Integer totalInstalls,
        Integer userAcquisitions,
        Integer deviceAcquisitions,
        Integer firstOpens
        ) {
}
