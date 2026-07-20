package com.bzj.chainsentinel.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesValidRequestIdAndCleansMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/transactions/risk/high");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "client-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
            assertEquals("client-request-123", MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY));
            ((MockHttpServletResponse) currentResponse).setStatus(204);
        });

        assertEquals("client-request-123", response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER));
        assertEquals(204, response.getStatus());
        assertNull(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void replacesInvalidRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/transactions/test");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "invalid request id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
        });

        String generatedRequestId = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertNotEquals("invalid request id", generatedRequestId);
        assertTrue(generatedRequestId.matches("^[a-f0-9-]{36}$"));
        assertNull(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void cleansMdcWhenRequestFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(IOException.class, () -> filter.doFilter(
                request,
                response,
                (currentRequest, currentResponse) -> {
                    throw new IOException("test failure");
                }
        ));

        assertNull(MDC.get(RequestTraceFilter.REQUEST_ID_MDC_KEY));
    }
}
