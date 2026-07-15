package com.querymind.connection.datasource;

import com.querymind.connection.domain.DatabaseConnection;
import com.querymind.connection.provider.DatabaseProvider;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lazily creates and caches a HikariDataSource per connection id, evicting
 * idle ones. Never uses the application's own MySQL datasource to run
 * user/AI-generated queries (memory.md §5).
 */
@Component
public class ConnectionDataSourceRegistry {

    private record Entry(DataSource dataSource, Instant lastUsed) {}

    private static final long IDLE_EVICT_MINUTES = 15;

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public DataSource getOrCreate(
            DatabaseConnection connection, String plaintextPassword, DatabaseProvider provider) {
        Entry entry = cache.compute(connection.getId(), (id, existing) -> {
            if (existing != null) {
                return new Entry(existing.dataSource(), Instant.now());
            }
            DataSource ds = provider.createDataSource(connection, plaintextPassword);
            return new Entry(ds, Instant.now());
        });
        return entry.dataSource();
    }

    public void evict(UUID connectionId) {
        Entry entry = cache.remove(connectionId);
        closeIfHikari(entry);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictIdle() {
        Instant cutoff = Instant.now().minusSeconds(IDLE_EVICT_MINUTES * 60);
        cache.entrySet().removeIf(e -> {
            boolean idle = e.getValue().lastUsed().isBefore(cutoff);
            if (idle) closeIfHikari(e.getValue());
            return idle;
        });
    }

    private void closeIfHikari(Entry entry) {
        if (entry != null && entry.dataSource() instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }
}
