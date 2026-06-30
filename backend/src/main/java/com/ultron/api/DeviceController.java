package com.ultron.api;

import com.ultron.devices.CommandRouter;
import com.ultron.devices.DeviceRegistry;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.governance.VoiceIdGate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Device API (L0/L5). Lists connected devices and dispatches commands to them over their WebSocket.
 * Governance: READ runs instantly; LOW/HIGH pass the {@link ApprovalGate}; CRITICAL (calls/messages)
 * additionally require a passing {@link VoiceIdGate}. Natural-language commands resolve through the
 * fast {@link CommandRouter} first (sub-LLM latency).
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRegistry registry;
    private final CommandRouter router;
    private final ApprovalGate approvalGate;
    private final VoiceIdGate voiceIdGate;

    public DeviceController(DeviceRegistry registry, CommandRouter router,
                            ApprovalGate approvalGate, VoiceIdGate voiceIdGate) {
        this.registry = registry;
        this.router = router;
        this.approvalGate = approvalGate;
        this.voiceIdGate = voiceIdGate;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return registry.list();
    }

    /** Natural-language command (e.g. "open YouTube on my phone"). Fast-routes simple commands. */
    @PostMapping("/command")
    public Map<String, Object> command(@RequestBody NlCommand req) {
        CommandRouter.Parsed p = router.route(req.text());
        if (p == null) {
            return Map.of("routed", false, "message", "No fast match — route via the agent loop / brain.");
        }
        return dispatch(p.deviceType(), p.action(), p.args(), p.risk(), req.approved(), req.similarity());
    }

    /** Direct command to a specific device id with an explicit action. */
    @PostMapping("/{id}/command")
    public Map<String, Object> direct(@PathVariable String id, @RequestBody DirectCommand req) {
        RiskLevel risk = RiskLevel.valueOf((req.risk() == null ? "READ" : req.risk()).toUpperCase());
        Map<String, Object> gateResult = gate(req.action(), risk, req.approved(), req.similarity());
        if (gateResult != null) {
            return gateResult;
        }
        String cmdId = registry.sendCommand(id, req.action(), req.args(), "approved");
        return result(cmdId, id, req.action(), risk);
    }

    private Map<String, Object> dispatch(String type, String action, Map<String, Object> args,
                                         RiskLevel risk, boolean approved, Double similarity) {
        Map<String, Object> gateResult = gate(action, risk, approved, similarity);
        if (gateResult != null) {
            return gateResult;
        }
        String cmdId = registry.sendToType(type, action, args, "approved");
        Map<String, Object> body = result(cmdId, type, action, risk);
        if (cmdId == null) {
            body.put("message", "No online " + type + " device. Run the agent on that device.");
        }
        return body;
    }

    /** Returns a blocked-response map if the gate denies, else null (allowed). */
    private Map<String, Object> gate(String action, RiskLevel risk, boolean approved, Double similarity) {
        if (risk == RiskLevel.READ) {
            return null;
        }
        if (risk == RiskLevel.CRITICAL) {
            VoiceIdGate.Result vid = voiceIdGate.verify(
                new ProposedAction("device." + action, RiskLevel.CRITICAL, action, "device"), similarity);
            if (!vid.passed()) {
                return Map.of("executed", false, "blocked", "voice-id",
                    "message", "CRITICAL device action blocked — voice verification failed.");
            }
        }
        ApprovalGate.GateResult g = approvalGate.evaluate(
            new ProposedAction("device." + action, risk, action, "device"), approved || risk == RiskLevel.CRITICAL);
        if (!g.allowed()) {
            return Map.of("executed", false, "blocked", "approval",
                "decision", g.decision().toString(), "message", "Action blocked by approval gate.");
        }
        return null;
    }

    private Map<String, Object> result(String cmdId, String target, String action, RiskLevel risk) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("executed", cmdId != null);
        body.put("commandId", cmdId);
        body.put("target", target);
        body.put("action", action);
        body.put("risk", risk.toString());
        return body;
    }

    public record NlCommand(String text, boolean approved, Double similarity) {
    }

    public record DirectCommand(String action, Map<String, Object> args, String risk, boolean approved, Double similarity) {
    }
}
