#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
KB_ID="${KB_ID:-2}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"
TOP_K="${TOP_K:-6}"
MIN_SCORE="${MIN_SCORE:-0.3}"
QUESTIONS_FILE="${QUESTIONS_FILE:-}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/phase0_qa_snapshot.sh [options]

Options:
  --base-url <url>        API base url (default: http://localhost:8080)
  --kb-id <id>            Knowledge base id (default: 2)
  --username <name>       Login username (default: admin)
  --password <pwd>        Login password (default: admin123)
  --top-k <n>             Retrieval topK for ask API (default: 6)
  --min-score <f>         Retrieval minScore for ask API (default: 0.3)
  --questions-file <path> One question per line (optional)
  -h, --help              Show help

Environment variables are also supported: BASE_URL, KB_ID, USERNAME, PASSWORD, TOP_K, MIN_SCORE, QUESTIONS_FILE
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"; shift 2 ;;
    --kb-id)
      KB_ID="$2"; shift 2 ;;
    --username)
      USERNAME="$2"; shift 2 ;;
    --password)
      PASSWORD="$2"; shift 2 ;;
    --top-k)
      TOP_K="$2"; shift 2 ;;
    --min-score)
      MIN_SCORE="$2"; shift 2 ;;
    --questions-file)
      QUESTIONS_FILE="$2"; shift 2 ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1 ;;
  esac
done

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

read_questions() {
  if [[ -n "$QUESTIONS_FILE" ]]; then
    if [[ ! -f "$QUESTIONS_FILE" ]]; then
      echo "Questions file not found: $QUESTIONS_FILE" >&2
      exit 1
    fi
    mapfile -t QUESTIONS < <(grep -v '^\s*$' "$QUESTIONS_FILE")
  else
    QUESTIONS=(
      "什么是RAG"
      "为什么需要向量化"
      "知识库没有该内容时应如何回答"
    )
  fi

  if [[ ${#QUESTIONS[@]} -eq 0 ]]; then
    echo "No questions found" >&2
    exit 1
  fi
}

login() {
  local login_payload
  login_payload=$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" '{username: $username, password: $password}')

  local login_resp
  login_resp=$(curl -sS -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$login_payload")

  ACCESS_TOKEN=$(echo "$login_resp" | jq -r '.data.accessToken // empty')

  if [[ -z "$ACCESS_TOKEN" ]]; then
    echo "Login failed. Response:" >&2
    echo "$login_resp" >&2
    exit 1
  fi
}

run_snapshot() {
  local ts out_dir summary_file
  ts=$(date +%Y%m%d-%H%M%S)
  out_dir="diagnostics/qa-snapshots/$ts"
  mkdir -p "$out_dir"
  summary_file="$out_dir/summary.md"

  echo "# Phase0 QA Snapshot" > "$summary_file"
  echo >> "$summary_file"
  echo "- baseUrl: $BASE_URL" >> "$summary_file"
  echo "- kbId: $KB_ID" >> "$summary_file"
  echo "- topK: $TOP_K" >> "$summary_file"
  echo "- minScore: $MIN_SCORE" >> "$summary_file"
  echo "- enableCache: false" >> "$summary_file"
  echo >> "$summary_file"
  echo "| idx | question | answerLength | contextCount | retrievedContextCount | removedByBudget | retrievedTopScore | retrievedAvgScore | citationCount |" >> "$summary_file"
  echo "|---|---|---:|---:|---:|---:|---:|---:|---:|" >> "$summary_file"

  local i=0
  for question in "${QUESTIONS[@]}"; do
    i=$((i + 1))
    local payload response_file
    payload=$(jq -n \
      --argjson kbId "$KB_ID" \
      --arg question "$question" \
      --argjson topK "$TOP_K" \
      --argjson minScore "$MIN_SCORE" \
      '{kbId: $kbId, question: $question, topK: $topK, minScore: $minScore, enableCache: false}')

    response_file="$out_dir/q${i}.json"

    curl -sS -X POST "$BASE_URL/api/qa/ask" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d "$payload" \
      | jq '.' > "$response_file"

    local answer_len context_count retrieved_context_count removed_by_budget top_score avg_score citation_count
    answer_len=$(jq -r '.data.answer | if . == null then 0 else length end' "$response_file")
    context_count=$(jq -r '.data.metadata.contextCount // 0' "$response_file")
    retrieved_context_count=$(jq -r '.data.metadata.retrievedContextCount // 0' "$response_file")
    removed_by_budget=$(jq -r '.data.metadata.removedByBudget // 0' "$response_file")
    top_score=$(jq -r '.data.metadata.retrievedTopScore // 0' "$response_file")
    avg_score=$(jq -r '.data.metadata.retrievedAvgScore // 0' "$response_file")
    citation_count=$(jq -r '.data.citations | if . == null then 0 else length end' "$response_file")

    printf '| %s | %s | %s | %s | %s | %s | %s | %s | %s |\n' \
      "$i" "$question" "$answer_len" "$context_count" "$retrieved_context_count" \
      "$removed_by_budget" "$top_score" "$avg_score" "$citation_count" >> "$summary_file"
  done

  echo >> "$summary_file"
  echo "Raw responses are saved under: $out_dir" >> "$summary_file"

  echo "Snapshot completed: $out_dir"
  echo "Summary: $summary_file"
}

main() {
  read_questions
  login
  run_snapshot
}

main
