/**
 * Converts technical errors into human-readable messages with actionable instructions
 */
export function humanizeError(error: unknown): string {
  const err = String(error);
  
  // Backend offline
  if (err.includes("Failed to fetch") || err.includes("NetworkError") || err.includes("ERR_CONNECTION")) {
    return "Backend offline. Start the backend:\n\ncd backend && ./mvnw spring-boot:run\n\nOr on Windows: backend\\mvnw.cmd spring-boot:run";
  }
  
  // 404 errors
  if (err.includes("404")) {
    return "Endpoint not found (404). This feature may require:\n\n" +
           "1. Backend restart after recent code changes\n" +
           "2. Verify you're running the latest version\n" +
           "3. Check backend logs for errors";
  }
  
  // 500 errors
  if (err.includes("500")) {
    return "Server error (500). Check backend logs:\n\n" +
           "cd backend && tail -f logs/spring.log\n\n" +
           "Common causes:\n" +
           "- Database not running (start PostgreSQL or use H2)\n" +
           "- Missing API keys in .env\n" +
           "- LLM provider unreachable";
  }
  
  // ai-layer errors
  if (err.includes("ai-layer") || err.includes("extraction failed")) {
    return "ai-layer is not running. Start it with:\n\n" +
           "cd ai-layer\n" +
           "pip install -r requirements.txt\n" +
           "python main.py\n\n" +
           "ai-layer handles URL ingestion, PDF extraction, and advanced embeddings.";
  }
  
  // Ollama errors
  if (err.includes("ollama") || err.includes("11434")) {
    return "Ollama is not running. Start it with:\n\n" +
           "ollama serve\n\n" +
           "Or install from: https://ollama.ai\n\n" +
           "Ultron will fall back to heuristic mode if Ollama is unavailable.";
  }
  
  // Authentication errors
  if (err.includes("401") || err.includes("unauthorized")) {
    return "Authentication required. If ULTRON_API_KEY is set in backend .env:\n\n" +
           "1. Go to Settings\n" +
           "2. Enter your API key\n" +
           "3. Reload the page";
  }
  
  // Timeout errors
  if (err.includes("timeout") || err.includes("ETIMEDOUT")) {
    return "Request timed out. This might mean:\n\n" +
           "1. LLM provider is slow or down\n" +
           "2. Network connectivity issues\n" +
           "3. Large request taking too long\n\n" +
           "Try again or switch to a faster provider in Settings.";
  }
  
  // Rate limiting
  if (err.includes("429") || err.includes("rate limit")) {
    return "Rate limit exceeded. You've hit the API quota for your provider.\n\n" +
           "Options:\n" +
           "1. Wait a few minutes and try again\n" +
           "2. Switch to a different provider in Settings\n" +
           "3. Use Ollama (local, no limits)";
  }
  
  // Generic fallback with the actual error
  return `Error: ${err}\n\nIf this persists:\n` +
         "1. Check backend logs\n" +
         "2. Verify all services are running\n" +
         "3. See docs/SETUP.md for troubleshooting";
}

/**
 * Extracts setup instructions based on missing service
 */
export function getSetupInstructions(service: "backend" | "ai-layer" | "ollama" | "postgres"): string {
  const instructions = {
    backend: "Start the backend:\n\n" +
             "cd backend\n" +
             "./mvnw spring-boot:run\n\n" +
             "On Windows: mvnw.cmd spring-boot:run",
    
    "ai-layer": "Start ai-layer:\n\n" +
                "cd ai-layer\n" +
                "pip install -r requirements.txt\n" +
                "python main.py\n\n" +
                "Runs on http://localhost:5000",
    
    ollama: "Install and start Ollama:\n\n" +
            "1. Download from https://ollama.ai\n" +
            "2. Run: ollama serve\n" +
            "3. Pull a model: ollama pull llama2\n\n" +
            "Ollama provides local LLM and embeddings.",
    
    postgres: "Start PostgreSQL:\n\n" +
              "Option 1 - Docker:\n" +
              "docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres\n\n" +
              "Option 2 - Use H2 (embedded):\n" +
              "Set spring.profiles.active=h2 in application.yml"
  };
  
  return instructions[service];
}
