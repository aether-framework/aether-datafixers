# Claude Code – User-Scope Provisioning

Contents of this directory are baked into the devcontainer image and copied
into the `vscode` user's home (`~/.claude/`) at image build time. They are
therefore available in every project that uses this devcontainer.

## Skills (`skills/`)

Drop user-scope skills here, one directory per skill, each containing a
`SKILL.md` plus any supporting files. Layout:

```
skills/
├── my-skill/
│   ├── SKILL.md
│   └── ...
└── another-skill/
    └── SKILL.md
```

After a container rebuild, every skill in this directory appears under
`~/.claude/skills/<name>/` inside the container and is discoverable by
Claude Code.

### Pre-baked skill collections

The `Dockerfile` additionally clones third-party skill collections at
image-build time and merges them into `~/.claude/skills/` alongside your
own skills. Currently bundled:

| Source                                                        | Skills merged                                                                  |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| [`leonxlnx/taste-skill`](https://github.com/leonxlnx/taste-skill) | `taste-skill`, `gpt-tasteskill`, `image-to-code-skill`, `redesign-skill`, `soft-skill`, `output-skill`, `minimalist-skill`, `brutalist-skill`, `stitch-skill`, `imagegen-frontend-web`, `imagegen-frontend-mobile` |
| [`claudiocebpaz/vite-react-best-practices`](https://github.com/claudiocebpaz/vite-react-best-practices) | `vite-react-best-practices` |

To pin a specific revision, change the `git clone --depth 1 …` line in the
`skill-sources` stage of the `Dockerfile` to `git clone … && git -C … checkout <ref>`.

## MCP Servers

User-scope MCP servers are written **directly to `~/.claude.json`** in
`post-create.sh` via a `jq` merge (not via `claude mcp add`). Reason: the
`claude` CLI subcommands behave inconsistently in a fresh, unauthenticated
devcontainer — file writes are independent of CLI state and idempotent.

Currently provisioned: **Context7** (`@upstash/context7-mcp`). The package
is pre-installed globally during the image build so `npx` can resolve it
without hitting the npm registry once the firewall is active.

To add another MCP server, append another `jq` invocation in the same
block in `post-create.sh`. If it's distributed via npm, also add it to
the global install line in the Dockerfile so it works offline.

## Plugins

`post-create.sh` provisions plugins via two layers:

1. **Primary (always works):** writes `extraKnownMarketplaces` and
   `enabledPlugins` directly into `~/.claude/settings.json` with `jq`.
   This pre-registers all marketplaces so they show up in `/plugin → Marketplaces`
   and `/plugin → Discover` immediately, even without `claude login`.
2. **Best-effort:** runs `claude plugin marketplace add …` and
   `claude plugin install … --scope user`. These succeed once the user
   has signed in to Claude Code; before login they may fail silently and
   that's fine — the settings written in step 1 still register everything.

Currently provisioned:

| Plugin                        | Marketplace                  | Source                                                |
| ----------------------------- | ---------------------------- | ----------------------------------------------------- |
| `frontend-design`             | `claude-plugins-official`    | `anthropics/claude-plugins-official`                  |
| `impeccable`                  | `pbakaus-impeccable`         | `pbakaus/impeccable`                                  |
| `java-development-assistant`  | `java-dev-assistant-local`   | `pluginagentmarketplace/custom-plugin-java` (wrapped) |

The first two fetch from `github.com`, which is part of the egress whitelist.

### First-run flow inside the container

1. Run `claude` and complete the OAuth login.
2. Open `/plugin` → **Marketplaces**: the three marketplaces are listed.
3. Open `/plugin` → **Discover** and install the three plugins (or run
   `/plugin install <name>@<marketplace>` for each), or simply re-run
   `bash /workspace/.devcontainer/post-create.sh` — the best-effort CLI
   block now succeeds because you're authenticated.

### Adding another plugin

Extend the `jq` block in `post-create.sh` with the new marketplace and
plugin entry, and append a matching CLI install attempt in the
best-effort loop. If the marketplace lives outside the existing firewall
whitelist, also add the host to the `for domain in …` list.

### Local wrapper marketplaces

When an upstream repo is shaped like a Claude Code plugin (valid `plugin.json`)
but its `marketplace.json` follows a different schema — for example the SASMP
format used by `pluginagentmarketplace/custom-plugin-java` — Claude Code
cannot consume the upstream marketplace directly. The Dockerfile clones such
repos and exposes them through a local wrapper marketplace under
`marketplaces/<id>/marketplace.json` in this directory. The wrapper points at
the cloned plugin via a relative `source` and is registered at runtime via
`claude plugin marketplace add /opt/claude-marketplaces/<id>`.

Layout in the running container:

```
/opt/claude-marketplaces/java-dev-assistant/
├── .claude-plugin/marketplace.json    ← our wrapper
└── custom-plugin-java/                ← cloned upstream plugin
```
