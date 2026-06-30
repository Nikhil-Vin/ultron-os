package com.ultron.api;

import com.ultron.intelligence.BrainSelector;
import com.ultron.intelligence.embedding.EmbedderSelector;
import com.ultron.memory.MemoryRepository;
import com.ultron.skills.SkillRepository;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/system} — real, live vitals for the JARVIS UI (no synthetic numbers): actual JVM
 * CPU load, heap usage, processor count, uptime, and real memory/skill counts. Everything the
 * dashboard shows is sourced here.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final MemoryRepository memories;
    private final SkillRepository skills;
    private final BrainSelector brain;
    private final EmbedderSelector embedder;

    public SystemController(MemoryRepository memories, SkillRepository skills,
                            BrainSelector brain, EmbedderSelector embedder) {
        this.memories = memories;
        this.skills = skills;
        this.brain = brain;
        this.embedder = embedder;
    }

    @GetMapping
    public Map<String, Object> system() {
        Map<String, Object> body = new LinkedHashMap<>();

        double cpu = -1;
        var os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            double load = sun.getCpuLoad();           // 0..1, whole machine
            cpu = load >= 0 ? Math.round(load * 100) : Math.round(sun.getProcessCpuLoad() * 100);
        }
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedMb = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMb = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        body.put("cpuPercent", cpu < 0 ? 0 : (int) cpu);
        body.put("heapUsedMb", usedMb);
        body.put("heapMaxMb", maxMb);
        body.put("heapPercent", maxMb > 0 ? (int) (usedMb * 100 / maxMb) : 0);
        body.put("processors", Runtime.getRuntime().availableProcessors());
        body.put("uptimeSeconds", uptimeMs / 1000);
        body.put("memories", memories.count());
        body.put("skills", skills.count());
        body.put("brain", brain.status().active());
        body.put("brainModel", brain.status().model());
        body.put("embedder", embedder.active().name());
        return body;
    }
}
