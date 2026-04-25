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

# ----- Primary path: write the user-scope config files directly -------------
# In a fresh, unauthenticated devcontainer the `claude` CLI subcommands for
# MCP/plugins behave inconsistently (some require an interactive session, some
# require login). Writing the config files directly is independent of CLI
# state, idempotent, and survives every rebuild. The CLI calls below are kept
# as a best-effort layer that takes over once `claude login` has been run.

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

echo "=== Writing user-scope plugin marketplaces to ~/.claude/settings.json ==="
CLAUDE_SETTINGS=/home/vscode/.claude/settings.json
if [ ! -s "$CLAUDE_SETTINGS" ]; then
    echo '{}' > "$CLAUDE_SETTINGS"
fi
tmp=$(mktemp)
jq '
    .extraKnownMarketplaces = (.extraKnownMarketplaces // {})
    | .extraKnownMarketplaces["claude-plugins-official"]    = {"source":{"source":"github","repo":"anthropics/claude-plugins-official"}}
    | .extraKnownMarketplaces["pbakaus-impeccable"]         = {"source":{"source":"github","repo":"pbakaus/impeccable"}}
    | .extraKnownMarketplaces["java-dev-assistant-local"]   = {"source":{"source":"directory","path":"/opt/claude-marketplaces/java-dev-assistant"}}
    | .enabledPlugins = (.enabledPlugins // {})
    | .enabledPlugins["frontend-design@claude-plugins-official"]              = true
    | .enabledPlugins["impeccable@pbakaus-impeccable"]                        = true
    | .enabledPlugins["java-development-assistant@java-dev-assistant-local"]  = true
' "$CLAUDE_SETTINGS" > "$tmp" && mv "$tmp" "$CLAUDE_SETTINGS"
chown -R vscode:vscode /home/vscode/.claude
echo "Plugin marketplaces and enabledPlugins written:"
jq '{extraKnownMarketplaces, enabledPlugins}' "$CLAUDE_SETTINGS"

# ----- Best-effort path: run the official CLI commands ----------------------
# These succeed once the user has run `claude login`. If they fail now, the
# direct settings written above still register the marketplaces, so the user
# can finish the install with `/plugin install <name>@<marketplace>` from
# inside Claude Code in seconds.

echo "=== Best-effort CLI registration (may fail before claude login) ==="
if command -v claude >/dev/null 2>&1; then
    echo "--- claude mcp add context7"
    claude mcp remove --scope user context7 2>&1 || true
    claude mcp add --scope user context7 -- npx -y @upstash/context7-mcp 2>&1 || \
        echo "INFO: claude mcp add failed; settings.json fallback is in place"

    for entry in \
        "anthropics/claude-plugins-official|frontend-design@claude-plugins-official" \
        "pbakaus/impeccable|impeccable@pbakaus-impeccable" \
        "/opt/claude-marketplaces/java-dev-assistant|java-development-assistant@java-dev-assistant-local"
    do
        marketplace_src="${entry%%|*}"
        plugin_id="${entry##*|}"
        echo "--- claude plugin marketplace add $marketplace_src"
        claude plugin marketplace add "$marketplace_src" 2>&1 || true
        echo "--- claude plugin install $plugin_id"
        claude plugin install "$plugin_id" --scope user 2>&1 || \
            echo "INFO: claude plugin install $plugin_id failed; settings.json fallback is in place"
    done
fi

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
echo "Plugin marketplaces (in ~/.claude/settings.json): claude-plugins-official, pbakaus-impeccable, java-dev-assistant-local"
echo "User-scope skills available under ~/.claude/skills/ (incl. taste-skill collection)"
echo ""
echo "First run: launch \`claude\`, sign in, then run \`/plugin\` to install"
echo "frontend-design, impeccable, and java-development-assistant from the"
echo "pre-registered marketplaces (or re-run this script after login to"
echo "have the best-effort CLI block do it for you)."
echo ""
echo "Egress firewall active - only whitelisted domains allowed"
