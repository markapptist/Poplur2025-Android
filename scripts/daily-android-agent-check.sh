#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/build/agent-runs"
mkdir -p "${LOG_DIR}"

STAMP="$(date +"%Y%m%d-%H%M%S")"
LOG_FILE="${LOG_DIR}/android-agent-${STAMP}.log"
LATEST_LOG="${LOG_DIR}/latest.log"

cd "${ROOT_DIR}"

{
  echo "Poplur Android daily agent run"
  echo "Started: $(date)"
  echo "Workspace: ${ROOT_DIR}"
  echo
} | tee "${LOG_FILE}"

if command -v caffeinate >/dev/null 2>&1; then
  caffeinate -dimsu "${ROOT_DIR}/scripts/android-agent-check.sh" --monkey-events 1500 2>&1 | tee -a "${LOG_FILE}"
else
  "${ROOT_DIR}/scripts/android-agent-check.sh" --monkey-events 1500 2>&1 | tee -a "${LOG_FILE}"
fi

STATUS="${PIPESTATUS[0]}"

{
  echo
  echo "Finished: $(date)"
  echo "Exit status: ${STATUS}"
} | tee -a "${LOG_FILE}"

ln -sf "${LOG_FILE}" "${LATEST_LOG}"
exit "${STATUS}"
