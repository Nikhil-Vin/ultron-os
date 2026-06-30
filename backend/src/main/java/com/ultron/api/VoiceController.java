package com.ultron.api;

import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.governance.VoiceIdGate;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.psychology.IntentClassifier;
import com.ultron.intelligence.psychology.IntentClassifier.Intent;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import com.ultron.intelligence.voice.SpokenResponseFormatter;
import com.ultron.intelligence.voice.VoiceProfileManager;
import com.ultron.kernel.AgentLoop;
import com.ultron.kernel.WorkModeManager;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Voice API (L0 — Section 9.9). Provides:
 * <ul>
 *   <li>{@code POST /api/voice/ask} — SSE: streams the grounded answer sentence-by-sentence so the
 *       Python voice agent can synthesize TTS while the LLM is still generating.</li>
 *   <li>{@code POST /api/voice/command} — the spoken-command path: classifies intent, enforces the
 *       {@link VoiceIdGate} for CRITICAL actions, and runs the {@link AgentLoop}.</li>
 *   <li>{@code POST /api/voice/say} — markdown→speech formatting for arbitrary text.</li>
 *   <li>work-mode + voice-profile control, and status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final Logger log = LoggerFactory.getLogger(VoiceController.class);

    private final RagService rag;
    private final BrainSelector brain;
    private final SpokenResponseFormatter formatter;
    private final WorkModeManager workMode;
    private final VoiceProfileManager voiceProfiles;
    private final VoiceIdGate voiceIdGate;
    private final IntentClassifier intentClassifier;
    private final AgentLoop agentLoop;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "voice-sse");
        t.setDaemon(true);
        return t;
    });

    public VoiceController(RagService rag, BrainSelector brain, SpokenResponseFormatter formatter,
                           WorkModeManager workMode, VoiceProfileManager voiceProfiles,
                           VoiceIdGate voiceIdGate, IntentClassifier intentClassifier, AgentLoop agentLoop) {
        this.rag = rag;
        this.brain = brain;
        this.formatter = formatter;
        this.workMode = workMode;
        this.voiceProfiles = voiceProfiles;
        this.voiceIdGate = voiceIdGate;
        this.intentClassifier = intentClassifier;
        this.agentLoop = agentLoop;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", workMode.current().name());
        body.put("voiceProfile", voiceProfiles.active());
        body.put("brain", brain.status());
        return body;
    }

    /** Streamed, grounded answer. Emits SSE events: {@code sentence} (n times) then {@code done}. */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody AskRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        WorkModeManager.WorkMode mode = workMode.current();
        sseExecutor.execute(() -> {
            try {
                List<RetrievedItem> context = rag.retrieve(request.question(), request.topK() > 0 ? request.topK() : 5);
                emitter.send(SseEmitter.event().name("context")
                    .data(Map.of("count", context.size())));

                String prompt = buildPrompt(request.question(), context);
                SentenceStreamer streamer = new SentenceStreamer(sentence -> {
                    try {
                        emitter.send(SseEmitter.event().name("sentence")
                            .data(formatter.format(sentence, mode)));
                    } catch (Exception e) {
                        log.debug("SSE send failed: {}", e.getMessage());
                    }
                });

                brain.streamThink(prompt, streamer::accept);
                streamer.flushRemainder();
                emitter.send(SseEmitter.event().name("done").data("end"));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("voice/ask stream error: {}", ex.getMessage());
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    /**
     * Spoken command path. Classifies intent; CRITICAL actions must pass the voice biometric gate
     * (Resemblyzer similarity supplied by the voice layer) before the agent loop is allowed to act.
     */
    @PostMapping("/command")
    public Map<String, Object> command(@RequestBody CommandRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        Intent intent = intentClassifier.classify(request.instruction());
        body.put("intent", intent.name());

        boolean critical = intent == Intent.TRADE_LIVE;
        if (critical) {
            VoiceIdGate.Result vid = voiceIdGate.verify(
                new ProposedAction("voice.command", RiskLevel.CRITICAL, request.instruction(), "voice"),
                request.similarity());
            body.put("voiceId", Map.of("passed", vid.passed(), "reason", vid.reason(), "auditId", vid.auditId()));
            if (!vid.passed()) {
                body.put("acted", false);
                body.put("spoken", formatter.format(
                    "I can't run that. Voice verification didn't pass, so the critical action is blocked."));
                return body;
            }
        }

        // Voice-verified (or non-critical) → run the loop; CRITICAL gets explicit approval here.
        AgentLoop.AgentTrace trace = agentLoop.run(request.instruction(), critical);
        body.put("worker", trace.worker());
        body.put("risk", trace.risk());
        body.put("decision", trace.decision());
        body.put("acted", trace.acted());
        body.put("spoken", formatter.format(trace.result()));
        return body;
    }

    @PostMapping("/say")
    public Map<String, Object> say(@RequestBody SayRequest request) {
        return Map.of("spoken", formatter.format(request.text()));
    }

    @PostMapping("/mode")
    public Map<String, Object> setMode(@RequestBody ModeRequest request) {
        WorkModeManager.WorkMode resolved = request.mode() != null
            ? WorkModeManager.WorkMode.valueOf(request.mode().toUpperCase())
            : workMode.fromPhrase(request.phrase());
        if (resolved != null) {
            workMode.setMode(resolved);
        }
        return Map.of("mode", workMode.current().name(), "behavior", workMode.behavior());
    }

    @PostMapping("/profile")
    public Map<String, Object> setProfile(@RequestBody ProfileRequest request) {
        VoiceProfileManager.VoiceProfile p = request.id() != null
            ? voiceProfiles.select(request.id())
            : voiceProfiles.selectFromPhrase(request.phrase());
        return Map.of("voiceProfile", p == null ? voiceProfiles.active() : p);
    }

    private static String buildPrompt(String question, List<RetrievedItem> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Answer the question using ONLY the context below. ")
          .append("If the context is insufficient, say so plainly.\n\nContext:\n");
        if (context.isEmpty()) {
            sb.append("(no relevant memories or skills found)\n");
        } else {
            int i = 1;
            for (RetrievedItem item : context) {
                sb.append(i++).append(". [").append(item.kind()).append("] ").append(item.content()).append('\n');
            }
        }
        sb.append("\nQuestion: ").append(question == null ? "" : question.strip());
        return sb.toString();
    }

    /** Buffers streamed tokens and flushes complete sentences to a consumer (for sentence-chunked TTS). */
    private static final class SentenceStreamer {
        private final java.util.function.Consumer<String> onSentence;
        private final StringBuilder buffer = new StringBuilder();

        SentenceStreamer(java.util.function.Consumer<String> onSentence) {
            this.onSentence = onSentence;
        }

        void accept(String token) {
            buffer.append(token);
            int idx;
            while ((idx = indexOfSentenceEnd(buffer)) >= 0) {
                String sentence = buffer.substring(0, idx + 1).strip();
                buffer.delete(0, idx + 1);
                if (!sentence.isBlank()) {
                    onSentence.accept(sentence);
                }
            }
        }

        void flushRemainder() {
            String rest = buffer.toString().strip();
            if (!rest.isBlank()) {
                onSentence.accept(rest);
            }
            buffer.setLength(0);
        }

        private static int indexOfSentenceEnd(CharSequence s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c == '.' || c == '!' || c == '?')
                    && (i + 1 >= s.length() || Character.isWhitespace(s.charAt(i + 1)))) {
                    return i;
                }
            }
            return -1;
        }
    }

    // --- DTOs ---
    public record AskRequest(@NotBlank String question, int topK) {
    }

    public record CommandRequest(@NotBlank String instruction, Double similarity) {
    }

    public record SayRequest(@NotBlank String text) {
    }

    public record ModeRequest(String mode, String phrase) {
    }

    public record ProfileRequest(String id, String phrase) {
    }
}
