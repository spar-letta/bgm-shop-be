package com.app.itibari.repository;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.RejectionsCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RejectionCodeRepository extends JpaRepository<RejectionsCode, Long>, JpaSpecificationExecutor<RejectionsCode> {

    Optional<RejectionsCode> findRejectionsCodeByCodeAndDeletedIsFalse(String code);
    Optional<RejectionsCode> findRejectionsCodeByCodeAndModuleAndDeletedIsFalse(String code, Module module);

    Optional<RejectionsCode> findRejectionsCodeByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<RejectionsCode> findRejectionsCodesByDeletedIsFalse(Pageable pageable);

    Page<RejectionsCode> findRejectionsCodesByDescriptionContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    RejectionsCode findRejectionsCodeByCodeIgnoreCaseAndDeletedIsFalse(String name);

    Page<RejectionsCode> findRejectionsCodesByModuleAndDeletedIsFalse(Module module, Pageable pageable);
}
