#!/usr/bin/env bash
# Wrapper around the JetBrains Writerside builder.
# Starts a virtual X display on demand and forwards all arguments to
# helpbuilderinspect, which is the headless documentation builder.
#
# Usage:
#   writerside <module>/<instance> [output-dir] [-- additional helpbuilderinspect flags]
#
# Example:
#   writerside Writerside/hi $WORKSPACE_DIR/output
set -euo pipefail

# Workspace path is injected by devcontainer.json's containerEnv (resolved
# from ${containerWorkspaceFolder}). Fall back to /workspace for ad-hoc
# invocations on hosts that don't set it.
: "${WORKSPACE_DIR:=/workspace}"

export DISPLAY="${DISPLAY:-:99}"

if ! pgrep -x Xvfb >/dev/null 2>&1; then
    Xvfb "$DISPLAY" -screen 0 1024x768x24 >/dev/null 2>&1 &
    # Wait briefly for Xvfb to come up before launching IntelliJ headless.
    for _ in 1 2 3 4 5; do
        if xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then break; fi
        sleep 0.2
    done
fi

if [[ $# -eq 0 ]]; then
    exec /opt/builder/bin/idea.sh helpbuilderinspect --help
fi

# Convenience mode: `writerside <module>/<instance> [output]`
if [[ "${1:-}" == */* && "${1:-}" != -* ]]; then
    module_instance="$1"; shift
    output_dir="${1:-$WORKSPACE_DIR/artifacts/help}"
    if [[ $# -ge 1 && "${1:-}" != -* ]]; then shift; fi
    source_dir="${WRITERSIDE_SOURCE_DIR:-$WORKSPACE_DIR}"
    runner="${WRITERSIDE_RUNNER:-other}"

    exec /opt/builder/bin/idea.sh helpbuilderinspect \
        --source-dir "$source_dir" \
        --product "$module_instance" \
        --output-dir "$output_dir" \
        --runner "$runner" \
        "$@"
fi

# Pass-through mode: caller supplies all flags directly.
exec /opt/builder/bin/idea.sh helpbuilderinspect "$@"