package com.ultron.connectors.homeassistant;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Home Assistant connector (L5). Triggers scenes — "Trading Mode", "Focus Mode", "End of Day"
 * (Section 9.6/9.8). Controlling physical devices is HIGH → gated. Opt-in via
 * {@code ultron.connectors.homeassistant-url} + token.
 */
@Component
public class HaConnector {

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final RestClient client = RestClient.create();

    public HaConnector(ConnectorProperties props, ApprovalGate approvalGate) {
        this.props = props;
        this.approvalGate = approvalGate;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getHomeassistantUrl())
            && ConnectorProperties.set(props.getHomeassistantToken());
    }

    /** Map a named work-mode scene to a Home Assistant scene entity. */
    public ConnectorResponse activateScene(String sceneName, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Home Assistant");
        }
        String entity = switch (sceneName == null ? "" : sceneName.toLowerCase()) {
            case "trading", "trading mode" -> "scene.trading_mode";
            case "focus", "focus mode", "deep work" -> "scene.focus_mode";
            case "end of day", "end-of-day" -> "scene.end_of_day";
            default -> "scene." + (sceneName == null ? "unknown" : sceneName.toLowerCase().replace(' ', '_'));
        };
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("homeassistant.scene", RiskLevel.HIGH, "Activate " + entity, "homeassistant"),
            humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Scene blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            client.post()
                .uri(props.getHomeassistantUrl() + "/api/services/scene/turn_on")
                .header("Authorization", "Bearer " + props.getHomeassistantToken())
                .body(Map.of("entity_id", entity))
                .retrieve().toBodilessEntity();
            return ConnectorResponse.ok("scene-activated", entity);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("HA scene failed: " + ex.getMessage());
        }
    }
}
