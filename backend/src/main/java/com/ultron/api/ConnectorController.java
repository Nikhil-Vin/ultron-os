package com.ultron.api;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.connectors.calendar.CalendarConnector;
import com.ultron.connectors.homeassistant.HaConnector;
import com.ultron.connectors.mail.GmailConnector;
import com.ultron.connectors.notion.NotionConnector;
import com.ultron.connectors.slack.SlackConnector;
import com.ultron.connectors.spotify.SpotifyConnector;
import com.ultron.connectors.twilio.TwilioConnector;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Connector API (L0 — Section 11.7). Reports connection status for the settings view and exposes a
 * few representative operations. Sends/creates/scenes are HIGH and gated inside each connector.
 */
@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {

    private final GmailConnector gmail;
    private final CalendarConnector calendar;
    private final NotionConnector notion;
    private final SlackConnector slack;
    private final SpotifyConnector spotify;
    private final HaConnector ha;
    private final TwilioConnector twilio;

    public ConnectorController(GmailConnector gmail, CalendarConnector calendar, NotionConnector notion,
                               SlackConnector slack, SpotifyConnector spotify, HaConnector ha, TwilioConnector twilio) {
        this.gmail = gmail;
        this.calendar = calendar;
        this.notion = notion;
        this.slack = slack;
        this.spotify = spotify;
        this.ha = ha;
        this.twilio = twilio;
    }

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gmail", gmail.isConfigured());
        body.put("calendar", calendar.isConfigured());
        body.put("notion", notion.isConfigured());
        body.put("slack", slack.isConfigured());
        body.put("spotify", spotify.isConfigured());
        body.put("homeassistant", ha.isConfigured());
        body.put("twilio", Map.of("configured", twilio.isConfigured(), "liveEnabled", twilio.isLiveEnabled()));
        body.put("note", "Sends/creates/scenes require approval. Twilio is a disabled contact extension point.");
        return body;
    }

    @PostMapping("/scene")
    public ConnectorResponse scene(@RequestBody SceneRequest req) {
        return ha.activateScene(req.scene(), req.approved());
    }

    @GetMapping("/gmail")
    public ConnectorResponse gmailSearch(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "10") int max) {
        return gmail.search(q, max);
    }

    @GetMapping("/calendar")
    public ConnectorResponse calendarUpcoming(@RequestParam(defaultValue = "10") int max) {
        return calendar.upcoming(max);
    }

    @GetMapping("/slack/summary")
    public ConnectorResponse slackSummary(@RequestParam String channel) {
        return slack.summary(channel);
    }

    public record SceneRequest(String scene, boolean approved) {
    }
}
