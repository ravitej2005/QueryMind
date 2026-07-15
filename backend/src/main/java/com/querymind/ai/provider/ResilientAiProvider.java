package com.querymind.ai.provider;

import com.querymind.ai.service.AiProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Wraps the active AiProvider with timeout/retry/circuit-breaker so a flaky
 * provider degrades to a clear error rather than hanging the request
 * (prd.md §5 Reliability / phases.md Phase4).
 */
@Component
public class ResilientAiProvider {

    private final AiProvider delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientAiProvider(List<AiProvider> providers) {
        if (providers.isEmpty()) {
            throw new IllegalStateException("No AiProvider bean is active; check ai.provider config");
        }
        this.delegate = providers.get(0); // exactly one is active via @ConditionalOnProperty
        this.circuitBreaker = CircuitBreaker.of(delegate.name(), CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build());
        this.retry = Retry.of(delegate.name(), RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .build());
    }

    public String generateSql(String schemaContext, String question) {
        return runProtected(() -> delegate.generateSql(schemaContext, question));
    }

    public String explainResult(String question, String sql, String resultSummary) {
        return runProtected(() -> delegate.explainResult(question, sql, resultSummary));
    }

    public String activeProviderName() {
        return delegate.name();
    }

    private String runProtected(Supplier<String> call) {
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(retry, call));
        try {
            return decorated.get();
        } catch (Exception e) {
            throw new AiProviderUnavailableException(
                    "AI provider (" + delegate.name() + ") is currently unavailable: " + e.getMessage());
        }
    }
}
