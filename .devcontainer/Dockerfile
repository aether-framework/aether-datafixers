FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

RUN rm -f /etc/apt/sources.list.d/yarn.list

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/*