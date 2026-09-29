#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source ./.env.local
  set +a
fi

MYSQL_HOST_PORT="${MYSQL_HOST_PORT:-3306}"
REDIS_HOST_PORT="${REDIS_HOST_PORT:-6379}"

DB_URL_DEFAULT="jdbc:mysql://localhost:${MYSQL_HOST_PORT}/rag_qa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"

export DB_URL="${DB_URL:-$DB_URL_DEFAULT}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="${DB_PASSWORD:-123456}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-$REDIS_HOST_PORT}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-123456}"

if [[ "${1:-}" == "--with-docker" ]]; then
  if [[ -f .env.local ]]; then
    docker compose --env-file .env.local up -d
  else
    docker compose up -d
  fi
fi

mvn -pl rag-admin -am install -DskipTests
exec mvn -f rag-admin/pom.xml spring-boot:run -DskipTests
