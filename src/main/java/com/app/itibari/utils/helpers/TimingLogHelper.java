package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.OperationTimingStatus;
import com.app.itibari.models.entities.OperationTimingLog;
import com.app.itibari.repository.OperationTimingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimingLogHelper {

    private final OperationTimingLogRepository operationTimingLogRepository;

    public <T> T time(String operationName, Supplier<T> operation) {
        return time(operationName, null, null, null, null, operation);
    }

    public <T> T time(String operationName,
                      OperationTimingEntityType entityType,
                      String routeId,
                      String routePublicId,
                      String requestedBy,
                      Supplier<T> operation) {
        return time(operationName, entityType, routeId, routePublicId, requestedBy, null, null, operation);
    }

    public <T> T time(String operationName,
                      OperationTimingEntityType entityType,
                      String routeId,
                      String routePublicId,
                      String requestedBy,
                      Function<T, String> routeIdExtractor,
                      Function<T, String> routePublicIdExtractor,
                      Supplier<T> operation) {
        LocalDateTime startTime = LocalDateTime.now();
        long startNanos = System.nanoTime();
        log.info("{} started at {}", operationName, startTime);

        try {
            T result = operation.get();
            String persistedRouteId = routeIdExtractor == null ? routeId : routeIdExtractor.apply(result);
            String persistedRoutePublicId = routePublicIdExtractor == null ? routePublicId : routePublicIdExtractor.apply(result);
            saveTimingLog(operationName, entityType, persistedRouteId, persistedRoutePublicId, requestedBy, startTime,
                    LocalDateTime.now(), elapsedMillis(startNanos), OperationTimingStatus.SUCCESS, null);
            return result;
        } catch (RuntimeException exception) {
            LocalDateTime failureTime = LocalDateTime.now();
            long durationMs = elapsedMillis(startNanos);
            log.info("{} failed at {} after {} ms", operationName, failureTime, durationMs);
            saveTimingLog(operationName, entityType, routeId, routePublicId, requestedBy, startTime,
                    failureTime, durationMs, OperationTimingStatus.FAILED, exception.getMessage());
            throw exception;
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            log.info("{} ended at {} and took {} ms", operationName, endTime, elapsedMillis(startNanos));
        }
    }

    public void time(String operationName, Runnable operation) {
        time(operationName, () -> {
            operation.run();
            return null;
        });
    }

    public void time(String operationName,
                     OperationTimingEntityType entityType,
                     String routeId,
                     String routePublicId,
                     String requestedBy,
                     Runnable operation) {
        time(operationName, entityType, routeId, routePublicId, requestedBy, () -> {
            operation.run();
            return null;
        });
    }

    private void saveTimingLog(String operationName,
                               OperationTimingEntityType entityType,
                               String routeId,
                               String routePublicId,
                               String requestedBy,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               long durationMs,
                               OperationTimingStatus status,
                               String errorMessage) {
        if (entityType == null) {
            return;
        }

        try {
            OperationTimingLog timingLog = new OperationTimingLog();
            timingLog.setOperationName(operationName);
            timingLog.setEntityType(entityType);
            timingLog.setRouteId(routeId);
            timingLog.setRoutePublicId(routePublicId);
            timingLog.setRequestedBy(requestedBy);
            timingLog.setStartTime(startTime);
            timingLog.setEndTime(endTime);
            timingLog.setDurationMs(durationMs);
            timingLog.setStatus(status);
            timingLog.setErrorMessage(errorMessage);
            operationTimingLogRepository.save(timingLog);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist operation timing log for {}: {}", operationName, exception.getMessage());
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
