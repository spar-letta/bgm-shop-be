package com.app.itibari.repository;

import com.app.itibari.models.entities.BnplFieldVisit;
import com.app.itibari.models.entities.BnplLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BnplFieldVisitRepository extends JpaRepository<BnplFieldVisit, Long> {

    Page<BnplFieldVisit> findByLeadAndDeletedIsFalseOrderByVisitDateDesc(BnplLead lead, Pageable pageable);
}