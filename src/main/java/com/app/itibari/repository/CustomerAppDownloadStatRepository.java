package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerAppDownloadStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CustomerAppDownloadStatRepository extends JpaRepository<CustomerAppDownloadStats, Long> {

	Optional<CustomerAppDownloadStats> findByReportMonth(LocalDate reportMonth);

	Optional<CustomerAppDownloadStats> findTopByOrderByReportMonthDesc();
}
