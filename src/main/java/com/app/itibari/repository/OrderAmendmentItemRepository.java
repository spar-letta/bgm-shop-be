package com.app.itibari.repository;

import com.app.itibari.models.entities.AmendmentProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAmendmentItemRepository extends JpaRepository<AmendmentProductItem, Long> {

}
