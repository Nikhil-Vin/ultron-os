package com.ultron.intelligence;

import com.ultron.config.UltronProperties;
import org.springframework.stereotype.Component;

/** NVIDIA NIM — free dev tier, strong reasoning (DeepSeek-R1). Opt-in via NVIDIA_API_KEY. */
@Component
public class NvidiaBrain extends OpenAiCompatibleBrain {

    public NvidiaBrain(UltronProperties properties) {
        super("nvidia", "https://integrate.api.nvidia.com/v1", properties.getBrain());
    }

    @Override
    protected String apiKey() {
        return config.getNvidiaApiKey();
    }

    @Override
    public String model() {
        return config.getNvidiaModel();
    }
}
