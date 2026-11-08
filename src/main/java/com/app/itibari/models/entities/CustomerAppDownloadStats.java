package com.app.itibari.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "customer_app_download_stats", schema = "itibari",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_app_download_stats_report_month", columnNames = "report_month"))
public class CustomerAppDownloadStats {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "store_listing_acquisitions")
    private Integer storeListingAcquisitions; // The number of unique users who visited your store listing and installed your app. This counts a user only once, even if they install on multiple devices.

    @Column(name = "store_listing_visitors")
    private Integer storeListingVisitors; // The number of unique users who visited your store listing. This is a key metric for calculating conversion rates.

    @Column(name = "total_installs")
    private Integer totalInstalls; // The cumulative, total number of times your app has been installed. This is a lifetime count.

    @Column(name = "user_acquisitions")
    private Integer userAcquisitions; // A metric representing unique user acquisitions, which includes both new installs and reactivations of previous users.

    @Column(name = "device_acquisitions")
    private Integer deviceAcquisitions; // The number of unique devices on which your app was installed, which can differ from user acquisitions if a single user has multiple devices.

    @Column(name = "first_opens")
    private Integer firstOpens; // The number of times the app was opened for the first time after installation.

    @Column(name = "source_object_path")
    private String sourceObjectPath;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;


}
