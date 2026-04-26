# Aether-Datafixers Dev Container

Hardened reproducible dev environment for the Aether-Datafixers project. Bakes
Java 21 + Python 3.12 + Node LTS + native Claude Code (with MCP servers, plugin
marketplaces, plugins and user-scope skills) into a single image, locks the
runtime down with capability drops + an egress-only firewall, and ships a
ready-to-use Writerside documentation builder on the side.

## Files

| File                                                      | Purpose                                                                                                                                                                         |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Dockerfile`                                              | Multi-stage image: clones third-party skill/marketplace repos, then layers them onto the Java devcontainer base, plus the Writerside builder and user-scope Claude Code skills. |
| `devcontainer.json`                                       | Devcontainer spec: features, security flags, port forwarding, post-create hook.                                                                                                 |
| `post-create.sh`                                          | One-shot provisioning run on first container start. Installs Claude Code, writes MCP/plugin config, pre-populates the plugin cache, then enables the egress firewall.           |
| `writerside.sh`                                           | Wrapper around JetBrains' headless Writerside builder (`helpbuilderinspect`). Starts an Xvfb display on demand and forwards arguments. Available on `$PATH` as `writerside`.    |
| `claude/skills/`                                          | User-scope Claude Code skills, baked into `~/.claude/skills/` at image build.                                                                                                   |
| `claude/marketplaces/java-dev-assistant/marketplace.json` | Local wrapper marketplace pointing at the cloned `pluginagentmarketplace/custom-plugin-java` plugin.                                                                            |

## Image build

Two stages:

1. **`skill-sources`** (`debian:bookworm-slim` + `git`): clones five upstream
   repos in separate `RUN` layers — three skill collections and two plugin
   marketplaces. Each clone has a 3-attempt retry loop and is its own layer,
   so a transient DNS hiccup only invalidates the one repo that failed. Debian
   + glibc here instead of `alpine/git` because rootless BuildKit's resolver
   is intermittently flaky for back-to-back clones, and Alpine's musl libc
   makes that worse than glibc.
2. **Runtime stage** (`mcr.microsoft.com/devcontainers/java:1-21-bookworm`):
   adds curl/jq/Xvfb/font libs (Writerside dependencies), copies the
   Writerside builder from `jetbrains/writerside-builder`, copies skills into
   `~/.claude/skills/`, and stages the three plugin marketplaces under
   `/opt/claude-marketplaces/`.

Everything that needs network egress happens at build time, so a
post-create-time firewall lockdown can be aggressive.

## Devcontainer features

- `ghcr.io/devcontainers/features/java:1` — Java 21 with Maven and Gradle.
- `ghcr.io/devcontainers/features/python:1` — Python 3.12 with pipx.
- `ghcr.io/devcontainers/features/node:1` — Node LTS with node-gyp deps.

## Hardening

`runArgs` in `devcontainer.json`:

- `--cap-drop=ALL` then re-adds only what's actually needed
  (`CHOWN`, `SETGID`, `SETUID`, `KILL`, `NET_ADMIN`, `FOWNER`).
  `NET_ADMIN` is required because `post-create.sh` configures `iptables`.
- `--security-opt=no-new-privileges` blocks setuid escalations.
- Resource caps: `--memory=6g`, `--cpus=4`, `--pids-limit=1024`.
- `host.docker.internal` mapped to the host gateway for OAuth callback flows.

`post-create.sh` runs an iptables `OUTPUT DROP` and only allows traffic to
explicitly whitelisted hosts (resolved at firewall-setup time):

```
api.anthropic.com   claude.ai            github.com           raw.githubusercontent.com
maven.apache.org    repo1.maven.org      services.gradle.org  gradle.org
registry.npmjs.org  registry.yarnpkg.com context7.com         mcp.context7.com
```

If a new tool needs an external host at runtime, append it to the `for domain
in …` block in `post-create.sh`.

## Provisioning at first start (`post-create.sh`)

Runs in this order:

1. **Take ownership** of `$WORKSPACE_DIR` and `~/.claude`.
2. **Install the Claude Code native binary** via the official installer.
3. **Pre-install Context7 MCP** (`@upstash/context7-mcp`) globally so `npx`
   resolves it offline once the firewall blocks the npm registry.
4. **Write user-scope MCP config** to `~/.claude.json` directly via `jq`. This
   sidesteps the `claude mcp add` CLI, which behaves inconsistently in a
   fresh, unauthenticated devcontainer.
5. **Provision plugins** (see below).
6. **Verify state** by listing MCP servers / marketplaces / plugins.
7. **Enable egress firewall**.

## MCP servers

Currently baked in: **Context7** (`@upstash/context7-mcp`).

To add another MCP server, append a `jq` invocation in the same block of
`post-create.sh`. If it ships via npm, also add a global install line in the
`Dockerfile` so `npx` finds it offline.

## Plugins

All marketplaces are baked into the image at `/opt/claude-marketplaces/` and
`post-create.sh` pre-installs the three plugins deterministically. **No
`claude login` is required to provision them**, and the egress firewall does
not need to allow the upstream marketplace hosts at runtime.

For each target plugin, `post-create.sh`:

1. Reads the marketplace's `name` and the plugin's `source` from
   `<marketplace>/.claude-plugin/marketplace.json`.
2. Resolves the version. Precedence:
   `plugin.json.version` → marketplace entry `version` → 12-char git SHA →
   literal `unknown`.
3. Copies the plugin source to
   `~/.claude/plugins/cache/<marketplace-name>/<plugin-name>/<version>/`
   — the on-disk format the Claude Code CLI itself uses.
4. Appends an entry to `~/.claude/plugins/installed_plugins.json` so the CLI
   treats the plugin as already installed (schema observed in
   `anthropics/claude-code` issue #15642).
5. Registers the marketplace as a `directory` source under
   `extraKnownMarketplaces` in `~/.claude/settings.json` and toggles the
   plugin on under `enabledPlugins`.

Currently provisioned:

| Plugin                         | Marketplace name            | Marketplace source on disk                                                             |
|--------------------------------|-----------------------------|----------------------------------------------------------------------------------------|
| `frontend-design`              | `aether-vendor-plugins`     | `anthropics/claude-plugins-official` (cloned, renamed at image build — see note below) |
| `impeccable`                   | `impeccable`                | `pbakaus/impeccable` (cloned)                                                          |
| `java-development-assistant`   | `java-dev-assistant-local`  | `pluginagentmarketplace/custom-plugin-java` (wrapped)                                  |

> **Why `aether-vendor-plugins`?** Claude Code reserves any marketplace name
> matching the regex `^(claude|anthropic)-?` for official Anthropic
> marketplaces ([anthropics/claude-code#46786][reserved-names]) — that
> rejects both the literal `claude-plugins-official` (only allowed for
> `github` sources from the `anthropics` org) and any `claude-…` /
> `anthropic-…` suffix variants. Since the egress firewall blocks GitHub at
> runtime we have to register the local clone as a `directory` source, so
> the `Dockerfile` rewrites the `.name` field of the cloned
> `marketplace.json` to `aether-vendor-plugins` (project-scoped, outside the
> reserved namespace). Plugin contents and IDs are unchanged.

[reserved-names]: https://github.com/anthropics/claude-code/issues/46786

### First-run flow inside the container

1. Run `claude` and complete the OAuth login.
2. `/plugin → Marketplaces`: all three marketplaces are listed.
3. `/plugin → Plugins`: all three plugins already appear as enabled — no
   manual install step required.

### Brittleness disclaimer

`installed_plugins.json` is not part of Claude Code's documented public API.
If a future Claude Code release changes its schema, plugins may fail to load
on a freshly built container. Recovery path:

```bash
rm -rf ~/.claude/plugins/cache ~/.claude/plugins/installed_plugins.json
# inside `claude` after login:
/plugin install frontend-design@aether-vendor-plugins
/plugin install impeccable@impeccable
/plugin install java-development-assistant@java-dev-assistant-local
```

### Adding another plugin

1. Pre-clone the marketplace in the `skill-sources` stage of the `Dockerfile`
   and copy it under `/opt/claude-marketplaces/<name>/`.
2. Append a `<marketplace-dir>:<plugin-name>` entry to the `PLUGIN_TARGETS`
   array in `post-create.sh`. The script reads the marketplace name and
   plugin source from `marketplace.json` automatically.
3. If a runtime resource (e.g. an MCP server backing the plugin) needs
   network access, also extend the firewall allowlist in `post-create.sh`.

### Local wrapper marketplaces

When an upstream repo is shaped like a Claude Code plugin (valid
`plugin.json`) but its `marketplace.json` follows a different schema — for
example the SASMP format used by `pluginagentmarketplace/custom-plugin-java` —
Claude Code cannot consume the upstream marketplace directly. The Dockerfile
clones such repos and exposes them through a local wrapper marketplace under
`claude/marketplaces/<id>/marketplace.json`. The wrapper points at the cloned
plugin via a relative `source`, and `post-create.sh` registers it as a
`directory` source automatically.

Layout in the running container:

```
/opt/claude-marketplaces/java-dev-assistant/
├── .claude-plugin/marketplace.json    ← our wrapper
└── custom-plugin-java/                ← cloned upstream plugin
```

## Skills (user-scope, `claude/skills/`)

Drop user-scope skills under `claude/skills/`, one directory per skill, each
containing a `SKILL.md` plus any supporting files. Layout:

```
claude/skills/
├── my-skill/
│   ├── SKILL.md
│   └── ...
└── another-skill/
    └── SKILL.md
```

After a container rebuild, every skill in this directory appears under
`~/.claude/skills/<name>/` inside the container and is auto-discovered by
Claude Code.

### Pre-baked skill collections

The `Dockerfile`'s `skill-sources` stage clones third-party collections at
image-build time and merges them into `~/.claude/skills/`:

| Source                                                                                                   | Skills merged                                                                                                                                                                                                      |
|----------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`leonxlnx/taste-skill`](https://github.com/leonxlnx/taste-skill)                                        | `taste-skill`, `gpt-tasteskill`, `image-to-code-skill`, `redesign-skill`, `soft-skill`, `output-skill`, `minimalist-skill`, `brutalist-skill`, `stitch-skill`, `imagegen-frontend-web`, `imagegen-frontend-mobile` |
| [`claudiocebpaz/vite-react-best-practices`](https://github.com/claudiocebpaz/vite-react-best-practices)  | `vite-react-best-practices`                                                                                                                                                                                        |

To pin a specific revision, change the `git clone --depth 1 …` line in the
`skill-sources` stage to `git clone … && git -C … checkout <ref>`.

## Writerside documentation builder

The image embeds JetBrains' headless Writerside builder
(`jetbrains/writerside-builder`). Use the `writerside` wrapper:

```bash
# Convenience mode: <module>/<instance> [output-dir]
writerside Writerside/hi "$WORKSPACE_DIR/artifacts/help"

# Pass-through mode: forward flags directly to helpbuilderinspect
writerside --source-dir "$WORKSPACE_DIR" --product Writerside/hi --output-dir /tmp/out --runner other
```

`writerside.sh` starts an Xvfb display on demand
(`DISPLAY=:99`, fallback can be set by exporting `DISPLAY` beforehand),
which is why `xvfb` and the X11 client libs are in the runtime stage.

## Forwarded ports

| Port  | Use                            | Auto-forward |
|-------|--------------------------------|--------------|
| 8080  | Claude Code OAuth callback     | ignore       |

## Container env

| Variable                | Value                 | Purpose                                                               |
|-------------------------|-----------------------|-----------------------------------------------------------------------|
| `CLAUDE_CODE_SANDBOX`   | `true`                | Tells Claude Code it's running in a sandboxed environment.            |
| `NO_PROXY` / `no_proxy` | `localhost,127.0.0.1` | Bypass any inherited proxy for loopback traffic.                      |

## Re-running provisioning

`post-create.sh` is idempotent and safe to re-run. Useful when:

- you've edited `post-create.sh` itself and want to apply the changes
  without rebuilding the image;
- a Claude Code release shipped that broke the cache layout (run after
  `claude login` so `/plugin install` can also work);
- you've added a new plugin or marketplace and want to provision it now.

```bash
bash "$WORKSPACE_DIR/.devcontainer/post-create.sh"
```

`$WORKSPACE_DIR` is set by `containerEnv` in `devcontainer.json` and points
at whatever path the IDE chose to mount the source under (VS Code defaults
to `/workspaces/<basename>`, JetBrains may pick a different path). Inside
the container, use this variable rather than hard-coding `/workspace`.

### Debug mode

`post-create.sh` is silent on the happy path, which makes it hard to tell
whether a long-running step (e.g. the Claude Code installer pulling the
binary) is hung or just slow. Set `DEVCONTAINER_DEBUG=1` to enable verbose
output:

```bash
# Ad-hoc re-run with full tracing:
DEVCONTAINER_DEBUG=1 bash "$WORKSPACE_DIR/.devcontainer/post-create.sh"
```

This activates `bash`'s `set -x` (every command is printed before
execution), drops `curl -s` so the installer download shows progress, and
runs the installer itself under `bash -x` so its internal steps are
visible too. To turn it on for the initial postCreate run, add
`"DEVCONTAINER_DEBUG": "1"` to `containerEnv` in `devcontainer.json`.

### Rebuilding the image

For changes to the `Dockerfile` itself (not just `post-create.sh`), use your
IDE's standard rebuild action — JetBrains Gateway "Rebuild and Restart
Container", VS Code "Dev Containers: Rebuild Container", or
`devcontainer build` from the CLI.
