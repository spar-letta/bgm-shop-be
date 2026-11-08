package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerFeedBack;
import com.app.itibari.models.entities.Customers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerFeedBackRepository extends JpaRepository<CustomerFeedBack, Long> {
    Page<CustomerFeedBack> findByCustomerAndDeletedIsFalse(Customers customer, Pageable pageable);

    Page<CustomerFeedBack> findAllByDeletedIsFalse(Pageable pageable);
}
