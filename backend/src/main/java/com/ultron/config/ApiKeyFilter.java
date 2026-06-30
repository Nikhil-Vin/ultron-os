package com.ultron.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Optional API-key gate (Section 11.8). When {@code ultron.api-key} is set, every {@code /api/**}
 * request (except health) must carry a matching {@code X-Ultron-Key} header. When the key is blank
 * (the default), the filter is a no-op — local dev and tests are unaffected. This is the secure
 * hook for browser extensions / remote web apps: set the key, send the header.
 */
@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final UltronProperties properties;

    public ApiKeyFilter(UltronProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String configured = properties.getApiKey();
        String path = req.getRequestURI();
        boolean enforced = configured != null && !configured.isBlank()
            && path.startsWith("/api/")
            && !path.startsWith("/api/health");

        if (enforced && !configured.equals(req.getHeader("X-Ultron-Key"))) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"invalid or missing X-Ultron-Key\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
