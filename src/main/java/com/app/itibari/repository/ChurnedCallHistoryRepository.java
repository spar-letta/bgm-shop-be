package com.app.itibari.repository;

import com.app.itibari.models.entities.ChurnedCallHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChurnedCallHistoryRepository extends JpaRepository<ChurnedCallHistory, Long> {

    Page<ChurnedCallHistory> findByCustomerPublicId(String customerPublicId, Pageable pageable);
}
