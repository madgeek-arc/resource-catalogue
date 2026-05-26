#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" <<-EOSQL
DO
\$\$
BEGIN
  IF NOT EXISTS (
    SELECT FROM pg_catalog.pg_roles WHERE rolname = '$APP_USER'
  ) THEN
    CREATE USER "$APP_USER" WITH PASSWORD '$APP_PASSWORD';
  END IF;
END
\$\$;

SELECT 'CREATE DATABASE "$APP_DB" OWNER "$APP_USER"'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = '$APP_DB'
)\gexec
EOSQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$APP_DB" <<-EOSQL
CREATE EXTENSION IF NOT EXISTS tablefunc;
CREATE EXTENSION IF NOT EXISTS vector;
EOSQL