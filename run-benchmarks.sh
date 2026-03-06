#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Aether Datafixers - Production Benchmark Suite
# =============================================================================
# Runs all JMH benchmarks with production-quality settings on a dedicated VM.
# Expected total runtime: ~4-8 hours depending on hardware.
#
# Usage:
#   chmod +x run-benchmarks.sh
#   nohup ./run-benchmarks.sh > benchmark-run.log 2>&1 &
#
# Prerequisites:
#   - Java 17+ (JAVA_HOME set or java on PATH)
#   - Maven 3.9.5+
#   - Git (for commit hash capture)
# =============================================================================

# --- Configuration -----------------------------------------------------------

WARMUP_ITERATIONS=5
MEASUREMENT_ITERATIONS=10
FORKS=3
JVM_ARGS="-Xms4G -Xmx4G -XX:+UseG1GC -XX:+AlwaysPreTouch"
RESULT_DIR="benchmark-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RUN_DIR="${RESULT_DIR}/${TIMESTAMP}"
JAR_PATTERN="aether-datafixers-benchmarks/target/aether-datafixers-benchmarks-*-benchmarks.jar"
COMMON_ARGS="-wi ${WARMUP_ITERATIONS} -i ${MEASUREMENT_ITERATIONS} -f ${FORKS} -jvmArgs \"${JVM_ARGS}\""

# --- Helper Functions --------------------------------------------------------

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

die() {
    log "ERROR: $*" >&2
    exit 1
}

run_benchmark() {
    local name="$1"
    local pattern="$2"
    local extra_args="${3:-}"
    local output_file="${RUN_DIR}/${name}.json"

    log "=== Starting: ${name} ==="
    log "Pattern: ${pattern}"
    log "Output:  ${output_file}"

    local start_time=$(date +%s)

    java -jar "${JAR}" \
        "${pattern}" \
        -wi ${WARMUP_ITERATIONS} \
        -i ${MEASUREMENT_ITERATIONS} \
        -f ${FORKS} \
        -jvmArgs "${JVM_ARGS}" \
        -rf json \
        -rff "${output_file}" \
        ${extra_args} \
        2>&1 | tee "${RUN_DIR}/logs/${name}.log"

    local exit_code=${PIPESTATUS[0]}
    local end_time=$(date +%s)
    local duration=$(( end_time - start_time ))
    local minutes=$(( duration / 60 ))
    local seconds=$(( duration % 60 ))

    if [ ${exit_code} -eq 0 ]; then
        log "=== Completed: ${name} in ${minutes}m ${seconds}s ==="
    else
        log "=== FAILED: ${name} after ${minutes}m ${seconds}s (exit code: ${exit_code}) ==="
    fi

    echo "${name},${exit_code},${duration}" >> "${RUN_DIR}/timing.csv"
    return ${exit_code}
}

# --- Pre-flight Checks -------------------------------------------------------

log "============================================="
log "Aether Datafixers Benchmark Suite"
log "============================================="

# Check Java
java -version 2>&1 || die "Java not found. Install Java 17+ and set JAVA_HOME."
JAVA_VERSION=$(java -version 2>&1 | head -1)
log "Java: ${JAVA_VERSION}"

# Check Maven
mvn --version 2>&1 | head -1 || die "Maven not found."
MVN_VERSION=$(mvn --version 2>&1 | head -1)
log "Maven: ${MVN_VERSION}"

# --- Build -------------------------------------------------------------------

log "Building project..."
mvn clean package -DskipTests -q || die "Build failed."
log "Build successful."

# Resolve JAR path
JAR=$(ls ${JAR_PATTERN} 2>/dev/null | head -1)
[ -f "${JAR}" ] || die "Benchmark JAR not found at ${JAR_PATTERN}"
log "JAR: ${JAR}"

# --- Setup Results Directory -------------------------------------------------

mkdir -p "${RUN_DIR}/logs"

# --- Capture System Info -----------------------------------------------------

log "Capturing system info..."
{
    echo "=== Benchmark Run: ${TIMESTAMP} ==="
    echo ""
    echo "--- Java ---"
    java -version 2>&1
    echo ""
    echo "--- OS ---"
    uname -a
    echo ""
    echo "--- CPU ---"
    if [ -f /proc/cpuinfo ]; then
        grep "model name" /proc/cpuinfo | head -1
        echo "Cores: $(nproc)"
    else
        echo "CPU info not available"
    fi
    echo ""
    echo "--- Memory ---"
    if command -v free &> /dev/null; then
        free -h
    else
        echo "Memory info not available"
    fi
    echo ""
    echo "--- Git ---"
    echo "Branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "Commit: $(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
    echo "Dirty:  $(git status --porcelain 2>/dev/null | wc -l) files"
    echo ""
    echo "--- JMH Settings ---"
    echo "Warmup iterations:      ${WARMUP_ITERATIONS}"
    echo "Measurement iterations: ${MEASUREMENT_ITERATIONS}"
    echo "Forks:                  ${FORKS}"
    echo "JVM args:               ${JVM_ARGS}"
} > "${RUN_DIR}/system-info.txt"

# --- Timing CSV Header -------------------------------------------------------

echo "benchmark,exit_code,duration_seconds" > "${RUN_DIR}/timing.csv"

# --- Run Benchmarks ----------------------------------------------------------

SUITE_START=$(date +%s)
FAILED=0

# 1. Core Benchmarks
#    SingleFix, MultiFixChain, SchemaLookup across SMALL/MEDIUM/LARGE payloads
run_benchmark "core" ".*benchmarks.core.*" || FAILED=$((FAILED + 1))

# 2. Format Benchmarks
#    Gson vs Jackson JSON, SnakeYAML vs Jackson YAML, TOML, XML
run_benchmark "format" ".*benchmarks.format.*" || FAILED=$((FAILED + 1))

# 3. Codec Benchmarks
#    Primitive and Collection encode/decode across all formats
run_benchmark "codec" ".*benchmarks.codec.*" || FAILED=$((FAILED + 1))

# 4. Concurrent Benchmarks
#    Multi-threaded migration performance and scalability
run_benchmark "concurrent" ".*benchmarks.concurrent.*" || FAILED=$((FAILED + 1))

# --- Summary -----------------------------------------------------------------

SUITE_END=$(date +%s)
SUITE_DURATION=$(( SUITE_END - SUITE_START ))
SUITE_HOURS=$(( SUITE_DURATION / 3600 ))
SUITE_MINUTES=$(( (SUITE_DURATION % 3600) / 60 ))

log "============================================="
log "Benchmark Suite Complete"
log "Total time: ${SUITE_HOURS}h ${SUITE_MINUTES}m"
log "Failed:     ${FAILED}/4"
log "Results in: ${RUN_DIR}/"
log "============================================="

# Write summary
{
    echo ""
    echo "=== Summary ==="
    echo "Total duration: ${SUITE_HOURS}h ${SUITE_MINUTES}m (${SUITE_DURATION}s)"
    echo "Failed suites:  ${FAILED}/4"
    echo ""
    echo "--- Timing ---"
    cat "${RUN_DIR}/timing.csv"
    echo ""
    echo "--- Result Files ---"
    ls -lh "${RUN_DIR}"/*.json 2>/dev/null || echo "No JSON result files found"
} >> "${RUN_DIR}/system-info.txt"

# List result files
log "Result files:"
ls -lh "${RUN_DIR}"/*.json 2>/dev/null

exit ${FAILED}
