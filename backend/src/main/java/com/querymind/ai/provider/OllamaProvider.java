package com.querymind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querymind.ai.service.AiProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Second AiProvider adapter: local Ollama (fully offline demo path, no
 * billing/API key needed — proves the AiProvider abstraction with two real,
 * differently-hosted providers per phases.md Phase4).
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaProvider implements AiProvider {

    private final String baseUrl;
    private final String model;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaProvider(
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.model:llama3.1}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public String generateSql(String schemaContext, String naturalLanguageQuestion) {
        return callOllama(PromptTemplates.sqlGenerationPrompt(schemaContext, naturalLanguageQuestion));
    }

    @Override
    public String explainResult(String question, String sql, String resultSummary) {
        return callOllama(PromptTemplates.explanationPrompt(question, sql, resultSummary));
    }

    private String callOllama(String prompt) {
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "model", model, "prompt", prompt, "stream", false));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("response").asText().trim();
        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama call failed: " + e.getMessage(), e);
        }
    }
}
