package com.ultron.api;

import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/ask} — answer a question grounded in the owner's memories + learned skills
 * (RAG, L3). READ-level: no approval required.
 */
@RestController
@RequestMapping("/api/ask")
public class ScholarController {

    private final RagService rag;

    public ScholarController(RagService rag) {
        this.rag = rag;
    }

    @PostMapping
    public AskResponse ask(@RequestBody AskRequest request) {
        RagService.RagAnswer answer = rag.answer(request.question(), request.topK() > 0 ? request.topK() : 5);
        return new AskResponse(answer.answer(), answer.context());
    }

    public record AskRequest(@NotBlank String question, int topK) {
    }

    public record AskResponse(String answer, List<RetrievedItem> context) {
    }
}
