package com.ultron.connectors.calendar;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Google Calendar connector (L5). Reading events is READ; creating an event is HIGH (gated).
 * {@link #suggestTimes} is a local READ heuristic. Opt-in via {@code ultron.connectors.calendar-token}.
 */
@Component
public class CalendarConnector {

    private static final String BASE = "https://www.googleapis.com/calendar/v3/calendars/primary";

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final RestClient client = RestClient.create();

    public CalendarConnector(ConnectorProperties props, ApprovalGate approvalGate) {
        this.props = props;
        this.approvalGate = approvalGate;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getCalendarToken());
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse upcoming(int max) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Calendar");
        }
        try {
            Map<String, Object> res = client.get()
                .uri(BASE + "/events?maxResults={m}&orderBy=startTime&singleEvents=true", max <= 0 ? 10 : max)
                .header("Authorization", "Bearer " + props.getCalendarToken())
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("events", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Calendar read failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse create(String title, String startIso, String endIso, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Calendar");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("calendar.create", RiskLevel.HIGH, "Create event '" + title + "'", "calendar"),
            humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Event create blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            Map<String, Object> res = client.post()
                .uri(BASE + "/events")
                .header("Authorization", "Bearer " + props.getCalendarToken())
                .body(Map.of("summary", title,
                    "start", Map.of("dateTime", startIso),
                    "end", Map.of("dateTime", endIso)))
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("created", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Calendar create failed: " + ex.getMessage());
        }
    }

    /** READ — naive free-slot suggestion within working hours (local heuristic). */
    public ConnectorResponse suggestTimes(int durationMinutes) {
        List<String> slots = List.of(
            slot(LocalTime.of(10, 0), durationMinutes),
            slot(LocalTime.of(14, 0), durationMinutes),
            slot(LocalTime.of(16, 30), durationMinutes));
        return ConnectorResponse.ok("suggested", slots);
    }

    private static String slot(LocalTime start, int mins) {
        return start + "–" + start.plusMinutes(mins);
    }
}
