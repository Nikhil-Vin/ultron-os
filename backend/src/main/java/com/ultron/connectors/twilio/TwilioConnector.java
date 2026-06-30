package com.ultron.connectors.twilio;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Twilio SMS/call connector (L5) — a CONTACT-touching extension point (Section 3 moat). Like live
 * trading, this ships DISABLED: even with credentials, it will not place a real SMS/call unless the
 * owner deliberately sets {@code ultron.connectors.twilio-live-enabled=true} AND the action passes
 * the CRITICAL gate + voice biometric. Never auto-armed on first run.
 */
@Component
public class TwilioConnector {

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;

    public TwilioConnector(ConnectorProperties props, ApprovalGate approvalGate) {
        this.props = props;
        this.approvalGate = approvalGate;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getTwilioAccountSid())
            && ConnectorProperties.set(props.getTwilioAuthToken());
    }

    /** True only when credentials exist AND the owner explicitly enabled live sending. */
    public boolean isLiveEnabled() {
        return isConfigured() && props.isTwilioLiveEnabled();
    }

    /**
     * Attempt to send an SMS. CRITICAL. Even when approved, if live sending isn't deliberately
     * enabled this returns blocked — the real Twilio REST call is intentionally not wired here.
     */
    public ConnectorResponse sendSms(String to, String body, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Twilio");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("twilio.sms", RiskLevel.CRITICAL, "Send SMS to " + to, "twilio"), humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("SMS blocked by approval gate (decision=" + gate.decision() + ").");
        }
        if (!isLiveEnabled()) {
            // ===========================================================================
            // CONTACT-TOUCHING EXTENSION POINT — DELIBERATELY NOT WIRED (Section 3 moat).
            // To actually send, the OWNER sets twilio-live-enabled=true AND implements the real
            // Twilio REST POST (/2010-04-01/Accounts/{sid}/Messages.json) below. Never auto-armed.
            // ===========================================================================
            return ConnectorResponse.blocked(
                "Twilio live sending is not enabled. This is intentional (Section 3 moat): real SMS/calls "
                    + "are an opt-in extension point you wire deliberately. Nothing was sent.");
        }
        // TODO(owner): real Twilio REST call goes here once you enable live sending.
        return ConnectorResponse.blocked("Twilio live send path not implemented by owner yet.");
    }
}
