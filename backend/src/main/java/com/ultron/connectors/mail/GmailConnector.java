package com.ultron.connectors.mail;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gmail connector (L5 — Section 11.7). Reading/searching is READ (free); drafting is LOW; SENDING
 * is HIGH and must pass the {@link ApprovalGate} (blocked by default — human-in-the-loop). Opt-in:
 * inert until {@code ultron.connectors.gmail-token} is set.
 */
@Component
public class GmailConnector {

    private static final Logger log = LoggerFactory.getLogger(GmailConnector.class);
    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me";

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final RestClient client = RestClient.create();

    public GmailConnector(ConnectorProperties props, ApprovalGate approvalGate) {
        this.props = props;
        this.approvalGate = approvalGate;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getGmailToken());
    }

    /** READ — list recent messages matching an optional query. */
    @SuppressWarnings("unchecked")
    public ConnectorResponse search(String query, int max) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Gmail");
        }
        try {
            Map<String, Object> res = client.get()
                .uri(BASE + "/messages?maxResults={m}&q={q}", max <= 0 ? 10 : max, query == null ? "" : query)
                .header("Authorization", "Bearer " + props.getGmailToken())
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("messages", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Gmail read failed: " + ex.getMessage());
        }
    }

    /** HIGH — send an email. Gated: blocked unless an approval is supplied. */
    @SuppressWarnings("unchecked")
    public ConnectorResponse send(String to, String subject, String body, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Gmail");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("gmail.send", RiskLevel.HIGH, "Send email to " + to, "gmail"), humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Email send blocked by approval gate (decision="
                + gate.decision() + "). Approve to proceed.");
        }
        try {
            String raw = "To: " + to + "\r\nSubject: " + subject + "\r\n\r\n" + body;
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes());
            Map<String, Object> res = client.post()
                .uri(BASE + "/messages/send")
                .header("Authorization", "Bearer " + props.getGmailToken())
                .body(Map.of("raw", encoded))
                .retrieve().body(Map.class);
            log.info("Gmail sent to {}", to);
            return ConnectorResponse.ok("sent", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Gmail send failed: " + ex.getMessage());
        }
    }
}
