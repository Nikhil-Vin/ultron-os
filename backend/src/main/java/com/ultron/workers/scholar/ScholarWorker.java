package com.ultron.workers.scholar;

import com.ultron.intelligence.rag.RagService;
import com.ultron.workers.Worker;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Scholar (L2 — The Staff). Researches a question by retrieving the owner's memories + learned
 * skills (RAG, L3) and producing a grounded answer. READ-level: it never mutates anything.
 */
@Component
public class ScholarWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(ScholarWorker.class);

    private final RagService rag;

    public ScholarWorker(RagService rag) {
        this.rag = rag;
    }

    @Override
    public String name() {
        return "scholar";
    }

    /**
     * Supported kinds:
     * <ul>
     *   <li>{@code research} / {@code ask} — params: {@code question} (required), {@code topK} (optional int).</li>
     * </ul>
     */
    @Override
    public WorkerResult handle(WorkerRequest request) {
        Map<String, Object> p = request.params();
        String question = str(p.get("question"));
        if (question == null || question.isBlank()) {
            return WorkerResult.fail("scholar: 'question' is required");
        }
        int topK = asInt(p.get("topK"), 5);
        RagService.RagAnswer result = rag.answer(question, topK);
        String sources = result.context().stream()
            .map(item -> item.kind() + ":" + item.title())
            .collect(Collectors.joining(", "));
        log.info("Scholar answered question, sources={}", result.context().size());
        return WorkerResult.ok(result.answer(),
            "sources=[" + sources + "]");
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int asInt(Object o, int fallback) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(o.toString().trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
