# Security Policy

## Supported Versions

We provide security updates for the following versions:

| Version | Support Status    | End of Support |
|--------|--------------------|----------------|
| 1.0.x  | 🔜 Planned LTS     | TBD (1 year)   |
| 0.5.x  | ✅ Active Support  | February 2026  |
| 0.4.x  | ❌ End of Life     | -              |
| 0.3.x  | ❌ End of Life     | -              |
| 0.2.x  | ❌ End of Life     | -              |
| 0.1.x  | ❌ End of Life     | -              |

If you are using an older version, we **strongly recommend upgrading** to the latest stable release.

---

## Security Features

### Automated Security Scanning

This project uses multiple automated security tools:

- **GitHub CodeQL** – Static Application Security Testing (SAST)
- **OWASP Dependency-Check** – Known vulnerability detection in dependencies
- **GitHub Dependency Review** – Pull request dependency analysis
- **Dependabot** – Automated dependency updates

All scans are executed automatically in CI pipelines on every pull request and release build.

---

## Supply Chain Security

### Artifact Integrity & Signing

All official release artifacts of **Aether Datafixers** are **cryptographically signed** to guarantee integrity and authenticity.

- All release artifacts are **GPG signed**
- Signatures are generated during the release pipeline
- Each published artifact is accompanied by a corresponding `.asc` signature file
- Consumers can verify artifacts before usage

Example verification flow:

```
gpg --verify artifact.jar.asc artifact.jar
```

Unsigned or modified artifacts **must not be trusted**.

---

### Signing Keys

- A **dedicated GPG key** is used for automated GitHub releases and deployments
- Release signing keys are **separate from personal developer keys**
- Private key material is **never committed** to the repository
- Keys are stored securely using CI secret management

The signing process is fully automated and enforced during release builds.

---

## Reporting a Vulnerability

If you discover a security vulnerability in **Aether Datafixers**, please report it **privately**.

### Contact

- **Email:** `security@splatgames.de`
- **GitHub Security Advisories:**  
  https://github.com/aether-framework/aether-datafixers/security/advisories/new
- **GitHub Issues:**  
  Do **not** report security vulnerabilities in public issues.

---

## Disclosure Process

1. Report the issue privately
2. Acknowledgment within **48 hours**
3. Fix timeline provided within **7 days**
4. Critical vulnerabilities (CVSS ≥ 9.0): patch within **72 hours**
5. High severity (CVSS ≥ 7.0): patch within **14 days**
6. Security advisory published after resolution

---

## Response Time SLA

| Severity                 | Acknowledgment | Fix Timeline |
|--------------------------|----------------|--------------|
| Critical (CVSS 9.0–10.0) | 24 hours       | 72 hours     |
| High (CVSS 7.0–8.9)      | 48 hours       | 14 days      |
| Medium (CVSS 4.0–6.9)    | 48 hours       | 30 days      |
| Low (CVSS 0.1–3.9)       | 72 hours       | Next release |

---

## Security Best Practices

- Always use the **latest stable version**
- Verify **GPG signatures** of all downloaded artifacts
- Enable automated dependency updates
- Validate input data at system boundaries
- Use appropriate `DynamicOps` implementations for untrusted data
- Avoid sensitive data in logs
- Review the attached **SBOM** for dependency transparency

---

## Vulnerability Disclosure Policy

We follow a **coordinated disclosure** process:

1. Private disclosure
2. Fix development
3. Advisory preparation
4. Coordinated release
5. Public disclosure after a grace period

---

## Security Audits

Security audits are welcome.

- Contact `security@splatgames.de` before starting
- Follow responsible disclosure practices
- Researchers may be credited with permission

---

## PGP Key

For encrypted communication and release verification:

- **Key Purpose:** Release artifact signing
- **Key ID:** Available upon request
- **Fingerprint:** Available upon request

Contact: **security@splatgames.de**

---

Thank you for helping keep **Aether Datafixers** secure.