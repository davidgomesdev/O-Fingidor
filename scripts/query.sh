#!/bin/bash
# Sends a question to the backend and streams only the response text.
#
# Usage: ./scripts/query.sh [-p persona] 'quem foi Fernando Pessoa?'
#   -p  persona code name (default: fernando_pessoa)
#       o_fingidor, fernando_pessoa, alberto_caeiro, alvaro_de_campos, ricardo_reis, bernardo_soares
# Env: PESSOA_URL (default: http://127.0.0.1:8080)

set -euo pipefail

persona="fernando_pessoa"

while getopts "p:h" opt; do
  case "$opt" in
    p) persona="$OPTARG" ;;
    *) sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
  esac
done
shift $((OPTIND - 1))

if [ $# -lt 1 ] || [ -z "$1" ]; then
  echo "Usage: $0 [-p persona] '<question>'" >&2
  exit 1
fi

url="${PESSOA_URL:-http://127.0.0.1:8080}/pensa/conversation"

curl --silent --show-error --no-buffer --fail-with-body \
  -X PUT "$url" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/x-ndjson' \
  --data "$(jq -n --arg input "$1" --arg persona "$persona" '{input: $input, persona: $persona}')" |
  jq --unbuffered -Rj 'fromjson? | select(.type == "token") | .value'

echo
