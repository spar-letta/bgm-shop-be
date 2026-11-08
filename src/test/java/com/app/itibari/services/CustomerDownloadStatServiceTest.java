package com.app.itibari.services;

import com.app.itibari.models.entities.CustomerAppDownloadStats;
import com.app.itibari.models.request.CustomerAppDownloadStatRequest;
import com.app.itibari.repository.CustomerAppDownloadStatRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomerDownloadStatServiceTest {
    @Mock
    private CustomerAppDownloadStatRepository customerAppDownloadStatRepository;

    @InjectMocks
    private CustomerAppDownloadStatServiceImpl customerAppDownloadStatService;


    @Test
    public void testPopulateCustomerAppDownloadsCreatesMonthlyRecord() {
        LocalDate reportMonth = LocalDate.of(2026, 6, 1);
        CustomerAppDownloadStatRequest mockRequest = new CustomerAppDownloadStatRequest(
                reportMonth,
                "gs://pubsite_prod_7360692091076360515/stats/installs/report.csv",
                100, // storeListingAcquisitions
                200, // storeListingVisitors
                300, // totalInstalls
                400, // userAcquisitions
                500, // deviceAcquisitions
                600  // firstOpens
        );

        when(customerAppDownloadStatRepository.findByReportMonth(reportMonth)).thenReturn(Optional.empty());

        customerAppDownloadStatService.populateCustomerAppDownloads(mockRequest);

        ArgumentCaptor<CustomerAppDownloadStats> statsCaptor = ArgumentCaptor.forClass(CustomerAppDownloadStats.class);
        verify(customerAppDownloadStatRepository).save(statsCaptor.capture());

        CustomerAppDownloadStats savedStats = statsCaptor.getValue();
        assertEquals(reportMonth, savedStats.getReportMonth());
        assertEquals(Integer.valueOf(100), savedStats.getStoreListingAcquisitions());
        assertEquals(Integer.valueOf(200), savedStats.getStoreListingVisitors());
        assertEquals(Integer.valueOf(300), savedStats.getTotalInstalls());
        assertEquals(Integer.valueOf(400), savedStats.getUserAcquisitions());
        assertEquals(Integer.valueOf(500), savedStats.getDeviceAcquisitions());
        assertEquals(Integer.valueOf(600), savedStats.getFirstOpens());
        assertEquals("gs://pubsite_prod_7360692091076360515/stats/installs/report.csv", savedStats.getSourceObjectPath());
    }

    @Test
    public void testPopulateCustomerAppDownloadsUpdatesExistingMonth() {
        LocalDate reportMonth = LocalDate.of(2026, 6, 1);
        CustomerAppDownloadStats existingStats = new CustomerAppDownloadStats();
        existingStats.setId(44L);
        existingStats.setReportMonth(reportMonth);
        existingStats.setSourceObjectPath("old-path");

        when(customerAppDownloadStatRepository.findByReportMonth(reportMonth)).thenReturn(Optional.of(existingStats));

        customerAppDownloadStatService.populateCustomerAppDownloads(new CustomerAppDownloadStatRequest(
                reportMonth,
                null,
                1,
                2,
                3,
                4,
                5,
                6
        ));

        ArgumentCaptor<CustomerAppDownloadStats> statsCaptor = ArgumentCaptor.forClass(CustomerAppDownloadStats.class);
        verify(customerAppDownloadStatRepository).save(statsCaptor.capture());

        CustomerAppDownloadStats savedStats = statsCaptor.getValue();
        assertEquals(Long.valueOf(44L), savedStats.getId());
        assertEquals(reportMonth, savedStats.getReportMonth());
        assertEquals(Integer.valueOf(3), savedStats.getTotalInstalls());
        assertNull(savedStats.getSourceObjectPath());

    }
}
