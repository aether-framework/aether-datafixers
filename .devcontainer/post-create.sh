#!/usr/bin/env bash
set -u

echo "=== Setting up ownership..."
sudo chown -R vscode:vscode /workspace 2>/dev/null || true
sudo chown -R vscode:vscode /home/vscode/.claude 2>/dev/null || true

echo "=== Installing Claude Code native binary..."
curl -fsSL https://claude.ai/install.sh | bash

# The Claude Code installer drops the binary into ~/.local/bin and updates
# the user's shell init files, but this non-interactive script has not
# re-sourced them yet. Export it directly so subsequent `claude` calls work.
export PATH="$HOME/.local/bin:$PATH"

echo "=== Pre-installing Context7 MCP server (offline-resolvable via npx)..."
# Installing globally up front means `npx @upstash/context7-mcp` finds the
# package locally once the egress firewall blocks the npm registry.
if command -v npm >/dev/null 2>&1; then
    npm install -g @upstash/context7-mcp || \
        echo "WARN: failed to globally install @upstash/context7-mcp; Context7 will need npm registry access at runtime"
else
    echo "WARN: npm not on PATH; skipping Context7 pre-install"
fi

echo "=== claude CLI version ==="
if command -v claude >/dev/null 2>&1; then
    claude --version || echo "WARN: claude --version failed"
else
    echo "WARN: claude CLI not on PATH"
fi

echo "=== Writing user-scope MCP config to ~/.claude.json (Context7) ==="
mkdir -p /home/vscode/.claude
CLAUDE_USER_JSON=/home/vscode/.claude.json
if [ ! -s "$CLAUDE_USER_JSON" ]; then
    echo '{}' > "$CLAUDE_USER_JSON"
fi
tmp=$(mktemp)
jq '.mcpServers = (.mcpServers // {}) | .mcpServers.context7 = {"command":"npx","args":["-y","@upstash/context7-mcp"]}' \
    "$CLAUDE_USER_JSON" > "$tmp" && mv "$tmp" "$CLAUDE_USER_JSON"
chown vscode:vscode "$CLAUDE_USER_JSON"
echo "context7 MCP entry written:"
jq '.mcpServers' "$CLAUDE_USER_JSON"

# ----- Plugin provisioning ---------------------------------------------------
# All three marketplaces are baked into the image at /opt/claude-marketplaces/.
# We deterministically replicate what `claude plugin install` would do, but
# without ever needing an authenticated Claude session:
#
#   1. Resolve each plugin's source directory from its marketplace.json.
#   2. Copy the plugin into ~/.claude/plugins/cache/<mkt>/<plugin>/<sha>/
#      using the on-disk format Claude Code expects (per anthropics/claude-code
#      issue #15642).
#   3. Append an entry to ~/.claude/plugins/installed_plugins.json so the CLI
#      treats the plugin as already installed.
#   4. Register each marketplace as a `directory` source in
#      ~/.claude/settings.json (extraKnownMarketplaces) and enable the plugin
#      via enabledPlugins.
#
# Trade-off: installed_plugins.json's schema is not officially documented, so
# a future Claude Code change could break this provisioning. If that happens,
# removing the cache + installed_plugins.json and running `/plugin install
# <name>@<marketplace>` after `claude login` is the documented recovery path.

# Each entry: <marketplace-dir-on-disk>:<plugin-name-from-marketplace.json>
PLUGIN_TARGETS=(
    "/opt/claude-marketplaces/claude-plugins-official:frontend-design"
    "/opt/claude-marketplaces/impeccable:impeccable"
    "/opt/claude-marketplaces/java-dev-assistant:java-development-assistant"
)

PLUGINS_DIR=/home/vscode/.claude/plugins
PLUGINS_CACHE=$PLUGINS_DIR/cache
INSTALLED_FILE=$PLUGINS_DIR/installed_plugins.json
SETTINGS_FILE=/home/vscode/.claude/settings.json

mkdir -p "$PLUGINS_CACHE"
[ -s "$INSTALLED_FILE" ] || echo '{}' > "$INSTALLED_FILE"
[ -s "$SETTINGS_FILE"  ] || echo '{}' > "$SETTINGS_FILE"

# Make sure the top-level objects exist so subsequent jq merges have something
# to land on, regardless of whether the file was empty or pre-existing.
tmp=$(mktemp)
jq '.extraKnownMarketplaces = (.extraKnownMarketplaces // {})
    | .enabledPlugins         = (.enabledPlugins // {})' \
    "$SETTINGS_FILE" > "$tmp" && mv "$tmp" "$SETTINGS_FILE"

echo "=== Pre-populating ~/.claude/plugins/cache from baked-in marketplaces ==="
for target in "${PLUGIN_TARGETS[@]}"; do
    mkt_dir="${target%%:*}"
    plugin="${target##*:}"
    mkt_json="$mkt_dir/.claude-plugin/marketplace.json"

    if [ ! -f "$mkt_json" ]; then
        echo "ERROR: marketplace.json missing at $mkt_json — skipping $plugin"
        continue
    fi

    mkt_name=$(jq -r '.name' "$mkt_json")
    plugin_src=$(jq -r --arg n "$plugin" \
        '.plugins[] | select(.name == $n) | .source' "$mkt_json")
    if [ -z "$plugin_src" ] || [ "$plugin_src" = "null" ]; then
        echo "ERROR: plugin '$plugin' not found in $mkt_json — skipping"
        continue
    fi

    case "$plugin_src" in
        /*) plugin_dir="$plugin_src" ;;
        *)  plugin_dir="$mkt_dir/$plugin_src" ;;
    esac
    if [ ! -d "$plugin_dir" ]; then
        echo "ERROR: plugin source directory missing: $plugin_dir — skipping $plugin"
        continue
    fi

    # Version resolution order matches Claude Code's documented precedence:
    # plugin.json.version -> marketplace entry .version -> short git SHA -> "unknown"
    version=""
    if [ -f "$plugin_dir/.claude-plugin/plugin.json" ]; then
        version=$(jq -r '.version // empty' "$plugin_dir/.claude-plugin/plugin.json")
    fi
    if [ -z "$version" ]; then
        version=$(jq -r --arg n "$plugin" \
            '.plugins[] | select(.name == $n) | .version // empty' "$mkt_json")
    fi
    git_sha=$(git -C "$plugin_dir" rev-parse HEAD 2>/dev/null \
              || git -C "$mkt_dir" rev-parse HEAD 2>/dev/null \
              || echo "")
    if [ -z "$version" ]; then
        if [ -n "$git_sha" ]; then
            version="${git_sha:0:12}"
        else
            version="unknown"
        fi
    fi

    cache_dir="$PLUGINS_CACHE/$mkt_name/$plugin/$version"
    rm -rf "$cache_dir"
    mkdir -p "$cache_dir"
    cp -a "$plugin_dir/." "$cache_dir/"

    key="$plugin@$mkt_name"
    lastUpdated=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")

    tmp=$(mktemp)
    jq --arg k "$key" \
       --arg ip "$cache_dir" \
       --arg ver "$version" \
       --arg ts "$lastUpdated" \
       --arg sha "$git_sha" \
       '.[$k] = [{
            "scope": "user",
            "installPath": $ip,
            "version": $ver,
            "lastUpdated": $ts,
            "gitCommitSha": $sha
        }]' "$INSTALLED_FILE" > "$tmp" && mv "$tmp" "$INSTALLED_FILE"

    tmp=$(mktemp)
    jq --arg n "$mkt_name" --arg p "$mkt_dir" --arg k "$key" \
       '.extraKnownMarketplaces[$n] = {"source":{"source":"directory","path":$p}}
        | .enabledPlugins[$k] = true' \
       "$SETTINGS_FILE" > "$tmp" && mv "$tmp" "$SETTINGS_FILE"

    echo "OK: $key version=$version sha=${git_sha:0:12} -> $cache_dir"
done

chown -R vscode:vscode /home/vscode/.claude

echo "installed_plugins.json:"
jq . "$INSTALLED_FILE"
echo "settings.json (relevant fields):"
jq '{extraKnownMarketplaces, enabledPlugins}' "$SETTINGS_FILE"

echo "=== Verifying Claude Code state ==="
if command -v claude >/dev/null 2>&1; then
    echo "--- MCP servers (user scope):"
    claude mcp list 2>&1 || echo "INFO: claude mcp list unavailable"
    echo "--- Plugin marketplaces:"
    claude plugin marketplace list 2>&1 || echo "INFO: claude plugin marketplace list unavailable"
    echo "--- Installed plugins:"
    claude plugin list 2>&1 || echo "INFO: claude plugin list unavailable"
fi

echo "=== Configuring strict egress firewall..."
sudo iptables -P OUTPUT DROP
sudo iptables -A OUTPUT -o lo -j ACCEPT
sudo iptables -A OUTPUT -d 127.0.0.1 -j ACCEPT
sudo iptables -A OUTPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

for domain in \
    api.anthropic.com \
    claude.ai \
    github.com \
    raw.githubusercontent.com \
    maven.apache.org \
    repo1.maven.org \
    services.gradle.org \
    gradle.org \
    registry.npmjs.org \
    registry.yarnpkg.com \
    context7.com \
    mcp.context7.com; do
    for ip in $(getent ahosts "$domain" | awk '{print $1}' | sort -u); do
        sudo iptables -A OUTPUT -d "$ip" -j ACCEPT
    done
done

git config --global --add safe.directory /workspace

echo "=== Dev Container ready ==="
echo "Claude Code (native) is installed. Run: claude"
echo "MCP servers (in ~/.claude.json): context7"
echo "Plugin marketplaces (directory sources in ~/.claude/settings.json):"
echo "  claude-plugins-official, impeccable, java-dev-assistant-local"
echo "Plugins pre-installed (cache + installed_plugins.json + enabledPlugins):"
echo "  frontend-design@claude-plugins-official"
echo "  impeccable@impeccable"
echo "  java-development-assistant@java-dev-assistant-local"
echo "User-scope skills available under ~/.claude/skills/ (incl. taste-skill collection)"
echo ""
echo "After \`claude login\`, /plugin should list all three plugins as enabled"
echo "with no manual install step required."
echo ""
echo "Egress firewall active - only whitelisted domains allowed"
