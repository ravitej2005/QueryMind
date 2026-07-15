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
 * Default AiProvider adapter: Google Gemini (free tier, no billing setup
 * required to run the project — prd.md FR8/memory.md §2). This is the ONLY
 * place a Gemini-specific API call may appear.
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiProvider implements AiProvider {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiProvider(
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.model}") String model,
            @Value("${ai.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public String generateSql(String schemaContext, String naturalLanguageQuestion) {
        return callGemini(PromptTemplates.sqlGenerationPrompt(schemaContext, naturalLanguageQuestion));
    }

    @Override
    public String explainResult(String question, String sql, String resultSummary) {
        return callGemini(PromptTemplates.explanationPrompt(question, sql, resultSummary));
    }

    private String callGemini(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured. Set it in the environment to use the Gemini provider.");
        }
        try {
            String url = "%s/models/%s:generateContent?key=%s".formatted(baseUrl, model, apiKey);
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "contents", java.util.List.of(java.util.Map.of(
                            "parts", java.util.List.of(java.util.Map.of("text", prompt))))));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Gemini API returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/candidates/0/content/parts/0/text").asText().trim();
        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini call failed: " + e.getMessage(), e);
        }
    }
}
