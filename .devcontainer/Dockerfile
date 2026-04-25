# Stage 1: pull third-party skill and plugin-marketplace repositories at
# image-build time. Cloning here (rather than in postCreate) keeps the install
# reproducible, means the artifacts are baked into the image regardless of
# runtime firewall state, and lets post-create.sh pre-populate the Claude Code
# plugin cache without ever needing `claude login` or network egress.
#
# Why debian:bookworm-slim + apt-get git instead of alpine/git: rootless
# BuildKit's DNS resolver intermittently times out for back-to-back GitHub
# clones, and Alpine's musl libc resolver makes that noticeably worse than
# glibc. The bind-mount of /etc/resolv.conf is the same; glibc just degrades
# more gracefully (longer retries, no hard 12s fail loop). Cost is one extra
# apt-get layer (~50 MB), which the BuildKit cache reuses on rebuilds.
#
# Each clone is its own RUN layer with a 3-attempt retry loop:
#   * Splitting clones across layers means a transient failure for one repo
#     doesn't invalidate the layers that already cloned successfully — the
#     build resumes from the failed step on the next attempt.
#   * The trailing `exit 1` only fires when all 3 attempts failed in a row,
#     surfacing a real error instead of a silent skip.
FROM debian:bookworm-slim AS skill-sources
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN for i in 1 2 3; do \
        git clone --depth 1 https://github.com/leonxlnx/taste-skill.git /skills/taste-skill && exit 0; \
        echo "clone failed (attempt $i), retrying in 5s..."; sleep 5; \
    done; exit 1
RUN for i in 1 2 3; do \
        git clone --depth 1 https://github.com/claudiocebpaz/vite-react-best-practices.git /skills/vite-react-best-practices && exit 0; \
        echo "clone failed (attempt $i), retrying in 5s..."; sleep 5; \
    done; exit 1
RUN for i in 1 2 3; do \
        git clone --depth 1 https://github.com/pluginagentmarketplace/custom-plugin-java.git /skills/custom-plugin-java && exit 0; \
        echo "clone failed (attempt $i), retrying in 5s..."; sleep 5; \
    done; exit 1
RUN for i in 1 2 3; do \
        git clone --depth 1 https://github.com/anthropics/claude-plugins-official.git /marketplaces/claude-plugins-official && exit 0; \
        echo "clone failed (attempt $i), retrying in 5s..."; sleep 5; \
    done; exit 1
RUN for i in 1 2 3; do \
        git clone --depth 1 https://github.com/pbakaus/impeccable.git /marketplaces/impeccable && exit 0; \
        echo "clone failed (attempt $i), retrying in 5s..."; sleep 5; \
    done; exit 1

FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

RUN rm -f /etc/apt/sources.list.d/yarn.list

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    jq \
    xvfb \
    x11-utils \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

# JetBrains Writerside builder (helpbuilderinspect). Pulled at image-build time
# from the official image so the resulting container needs no Docker socket and
# stays compatible with the hardened egress firewall configured at postCreate.
COPY --from=jetbrains/writerside-builder:2026.04.8711 /opt/builder /opt/builder

COPY writerside.sh /usr/local/bin/writerside
RUN chmod +x /usr/local/bin/writerside

# Bake user-scope Claude Code skills into the image. Drop new skills under
# .devcontainer/claude/skills/ in the repo; they end up at ~/.claude/skills/
# for the vscode user and are discovered automatically on container start.
RUN install -d -o vscode -g vscode /home/vscode/.claude /home/vscode/.claude/skills
COPY --chown=vscode:vscode claude/skills/ /home/vscode/.claude/skills/

# leonxlnx/taste-skill: curated frontend/design skill collection. Both sub-trees
# (`skills/` and `imagegen-skills/`) follow the standard `<name>/SKILL.md`
# layout, so they merge directly into the user-scope skills directory.
COPY --from=skill-sources --chown=vscode:vscode /skills/taste-skill/skills/ /home/vscode/.claude/skills/
COPY --from=skill-sources --chown=vscode:vscode /skills/taste-skill/imagegen-skills/ /home/vscode/.claude/skills/

# claudiocebpaz/vite-react-best-practices: single skill with SKILL.md at the
# repo root, so the whole repository is the skill directory.
COPY --from=skill-sources --chown=vscode:vscode /skills/vite-react-best-practices/ /home/vscode/.claude/skills/vite-react-best-practices/

# pluginagentmarketplace/custom-plugin-java: a full Claude Code plugin (agents +
# skills + commands). Its upstream marketplace.json follows the SASMP schema
# rather than Claude Code's, so we ship a local wrapper marketplace that points
# at the cloned plugin directory via a relative `./custom-plugin-java` source.
RUN install -d -o vscode -g vscode \
        /opt/claude-marketplaces \
        /opt/claude-marketplaces/java-dev-assistant \
        /opt/claude-marketplaces/java-dev-assistant/.claude-plugin
COPY --chown=vscode:vscode claude/marketplaces/java-dev-assistant/marketplace.json \
        /opt/claude-marketplaces/java-dev-assistant/.claude-plugin/marketplace.json
COPY --from=skill-sources --chown=vscode:vscode /skills/custom-plugin-java/ \
        /opt/claude-marketplaces/java-dev-assistant/custom-plugin-java/

# anthropics/claude-plugins-official and pbakaus/impeccable: the two GitHub-hosted
# upstream marketplaces are baked in too, so post-create.sh can pre-populate
# ~/.claude/plugins/cache/ without hitting GitHub at runtime (the egress firewall
# would block the clone after post-create finishes anyway).
COPY --from=skill-sources --chown=vscode:vscode /marketplaces/claude-plugins-official/ \
        /opt/claude-marketplaces/claude-plugins-official/
COPY --from=skill-sources --chown=vscode:vscode /marketplaces/impeccable/ \
        /opt/claude-marketplaces/impeccable/