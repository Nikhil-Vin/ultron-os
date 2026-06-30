package com.ultron.workers.planner;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Planner (L2 — The Staff). Turns a goal + a list of tasks into a prioritised, reasoned plan.
 * It pulls relevant memories/skills (RAG, L3) as context and asks the active brain to order the
 * work. READ-level: planning proposes, it never acts.
 */
@Component
public class PlannerWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(PlannerWorker.class);

    private final BrainSelector brain;
    private final RagService rag;

    public PlannerWorker(BrainSelector brain, RagService rag) {
        this.brain = brain;
        this.rag = rag;
    }

    @Override
    public String name() {
        return "planner";
    }

    /**
     * Supported kinds:
     * <ul>
     *   <li>{@code plan} — params: {@code goal} (string) and/or {@code tasks} (collection of strings).</li>
     * </ul>
     */
    @Override
    public WorkerResult handle(WorkerRequest request) {
        Map<String, Object> p = request.params();
        String goal = str(p.get("goal"));
        List<String> tasks = asStringList(p.get("tasks"));
        if ((goal == null || goal.isBlank()) && tasks.isEmpty()) {
            return WorkerResult.fail("planner: provide a 'goal' and/or 'tasks'");
        }

        String focus = (goal == null || goal.isBlank()) ? String.join("; ", tasks) : goal;
        List<RetrievedItem> context = rag.retrieve(focus, 3);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a planning assistant. Produce a short, prioritised, numbered plan.\n");
        if (goal != null && !goal.isBlank()) {
            prompt.append("Goal: ").append(goal).append('\n');
        }
        if (!tasks.isEmpty()) {
            prompt.append("Candidate tasks: ").append(String.join(", ", tasks)).append('\n');
        }
        if (!context.isEmpty()) {
            prompt.append("Relevant context:\n");
            for (RetrievedItem item : context) {
                prompt.append(" - ").append(item.content()).append('\n');
            }
        }

        String plan = brain.think(prompt.toString());
        log.info("Planner produced a plan for {} task(s) with {} context item(s)", tasks.size(), context.size());
        return WorkerResult.ok(plan, "tasks=" + tasks.size() + " context=" + context.size());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null && !item.toString().isBlank()) {
                    out.add(item.toString());
                }
            }
        } else if (o instanceof String s && !s.isBlank()) {
            for (String part : s.split("[,;\\n]")) {
                if (!part.isBlank()) {
                    out.add(part.trim());
                }
            }
        }
        return out;
    }
}
