package com.ultron.intelligence.meeting;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import com.ultron.memory.MemoryService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Meeting intelligence (Section 9.5). Builds a pre-meeting brief from the knowledge base, and turns
 * a post-call transcript into extracted action items that are archived to memory.
 */
@Service
public class MeetingIntelligenceService {

    private final RagService rag;
    private final BrainSelector brain;
    private final MemoryService memoryService;

    public MeetingIntelligenceService(RagService rag, BrainSelector brain, MemoryService memoryService) {
        this.rag = rag;
        this.brain = brain;
        this.memoryService = memoryService;
    }

    /** Pre-meeting brief: relevant context + suggested talking points. */
    public Brief preMeetingBrief(String title, List<String> attendees) {
        String topic = title + " " + String.join(" ", attendees == null ? List.of() : attendees);
        List<RetrievedItem> context = rag.retrieve(topic, 5);
        String contextText = context.isEmpty() ? "(no prior context found)"
            : context.stream().map(c -> "- " + c.content()).collect(Collectors.joining("\n"));
        String talkingPoints = brain.think(
            "Prepare 3 concise talking points for a meeting titled '" + title + "' with "
                + (attendees == null ? "no listed attendees" : String.join(", ", attendees))
                + ". Use only this context:\n" + contextText);
        return new Brief(title, context.size(), talkingPoints);
    }

    /** Post-call: extract action items from a transcript and archive a summary to memory. */
    public PostCall capture(String title, String transcript) {
        String extracted = brain.think(
            "From this meeting transcript, list the action items as short imperative lines.\n\n" + transcript);
        List<String> items = Arrays.stream(extracted.split("\\r?\\n"))
            .map(String::strip)
            .filter(s -> !s.isBlank())
            .map(s -> s.replaceFirst("^[-*\\d.\\)\\s]+", ""))
            .filter(s -> !s.isBlank())
            .toList();
        memoryService.save("Meeting '" + title + "' — " + items.size() + " action items: "
            + String.join("; ", items), "MEETING", "meeting-intelligence", "meeting,actions");
        return new PostCall(title, items);
    }

    public record Brief(String title, int contextItems, String talkingPoints) {
    }

    public record PostCall(String title, List<String> actionItems) {
    }
}
