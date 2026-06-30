package com.ultron.api;

import com.ultron.intelligence.coding.CodeGeneratorService;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Code API (L0 — Section 9.2). Generate projects, review files, refactor code — all grounded in
 * ingested skills and written to disk under the sandboxed output root.
 */
@RestController
@RequestMapping("/api/code")
public class CodeController {

    private final CodeGeneratorService generator;

    public CodeController(CodeGeneratorService generator) {
        this.generator = generator;
    }

    @PostMapping("/generate")
    public CodeGeneratorService.Result generate(@RequestBody GenerateRequest req) {
        return generator.generate(req.prompt(), req.projectName(), req.language());
    }

    @PostMapping("/review")
    public ResponseEntity<CodeGeneratorService.Result> review(@RequestBody ReviewRequest req) {
        try {
            return ResponseEntity.ok(generator.review(req.filePath(), req.code()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refactor")
    public ResponseEntity<CodeGeneratorService.Result> refactor(@RequestBody RefactorRequest req) {
        try {
            return ResponseEntity.ok(generator.refactor(req.filePath(), req.instructions()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record GenerateRequest(@NotBlank String prompt, String projectName, String language) {
    }

    public record ReviewRequest(String filePath, String code) {
    }

    public record RefactorRequest(@NotBlank String filePath, @NotBlank String instructions) {
    }
}
