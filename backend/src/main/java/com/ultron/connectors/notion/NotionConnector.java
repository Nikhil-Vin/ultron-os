package com.ultron.connectors.notion;

import com.ultron.config.ConnectorProperties;
import com.ultron.connectors.ConnectorResponse;
import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.skills.Skill;
import com.ultron.skills.SkillService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Notion connector (L5). Read pages (READ), write/append (HIGH, gated), and ingest a Notion page as
 * a skill (Section 6 crossover). Opt-in via {@code ultron.connectors.notion-token}.
 */
@Component
public class NotionConnector {

    private static final Logger log = LoggerFactory.getLogger(NotionConnector.class);
    private static final String BASE = "https://api.notion.com/v1";
    private static final String VERSION = "2022-06-28";

    private final ConnectorProperties props;
    private final ApprovalGate approvalGate;
    private final SkillService skillService;
    private final RestClient client = RestClient.create();

    public NotionConnector(ConnectorProperties props, ApprovalGate approvalGate, SkillService skillService) {
        this.props = props;
        this.approvalGate = approvalGate;
        this.skillService = skillService;
    }

    public boolean isConfigured() {
        return ConnectorProperties.set(props.getNotionToken());
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse readPage(String pageId) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Notion");
        }
        try {
            Map<String, Object> res = client.get()
                .uri(BASE + "/blocks/{id}/children?page_size=100", pageId)
                .header("Authorization", "Bearer " + props.getNotionToken())
                .header("Notion-Version", VERSION)
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("blocks", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Notion read failed: " + ex.getMessage());
        }
    }

    /** Ingest a Notion page's plain text as a skill (LOW intake inside SkillService). */
    public ConnectorResponse ingestAsSkill(String pageId, String skillName) {
        ConnectorResponse page = readPage(pageId);
        if (!page.connected() || !page.ok()) {
            return page;
        }
        String text = page.data() == null ? "" : page.data().toString();
        Skill skill = skillService.intake(skillName, "Imported from Notion page " + pageId,
            text.substring(0, Math.min(text.length(), 18000)), "notion", "notion:" + pageId);
        return ConnectorResponse.ok("skill-ingested", Map.of("skillId", skill.getId().toString()));
    }

    @SuppressWarnings("unchecked")
    public ConnectorResponse append(String pageId, String text, boolean humanApproved) {
        if (!isConfigured()) {
            return ConnectorResponse.notConnected("Notion");
        }
        ApprovalGate.GateResult gate = approvalGate.evaluate(
            new ProposedAction("notion.append", RiskLevel.HIGH, "Append to Notion page " + pageId, "notion"),
            humanApproved);
        if (!gate.allowed()) {
            return ConnectorResponse.blocked("Notion write blocked by approval gate (decision=" + gate.decision() + ").");
        }
        try {
            Map<String, Object> block = Map.of("object", "block", "type", "paragraph",
                "paragraph", Map.of("rich_text", new Object[]{Map.of("type", "text", "text", Map.of("content", text))}));
            Map<String, Object> res = client.patch()
                .uri(BASE + "/blocks/{id}/children", pageId)
                .header("Authorization", "Bearer " + props.getNotionToken())
                .header("Notion-Version", VERSION)
                .body(Map.of("children", new Object[]{block}))
                .retrieve().body(Map.class);
            return ConnectorResponse.ok("appended", res);
        } catch (RuntimeException ex) {
            return ConnectorResponse.blocked("Notion write failed: " + ex.getMessage());
        }
    }
}
