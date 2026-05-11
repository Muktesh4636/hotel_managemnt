#!/usr/bin/env bash
# Backwards-compatible: ./deploy/push_to_server.sh user@host
set -euo pipefail
HOST="${1:?Usage: $0 user@host   (example: $0 root@72.61.148.117)}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/deploy.sh" "$HOST"
