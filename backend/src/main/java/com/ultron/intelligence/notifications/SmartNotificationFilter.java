package com.ultron.intelligence.notifications;

import com.ultron.kernel.WorkModeManager;
import org.springframework.stereotype.Service;

/**
 * Smart notification filter (Section 9.5). Routes every notification through the current
 * {@link WorkModeManager.WorkMode}: in TRADING/DEEP_WORK only CRITICAL gets through immediately,
 * the rest are queued for the next state transition; relaxed modes surface more.
 */
@Service
public class SmartNotificationFilter {

    public enum Level { LOW, NORMAL, HIGH, CRITICAL }

    public enum Routing { DELIVER_NOW, QUEUE, SUPPRESS }

    private final WorkModeManager workMode;

    public SmartNotificationFilter(WorkModeManager workMode) {
        this.workMode = workMode;
    }

    public Decision route(Level level, String source, String text) {
        WorkModeManager.WorkMode mode = workMode.current();
        Routing routing = decide(level, mode);
        return new Decision(routing, level, mode.name(), source, text);
    }

    private Routing decide(Level level, WorkModeManager.WorkMode mode) {
        boolean terse = mode.behavior().terse(); // TRADING / DEEP_WORK
        if (level == Level.CRITICAL) {
            return Routing.DELIVER_NOW;
        }
        if (terse) {
            // Focused modes: hold everything non-critical for later.
            return level == Level.HIGH ? Routing.QUEUE : Routing.SUPPRESS;
        }
        // Relaxed/expansive modes: deliver high/normal, queue low.
        return level == Level.LOW ? Routing.QUEUE : Routing.DELIVER_NOW;
    }

    public record Decision(Routing routing, Level level, String mode, String source, String text) {
    }
}
