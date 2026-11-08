package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.OperationTimingStatus;
import com.app.itibari.models.entities.OperationTimingLog;
import com.app.itibari.repository.OperationTimingLogRepository;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TimingLogHelperTest {

    @Test
    public void time_persistsSuccessTimingLog() {
        List<OperationTimingLog> savedLogs = new ArrayList<>();
        TimingLogHelper helper = new TimingLogHelper(repositoryCapturing(savedLogs));

        String result = helper.time(
                "Create route planning",
                OperationTimingEntityType.ROUTE_PLANNING,
                null,
                null,
                "admin@test.com",
                value -> "RT-001",
                value -> "route-public-id",
                () -> "done"
        );

        assertEquals("done", result);
        assertEquals(1, savedLogs.size());

        OperationTimingLog log = savedLogs.get(0);
        assertEquals("Create route planning", log.getOperationName());
        assertEquals(OperationTimingEntityType.ROUTE_PLANNING, log.getEntityType());
        assertEquals("RT-001", log.getRouteId());
        assertEquals("route-public-id", log.getRoutePublicId());
        assertEquals("admin@test.com", log.getRequestedBy());
        assertEquals(OperationTimingStatus.SUCCESS, log.getStatus());
        assertNull(log.getErrorMessage());
        assertNotNull(log.getStartTime());
        assertNotNull(log.getEndTime());
        assertNotNull(log.getDurationMs());
        assertTrue(log.getDurationMs() >= 0);
    }

    @Test
    public void time_persistsFailedTimingLogAndRethrowsException() {
        List<OperationTimingLog> savedLogs = new ArrayList<>();
        TimingLogHelper helper = new TimingLogHelper(repositoryCapturing(savedLogs));

        RuntimeException thrown = null;
        try {
            helper.time(
                    "Update issuance table routeId=RT-001 products=2",
                    OperationTimingEntityType.ISSUANCE,
                    "RT-001",
                    "route-public-id",
                    "admin@test.com",
                    () -> {
                        throw new RuntimeException("database timeout");
                    }
            );
            fail("Expected timing helper to rethrow the operation exception");
        } catch (RuntimeException exception) {
            thrown = exception;
        }

        assertNotNull(thrown);
        assertEquals("database timeout", thrown.getMessage());
        assertEquals(1, savedLogs.size());

        OperationTimingLog log = savedLogs.get(0);
        assertEquals("Update issuance table routeId=RT-001 products=2", log.getOperationName());
        assertEquals(OperationTimingEntityType.ISSUANCE, log.getEntityType());
        assertEquals("RT-001", log.getRouteId());
        assertEquals("route-public-id", log.getRoutePublicId());
        assertEquals("admin@test.com", log.getRequestedBy());
        assertEquals(OperationTimingStatus.FAILED, log.getStatus());
        assertEquals("database timeout", log.getErrorMessage());
        assertNotNull(log.getStartTime());
        assertNotNull(log.getEndTime());
        assertNotNull(log.getDurationMs());
        assertTrue(log.getDurationMs() >= 0);
    }

    private OperationTimingLogRepository repositoryCapturing(List<OperationTimingLog> savedLogs) {
        return (OperationTimingLogRepository) Proxy.newProxyInstance(
                OperationTimingLogRepository.class.getClassLoader(),
                new Class[]{OperationTimingLogRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName()) && args != null && args.length == 1
                            && args[0] instanceof OperationTimingLog timingLog) {
                        savedLogs.add(timingLog);
                        return timingLog;
                    }
                    if ("toString".equals(method.getName())) {
                        return "OperationTimingLogRepository test proxy";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException("Unexpected repository method call: " + method.getName());
                }
        );
    }
}
