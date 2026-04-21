#!/usr/bin/env bash
set -u

echo "=== Setting up ownership..."
sudo chown -R vscode:vscode /workspace 2>/dev/null || true

echo "=== Installing Claude Code native binary..."
curl -fsSL https://claude.ai/install.sh | bash

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
    gradle.org; do
    for ip in $(getent ahosts "$domain" | awk '{print $1}' | sort -u); do
        sudo iptables -A OUTPUT -d "$ip" -j ACCEPT
    done
done

git config --global --add safe.directory /workspace

echo "=== Dev Container ready ==="
echo "Claude Code (native) is installed. Run: claude"
echo "Egress firewall active - only whitelisted domains allowed"
