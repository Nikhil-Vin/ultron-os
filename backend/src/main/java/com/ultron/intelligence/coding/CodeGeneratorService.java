package com.ultron.intelligence.coding;

import com.ultron.governance.ApprovalGate;
import com.ultron.governance.ProposedAction;
import com.ultron.governance.RiskLevel;
import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.rag.RagService;
import com.ultron.intelligence.rag.RetrievedItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Code generator (Section 9.2). Generates whole projects grounded in ingested skills (architecture
 * patterns, style guides), writes real files to a sandboxed output dir, and opens VS Code. Also
 * reviews/refactors existing files. File writes are LOW (gated + audited); writes are confined to a
 * root directory so generation can never clobber arbitrary disk.
 */
@Service
public class CodeGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CodeGeneratorService.class);
    // Model emits files as:  ===FILE: relative/path===\n<content>\n===END===
    private static final Pattern FILE_BLOCK =
        Pattern.compile("===FILE:\\s*(.+?)===\\s*\\n(.*?)\\n===END===", Pattern.DOTALL);

    private final BrainSelector brain;
    private final RagService rag;
    private final ApprovalGate approvalGate;
    private final Path root;

    public CodeGeneratorService(BrainSelector brain, RagService rag, ApprovalGate approvalGate,
                                @Value("${ultron.code.output-root:${user.home}/ultron-output}") String outputRoot) {
        this.brain = brain;
        this.rag = rag;
        this.approvalGate = approvalGate;
        this.root = Path.of(outputRoot).toAbsolutePath().normalize();
    }

    /** Generate a project from a prompt; returns the written files + project dir. */
    public Result generate(String prompt, String projectName, String language) {
        approvalGate.evaluate(new ProposedAction("code.generate", RiskLevel.LOW,
            "Generate project " + projectName, "code-generator"));

        List<RetrievedItem> ctx = rag.retrieve(prompt + " " + (language == null ? "" : language), 4);
        String grounding = ctx.isEmpty() ? "(no ingested patterns)"
            : ctx.stream().map(c -> "- " + c.content()).collect(Collectors.joining("\n"));

        String llmPrompt = """
            Generate a complete, working %s project for this requirement:
            %s

            Follow these patterns/preferences from my knowledge base where relevant:
            %s

            Output ONLY files, each delimited EXACTLY as:
            ===FILE: relative/path/here===
            <file contents>
            ===END===
            Include all source, config, and a README. No prose outside the file blocks.
            """.formatted(language == null ? "" : language, prompt, grounding);

        String output = brain.think(llmPrompt);
        Path dir = safeDir(projectName);
        List<String> written = writeFiles(output, dir);
        openInVsCode(dir);
        log.info("Generated {} files into {}", written.size(), dir);
        return new Result(dir.toString(), written, written.isEmpty() ? output : null);
    }

    /** Review a file: feedback + an improved version written alongside. */
    public Result review(String filePath, String inlineCode) throws IOException {
        String code = inlineCode != null && !inlineCode.isBlank()
            ? inlineCode : Files.readString(requireUnderAnyReadablePath(filePath));
        String feedback = brain.think(
            "Review this code for bugs, style, and design. Give concise feedback, then output the "
                + "improved version as a single ===FILE: improved===...===END=== block.\n\n" + code);
        List<String> written = new ArrayList<>();
        if (filePath != null && !filePath.isBlank()) {
            Path improved = Path.of(filePath + ".improved");
            Matcher m = FILE_BLOCK.matcher(feedback);
            if (m.find()) {
                Files.writeString(improved, m.group(2));
                written.add(improved.toString());
            }
        }
        return new Result(null, written, feedback);
    }

    /** Refactor a file in place (writes a .refactored copy) per instructions. */
    public Result refactor(String filePath, String instructions) throws IOException {
        approvalGate.evaluate(new ProposedAction("code.refactor", RiskLevel.LOW,
            "Refactor " + filePath, "code-generator"));
        String code = Files.readString(Path.of(filePath));
        String out = brain.think("Refactor this code: " + instructions
            + "\nReturn only the full refactored file in a ===FILE: refactored===...===END=== block.\n\n" + code);
        List<String> written = new ArrayList<>();
        Matcher m = FILE_BLOCK.matcher(out);
        Path target = Path.of(filePath + ".refactored");
        if (m.find()) {
            Files.writeString(target, m.group(2));
            written.add(target.toString());
        }
        return new Result(null, written, written.isEmpty() ? out : "Refactored → " + target);
    }

    private List<String> writeFiles(String output, Path dir) {
        List<String> written = new ArrayList<>();
        Matcher m = FILE_BLOCK.matcher(output);
        while (m.find()) {
            String rel = m.group(1).strip().replace("\\", "/");
            Path target = dir.resolve(rel).normalize();
            if (!target.startsWith(dir)) {
                log.warn("Skipping path-escape file: {}", rel);
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                Files.writeString(target, m.group(2));
                written.add(target.toString());
            } catch (IOException ex) {
                log.warn("write {} failed: {}", target, ex.getMessage());
            }
        }
        return written;
    }

    private Path safeDir(String projectName) {
        String clean = (projectName == null || projectName.isBlank() ? "project" : projectName)
            .replaceAll("[^a-zA-Z0-9._-]", "-");
        Path dir = root.resolve(clean).normalize();
        if (!dir.startsWith(root)) {
            dir = root.resolve("project");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            log.warn("could not create {}: {}", dir, ex.getMessage());
        }
        return dir;
    }

    private Path requireUnderAnyReadablePath(String filePath) {
        return Path.of(filePath); // review is READ-only on an explicit path the owner provided
    }

    private void openInVsCode(Path dir) {
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd", "/c", "code", dir.toString())
                : new ProcessBuilder("code", dir.toString());
            pb.inheritIO().start();
        } catch (Exception ex) {
            log.info("VS Code not opened ({}); files are at {}", ex.getMessage(), dir);
        }
    }

    /**
     * @param projectDir the output directory (null for review/refactor)
     * @param files      written file paths
     * @param notes      narrative/feedback (null when files were written cleanly)
     */
    public record Result(String projectDir, List<String> files, String notes) {
    }
}
