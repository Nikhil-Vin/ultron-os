package com.ultron.connectors.slack;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.intelligence.BrainSelector;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Slack connector (L5). Read history (READ), post a message (HIGH, gated), and a brain-summarised
 * channel digest. Opt-in via {@code ultron.connectors.slack-token}.
 */
@Component
public class SlackConnector {

    private static final String BASE = "https://slack.com/api";

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final BrainSelector brain;
    private final RestClient client = RestClient.create();

    public SlackConnector(ConnectorProperties props, ApprovalGate approvalGate, BrainSelector brain) {
        this.props = props;
        this.approvalGate = approvalGate;
        this.brain = brain;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getSlackToken());
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse history(String channel, int limit) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Slack");
        }
        try {
            Map<String, Object> res = client.get()
                .uri(BASE + "/conversations.history?channel={c}&limit={l}", channel, limit <= 0 ? 20 : limit)
                .header("Authorization", "Bearer " + props.getSlackToken())
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("history", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Slack read failed: " + ex.getMessage());
        }
    }

    public ConnectorResponse summary(String channel) {
        ConnectorResponse h = history(channel, 30);
        if (!h.connected() || !h.ok()) {
            return h;
        }
        String text = brain.think("Summarise this Slack channel activity in 3 lines:\n" + h.data());
        return ConnectorResponse.ok("summary", text);
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse send(String channel, String text, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Slack");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("slack.send", RiskLevel.HIGH, "Post to Slack " + channel, "slack"), humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Slack post blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            Map<String, Object> res = client.post()
                .uri(BASE + "/chat.postMessage")
                .header("Authorization", "Bearer " + props.getSlackToken())
                .body(Map.of("channel", channel, "text", text))
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("posted", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Slack post failed: " + ex.getMessage());
        }
    }
}
