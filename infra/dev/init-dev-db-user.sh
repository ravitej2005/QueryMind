#!/bin/bash
# -----------------------------------------------------------------------------
# Development-only: provisions a SEPARATE demo business database
# ("querymind_demo", the e-commerce dataset in /seed-data) and a read-only
# user scoped ONLY to it — never to QueryMind's own app database.
#
# WHY A SEPARATE DATABASE: QueryMind's product story is "connect to YOUR
# existing business database" — it must never look like it's querying its
# own internal tables (users/workspaces/connections/chat_messages/...).
# Giving qm_reader access to the app's own "querymind" DB would make the demo
# accidentally reinforce the exact confusion the product should avoid.
# qm_reader here can see querymind_demo ONLY.
#
# This runs automatically via Docker's /docker-entrypoint-initdb.d/
# mechanism on the FIRST start of the mysql container (when the data volume
# does not yet exist). It does NOT run on subsequent restarts — for a full
# re-seed, run `docker compose down -v` first.
#
# IMPORTANT: The MySQL Docker entrypoint SOURCEs .sh files (`. "$f"`) rather
# than executing them in a subprocess. Do NOT use "set -u" or
# "set -euo pipefail" here — those options would leak into the parent
# entrypoint shell and cause it to fail on optional variables like
# MYSQL_ONETIME_PASSWORD that it references without a default.
#
# This is a DEV convenience only — it must never be mounted in production.
# The 02/03 numbered .sql files (schema + seed data) run automatically
# before this script via the same init mechanism (alphabetical order).
# -----------------------------------------------------------------------------

echo "[init] Creating read-only demo user 'qm_reader', scoped to querymind_demo ONLY..."

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE USER IF NOT EXISTS 'qm_reader'@'%' IDENTIFIED BY 'qm_reader_pass';
    GRANT SELECT, SHOW VIEW ON \`querymind_demo\`.* TO 'qm_reader'@'%';
    FLUSH PRIVILEGES;
EOSQL

echo "[init] Done. Add this as a Data Source in QueryMind:"
echo "       Name     : Demo E-commerce Store"
echo "       Host     : mysql (Docker service name)"
echo "       Port     : 3306"
echo "       Database : querymind_demo"
echo "       User     : qm_reader"
echo "       Password : qm_reader_pass"
echo "       (SELECT + SHOW VIEW only, on querymind_demo ONLY — QueryMind's"
echo "        own app database is not visible to this user, by design.)"
