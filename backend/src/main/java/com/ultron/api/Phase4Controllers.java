package com.ultron.api;

import com.ultron.intelligence.finance.FinancialOverviewService;
import com.ultron.intelligence.meeting.MeetingIntelligenceService;
import com.ultron.intelligence.notifications.SmartNotificationFilter;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phase 4 intelligence endpoints: meeting brief/capture, notification routing, financial overview. */
public class Phase4Controllers {

    @RestController
    @RequestMapping("/api/meeting")
    public static class MeetingController {
        private final MeetingIntelligenceService meeting;

        public MeetingController(MeetingIntelligenceService meeting) {
            this.meeting = meeting;
        }

        @PostMapping("/brief")
        public MeetingIntelligenceService.Brief brief(@RequestBody BriefRequest req) {
            return meeting.preMeetingBrief(req.title(), req.attendees());
        }

        @PostMapping("/capture")
        public MeetingIntelligenceService.PostCall capture(@RequestBody CaptureRequest req) {
            return meeting.capture(req.title(), req.transcript());
        }

        public record BriefRequest(@NotBlank String title, List<String> attendees) {
        }

        public record CaptureRequest(@NotBlank String title, @NotBlank String transcript) {
        }
    }

    @RestController
    @RequestMapping("/api/notifications")
    public static class NotificationController {
        private final SmartNotificationFilter filter;

        public NotificationController(SmartNotificationFilter filter) {
            this.filter = filter;
        }

        @PostMapping("/route")
        public SmartNotificationFilter.Decision route(@RequestBody RouteRequest req) {
            SmartNotificationFilter.Level level = SmartNotificationFilter.Level.valueOf(
                (req.level() == null ? "NORMAL" : req.level()).toUpperCase());
            return filter.route(level, req.source(), req.text());
        }

        public record RouteRequest(String level, String source, String text) {
        }
    }

    @RestController
    @RequestMapping("/api/finance")
    public static class FinanceController {
        private final FinancialOverviewService finance;

        public FinanceController(FinancialOverviewService finance) {
            this.finance = finance;
        }

        @PostMapping("/overview")
        public FinancialOverviewService.Overview overview(@RequestBody OverviewRequest req) {
            return finance.overview(req.holdings(), req.monthlySpend(), req.budget());
        }

        public record OverviewRequest(List<FinancialOverviewService.Holding> holdings,
                                      double monthlySpend, double budget) {
        }
    }
}
