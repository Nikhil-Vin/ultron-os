package com.ultron.workers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Registry of all {@link Worker} beans (L2). Spring injects every worker on the classpath;
 * the {@link com.ultron.kernel.Kernel} looks them up by name to dispatch.
 */
@Component
public class WorkerRegistry {

    private final Map<String, Worker> workers;

    public WorkerRegistry(List<Worker> workerBeans) {
        this.workers = workerBeans.stream()
            .collect(Collectors.toUnmodifiableMap(Worker::name, Function.identity()));
    }

    public Worker get(String name) {
        Worker worker = workers.get(name);
        if (worker == null) {
            throw new IllegalArgumentException("No worker registered with name: " + name);
        }
        return worker;
    }

    public boolean has(String name) {
        return workers.containsKey(name);
    }

    public Set<String> names() {
        return workers.keySet();
    }
}
