package com.ultron.api;

import com.ultron.kernel.Kernel;
import com.ultron.workers.WorkerRequest;
import com.ultron.workers.WorkerResult;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/brief} — generate the overnight brief by dispatching the Sentinel worker
 * through the {@link Kernel}. READ-level: no approval required.
 */
@RestController
@RequestMapping("/api/brief")
public class BriefController {

    private final Kernel kernel;

    public BriefController(Kernel kernel) {
        this.kernel = kernel;
    }

    @PostMapping
    public BriefResponse brief() {
        WorkerResult result = kernel.dispatch("sentinel",
            new WorkerRequest("brief", Map.of("trigger", "api")));
        return new BriefResponse(result.success(), result.summary(), result.detail());
    }

    public record BriefResponse(boolean success, String brief, String detail) {
    }
}
