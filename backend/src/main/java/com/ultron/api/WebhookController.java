package com.ultron.api;

import com.ultron.governance.InputSanitizer;
import com.ultron.governance.RateLimiter;
import com.ultron.memory.Memory;
import com.ultron.memory.MemoryService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/webhook} — inbound events from external sources (CI, GitHub, IFTTT, etc.) are
 * sanitised, rate-limited per source, and captured into memory as a LOW-risk note (gated + audited).
 *
 * <p><b>Security note:</b> this endpoint is network-facing ingress. Phase 1 protects it with input
 * sanitisation + per-source rate limiting only; it has no authentication yet. Before exposing it
 * beyond localhost, add a shared-secret/HMAC signature check (planned with the OAuth2/mTLS work in
 * {@code SecurityConfig}). Treat all webhook content as untrusted data, never as instructions.
 */
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final InputSanitizer sanitizer;
    private final RateLimiter rateLimiter;
    private final MemoryService memoryService;

    public WebhookController(InputSanitizer sanitizer, RateLimiter rateLimiter, MemoryService memoryService) {
        this.sanitizer = sanitizer;
        this.rateLimiter = rateLimiter;
        this.memoryService = memoryService;
    }

    @PostMapping
    public ResponseEntity<WebhookResponse> receive(@RequestBody WebhookEvent event) {
        String source = sanitizer.sanitize(event.source());
        String key = source.isBlank() ? "anonymous" : source;

        if (!rateLimiter.tryAcquire("webhook:" + key)) {
            log.warn("Webhook throttled for source={}", key);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new WebhookResponse(false, null, "rate limit exceeded for source " + key));
        }

        InputSanitizer.Result screened = sanitizer.screen(event.payload());
        String note = "[webhook:" + key + "] " + event.event() + " — " + screened.sanitized();
        Memory saved = memoryService.save(note, "WEBHOOK", "webhook:" + key,
            screened.suspicious() ? "webhook,flagged" : "webhook");

        log.info("Webhook captured source={} event={} flagged={}", key, event.event(), screened.suspicious());
        return ResponseEntity.accepted()
            .body(new WebhookResponse(true, saved.getId().toString(),
                screened.suspicious() ? "captured (content flagged, stored as data only)" : "captured"));
    }

    public record WebhookEvent(@NotBlank String source, String event, String payload) {
    }

    public record WebhookResponse(boolean accepted, String memoryId, String message) {
    }
}
