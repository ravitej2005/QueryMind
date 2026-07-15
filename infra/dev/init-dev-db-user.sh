#!/bin/bash
# -----------------------------------------------------------------------------
# Development-only: creates a read-only demo MySQL user (qm_reader) that
# developers can use as the QueryMind connection when testing the AI chat.
#
# This script runs automatically via Docker's /docker-entrypoint-initdb.d/
# mechanism on the FIRST start of the mysql container (when the data volume
# does not yet exist). It does NOT run on subsequent restarts.
#
# IMPORTANT: The MySQL Docker entrypoint SOURCEs .sh files (`. "$f"`) rather
# than executing them in a subprocess. Do NOT use "set -u" or "set -euo pipefail"
# here — those options would leak into the parent entrypoint shell and cause it
# to fail on optional variables like MYSQL_ONETIME_PASSWORD that it references
# without a default.
#
# This is a DEV convenience only — it must never be mounted in production.
# -----------------------------------------------------------------------------

DB="${MYSQL_DATABASE:-querymind}"

echo "[init] Creating read-only dev user 'qm_reader' on database '${DB}'..."

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE USER IF NOT EXISTS 'qm_reader'@'%' IDENTIFIED BY 'qm_reader_pass';
    GRANT SELECT, SHOW VIEW ON \`${DB}\`.* TO 'qm_reader'@'%';
    FLUSH PRIVILEGES;
EOSQL

echo "[init] Done. Connection details for QueryMind:"
echo "       Host     : mysql (Docker service name)"
echo "       Port     : 3306"
echo "       Database : ${DB}"
echo "       User     : qm_reader"
echo "       Password : qm_reader_pass"
echo "       (SELECT + SHOW VIEW only — QueryMind security check will pass)"
