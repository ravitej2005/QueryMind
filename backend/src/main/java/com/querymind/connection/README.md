# connection module

Responsibility: connect/test/list/delete user database connections,
AES-256-GCM credential encryption at rest, pluggable DatabaseProvider
(MySqlProvider today), per-connection HikariDataSource lifecycle, and
schema introspection cached in Redis.

Public service interface: `ConnectionService`. Exposes DataSource handles
and DTO schema snapshots to other modules (e.g. `query`) — never the
DatabaseConnection entity itself (module boundary rule).
