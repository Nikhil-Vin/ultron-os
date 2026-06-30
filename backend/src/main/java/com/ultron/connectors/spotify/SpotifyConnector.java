package com.ultron.connectors.spotify;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Spotify connector (L5). Playback control is HIGH (it changes device state) → gated. Used by scene
 * automation (e.g. Trading Mode → focus playlist). Opt-in via {@code ultron.connectors.spotify-token}.
 */
@Component
public class SpotifyConnector {

    private static final String BASE = "https://api.spotify.com/v1/me/player";

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final RestClient client = RestClient.create();

    public SpotifyConnector(ConnectorProperties props, ApprovalGate approvalGate) {
        this.props = props;
        this.approvalGate = approvalGate;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getSpotifyToken());
    }

    public ConnectorResponse play(String contextUri, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Spotify");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("spotify.play", RiskLevel.HIGH, "Start playback " + contextUri, "spotify"),
            humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Playback blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            client.put()
                .uri(BASE + "/play")
                .header("Authorization", "Bearer " + props.getSpotifyToken())
                .body(contextUri == null ? java.util.Map.of() : java.util.Map.of("context_uri", contextUri))
                .retrieve().toBodilessEntity();
            return ConnectorResponse.ok("playing", contextUri);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Spotify play failed: " + ex.getMessage());
        }
    }

    public ConnectorResponse pause(boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Spotify");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("spotify.pause", RiskLevel.HIGH, "Pause playback", "spotify"), humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Pause blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            client.put().uri(BASE + "/pause")
                .header("Authorization", "Bearer " + props.getSpotifyToken())
                .retrieve().toBodilessEntity();
            return ConnectorResponse.ok("paused", null);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Spotify pause failed: " + ex.getMessage());
        }
    }
}
