package com.app.itibari.repository;

import com.app.itibari.models.entities.AvcoItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AvcoRepository extends JpaRepository<AvcoItems, Long>, JpaSpecificationExecutor<AvcoItems> {

}
