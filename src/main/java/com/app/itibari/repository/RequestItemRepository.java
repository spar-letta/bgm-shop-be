package com.app.itibari.repository;

import com.app.itibari.models.entities.RequestItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestItemRepository extends JpaRepository<RequestItems, Long> {

}
