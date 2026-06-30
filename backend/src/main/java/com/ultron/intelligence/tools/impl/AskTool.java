package com.ultron.intelligence.tools.impl;

import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.tools.Tool;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tool: answer a question grounded in the owner's memories + skills (READ). */
@Component
public class AskTool implements Tool {

    private final RagService rag;

    public AskTool(RagService rag) {
        this.rag = rag;
    }

    @Override
    public String name() {
        return "ask";
    }

    @Override
    public String description() {
        return "Answer a question grounded in the owner's memories and learned skills. Args: question (string), topK (int, optional).";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String question = String.valueOf(args.getOrDefault("question", ""));
        int topK = args.get("topK") instanceof Number n ? n.intValue() : 5;
        RagService.RagAnswer answer = rag.answer(question, topK);
        return Map.of("answer", answer.answer(), "sources", answer.context().size());
    }
}
