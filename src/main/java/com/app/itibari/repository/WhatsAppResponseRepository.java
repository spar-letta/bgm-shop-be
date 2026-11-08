package com.app.itibari.repository;

import com.app.itibari.dataTypes.WhatsAppStatus;
import com.app.itibari.models.entities.WhatsAppResponse;
import com.app.itibari.models.response.WhatsAppResponseWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WhatsAppResponseRepository extends JpaRepository<WhatsAppResponse, Long> {
    Page<WhatsAppResponse> findByDeletedIsFalse(Pageable pageable);

    Optional<WhatsAppResponse> findByMessageIdIgnoreCase(String id);

    Page<WhatsAppResponse> findByPhoneNumberContainingIgnoreCase(String phoneNumber, Pageable pageable);
    List<WhatsAppResponse> findByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCase(String phoneNumber, String sourceType);

    Optional<WhatsAppResponse> findTopByPhoneNumberIgnoreCaseOrderByCreationTimeDesc(String phoneNumber1);

    Optional<WhatsAppResponse> findTopByPhoneNumberContainingIgnoreCaseOrderByCreationTimeDesc(String phoneNumber);

    boolean existsByPhoneNumberContainingIgnoreCaseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
            String phoneNumber, String sourceType, LocalDateTime since);

    long countByCxReadFalseAndSourceTypeIgnoreCaseAndCreationTimeGreaterThanEqual(
            String sourceType, LocalDateTime since);

    @Query("""
        SELECT new com.app.itibari.models.response.WhatsAppResponseWrapper(
            CAST(FUNCTION('TO_CHAR', w.creationTime, 'YYYY-MM-DD HH24:MI:SS') AS String),
            w.customerName,
            w.phoneNumber,
            w.storeName,
            (
                SELECT COUNT(w2)
                FROM WhatsAppResponse w2
                WHERE FUNCTION('RIGHT', w2.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
                  AND w2.cxRead = false
                  AND w2.sourceType = 'Direct'
                  AND w2.creationTime >= :unreadSince
            )
        )
        FROM WhatsAppResponse w
        WHERE w.creationTime = (
            SELECT MAX(w2.creationTime)
            FROM WhatsAppResponse w2
            WHERE FUNCTION('RIGHT', w2.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
        )
        AND (CAST(:status AS String) IS NULL OR w.status = :status)
        AND (CAST(:search AS String) IS NULL OR UPPER(w.phoneNumber) LIKE :search
             OR UPPER(w.customerName) LIKE :search)
        AND (
            EXISTS (
                SELECT 1
                FROM WhatsAppResponse u
                WHERE FUNCTION('RIGHT', u.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
                  AND u.cxRead = false
                  AND u.sourceType = 'Direct'
                  AND u.creationTime >= :unreadSince
            )
            OR (
                w.creationTime >= COALESCE(CAST(:startDate AS LocalDateTime), w.creationTime)
                AND w.creationTime <= COALESCE(CAST(:endDate AS LocalDateTime), w.creationTime)
            )
        )
        AND (
            CAST(:unread AS boolean) IS NULL
            OR (
                :unread = true
                AND EXISTS (
                    SELECT 1
                    FROM WhatsAppResponse u
                    WHERE FUNCTION('RIGHT', u.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
                      AND u.cxRead = false
                      AND u.sourceType = 'Direct'
                      AND u.creationTime >= :unreadSince
                )
            )
            OR (
                :unread = false
                AND NOT EXISTS (
                    SELECT 1
                    FROM WhatsAppResponse u
                    WHERE FUNCTION('RIGHT', u.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
                      AND u.cxRead = false
                      AND u.sourceType = 'Direct'
                      AND u.creationTime >= :unreadSince
                )
            )
        )
        ORDER BY
            CASE
                WHEN (
                    SELECT COUNT(u)
                    FROM WhatsAppResponse u
                    WHERE FUNCTION('RIGHT', u.phoneNumber, 9) = FUNCTION('RIGHT', w.phoneNumber, 9)
                      AND u.cxRead = false
                      AND u.sourceType = 'Direct'
                      AND u.creationTime >= :unreadSince
                ) > 0 THEN 0
                ELSE 1
            END ASC,
            w.creationTime DESC
    """)
    Page<WhatsAppResponseWrapper> fetchLatestWithUnread(
            @Param("status") WhatsAppStatus status,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("unread") Boolean unread,
            @Param("unreadSince") LocalDateTime unreadSince,
            Pageable pageable
    );

}
