package com.app.itibari.repository;

import com.app.itibari.models.entities.SmsGroup;
import com.app.itibari.models.entities.SmsGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmsGroupEntityRepository extends JpaRepository<SmsGroupEntity, Long> {
    Optional<SmsGroupEntity> findByPhoneAndNameIgnoreCaseAndSmsGroup(String phone, String name, SmsGroup saveSmsGroup);

    List<SmsGroupEntity> findBySmsGroup(SmsGroup smsGroup);

}
