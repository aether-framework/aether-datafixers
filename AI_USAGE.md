# 🤖 AI Usage Guidelines

Aether Datafixers permits the use of AI tools in contributions — but under strict conditions. Transparency, quality, and human accountability are **non-negotiable**. This document defines the rules that all contributors and maintainers must follow when using AI-assisted tooling.

---

## 📋 Purpose & Scope

### Why This Policy Exists

AI coding assistants (GitHub Copilot, Claude, ChatGPT, Cursor, and others) are increasingly part of modern development workflows. **We welcome the responsible and transparent use of AI tools** to accelerate development while maintaining our high standards for **transparency**, **quality**, **security**, and **legal compliance**.

### Who This Applies To

- ✅ All **contributors** (external and internal) submitting code, documentation, or other content
- ✅ All **maintainers** reviewing, merging, or creating contributions
- ✅ **Automated AI agents** (e.g., `copilot-swe-agent[bot]`) that submit PRs directly

### What This Covers

This policy applies to all forms of contribution:

- Source code and tests
- Documentation and Javadoc
- Commit messages and PR descriptions
- Issue comments and discussion posts
- Code reviews and review comments
- Configuration and CI/CD changes

---

## ⚖️ General Principles

1. **AI is a tool, humans are accountable.** AI does not author contributions — humans do. The person who submits AI-assisted work bears full responsibility for it.
2. **Same quality bar — no exceptions.** AI-generated output must meet the exact same standards as human-written code: conventions, tests, documentation, and review.
3. **Transparency is mandatory.** Disclosure of significant AI assistance is required, not optional. Honest disclosure is always better than concealment.
4. **Quality and security over speed.** AI can accelerate development, but speed must never come at the expense of correctness, maintainability, or security.
5. **When in doubt, write it yourself.** If you are unsure whether AI output is correct, secure, or license-compatible — do not submit it. Rewrite it by hand.

---

## ✅ Permitted Uses

AI tools may be used in the following areas, subject to the conditions listed:

| Use Case                              | Status    | Conditions                                                        |
|---------------------------------------|-----------|-------------------------------------------------------------------|
| Code generation (features, bug fixes) | ✅ Allowed | Must be fully understood, tested, and reviewed by the contributor |
| Test generation (JUnit 5 / AssertJ)   | ✅ Allowed | Assertions and edge cases must be manually verified               |
| Documentation and Javadoc             | ✅ Allowed | Must be factually accurate — AI may hallucinate API details       |
| Boilerplate and scaffolding           | ✅ Allowed | Still requires review for project conventions                     |
| Commit message drafting               | ✅ Allowed | Must accurately describe the actual changes                       |
| PR description drafting               | ✅ Allowed | Must reflect the real changes, not AI assumptions                 |
| Refactoring suggestions               | ✅ Allowed | All existing tests must pass; behavior must be preserved          |
| Issue triage and response drafting    | ✅ Allowed | Must be reviewed before posting                                   |

**Important:** All permitted uses require [disclosure](#-disclosure-requirements) and are subject to [quality standards](#-quality-standards).

---

## 🚫 Restricted & Prohibited Uses

### 🏷️ Good First Issues — Learning First

Issues labeled **`good first issue`** are **strongly encouraged** to be completed **primarily without significant AI assistance**.  

**Why:** Good first issues exist to help new contributors learn the codebase and ramp up through hands-on experience. They are intentionally kept simple. Solving them with your own effort lets you truly understand the project’s structure, patterns, and conventions — and keeps the opportunity fair for everyone who wants to make their first contribution through genuine learning.

If you are new to the project, embrace these issues as a learning opportunity. If you are experienced enough to use AI effectively, you are welcome to pick a more challenging issue instead.

> Contributors may still use AI for small suggestions or learning purposes, but the core solution should reflect personal understanding and effort.

### ⚠️ Restricted Uses

The following areas require **extra scrutiny and maintainer approval** before AI-assisted contributions are accepted:

- **Security-sensitive code** — Any code handling untrusted data through `DynamicOps`, cryptographic operations, or artifact signing. Must be thoroughly reviewed by a maintainer with security context. See [SECURITY.md](SECURITY.md).
- **Public API design** — AI may suggest API shapes, but public API decisions must be made deliberately by maintainers. AI suggestions must not be accepted wholesale.
- **Core optics and type system changes** — Changes to the optic hierarchy (`Lens`, `Prism`, `Iso`, `Affine`, `Traversal`) or the type system require deep domain understanding that AI tools may lack.
- **CI/CD pipeline modifications** — Changes to GitHub Actions workflows, Maven profiles, or build configuration require maintainer review for security and correctness.

### 🚫 Prohibited Uses

The following uses of AI are **not permitted**:

- **AI as sole code reviewer** — Every PR must be reviewed and approved by at least one human maintainer. AI review tools may supplement but never replace human review.
- **Submitting unreviewed AI output** — Copy-pasting AI-generated code without reading, understanding, and verifying it is prohibited. You must be able to explain every line you submit.
- **Undisclosed significant AI usage** — Knowingly concealing significant AI assistance violates this policy. See [Accountability](#-accountability).
- **AI-generated vulnerability reports** — Automated AI scanning to generate public vulnerability reports is prohibited. Follow the private disclosure process in [SECURITY.md](SECURITY.md).
- **Bypassing project standards** — Using AI to circumvent tests, Checkstyle rules, or other quality gates is prohibited.
- **AI for license or legal decisions** — AI tools must not be relied upon for license compatibility analysis, DCO compliance interpretation, or other legal matters.

---

## 📢 Disclosure Requirements

Disclosure of AI assistance is **mandatory**. This is the most important operational requirement of this policy.

### 🔎 When to Disclose

Disclosure is required whenever AI tools were used to **generate or substantially modify** code, documentation, or other contributions.

**Rule of thumb:** If the AI produced a block of code, a paragraph of text, or a test case that you then submitted — even if you edited it afterward — disclose it.

**Exempt:** Minor AI-assisted tasks such as IDE autocomplete suggestions, spell-checking, or grammar corrections do not require disclosure.

### 💬 In Commits

Use a `Co-Authored-By` trailer in your commit message:

```
feat: Add field renaming support for nested types

Implemented recursive field renaming in NestedTypeFix with
full test coverage.

Co-Authored-By: GitHub Copilot <noreply@github.com>
Signed-off-by: Your Name <your.email@example.com>
```

Common AI tool trailers:

```
Co-Authored-By: GitHub Copilot <noreply@github.com>
Co-Authored-By: Claude <noreply@anthropic.com>
Co-Authored-By: ChatGPT <noreply@openai.com>
```

For automated AI agents (e.g., `copilot-swe-agent[bot]`), the commit author field already serves as disclosure.

### 📝 In Pull Requests

1. Check the **AI Disclosure** checkbox in the [PR template](.github/PULL_REQUEST_TEMPLATE.md).
2. In the PR description, state **which parts** were AI-assisted and **which tool** was used.

Example:

```markdown
**AI Disclosure:** The implementation in `NestedTypeFix.java` and its unit tests
were generated with Claude and reviewed/modified by the contributor. The Javadoc
was written manually.
```

### 💭 In Issues and Discussions

When posting AI-generated analysis, suggestions, or responses, add a brief note:

> *This response was drafted with AI assistance.*

---

## 🔍 Quality Standards

### Code Quality

AI-generated code must meet the same standards as human-written code:

- ✅ Pass all existing tests (`mvn clean package`)
- ✅ Include new tests for new behavior (JUnit 5 + AssertJ)
- ✅ Follow project coding conventions (validated by Checkstyle)
- ✅ Include proper Javadoc for all public API methods
- ✅ Use JetBrains annotations (`@NotNull`, `@Nullable`) where appropriate
- ✅ Use Guava `Preconditions` for argument validation
- ✅ Maintain thread-safety invariants — Aether Datafixers types are immutable and thread-safe by design
- ✅ Not introduce unnecessary dependencies

### Documentation Quality

AI-generated documentation must:

- ✅ Be **factually accurate** — AI tools frequently hallucinate API details, method signatures, and module names
- ✅ Be **consistent** with existing documentation style and structure
- ✅ Reference **correct** class, method, and module names
- ✅ Not contradict existing documentation

### Understanding Requirement

Contributors must **understand** the code they submit. "The AI wrote it" is not a valid explanation for bugs, security issues, or convention violations. Reviewers may ask contributors to explain AI-generated code, and the contributor must be able to do so.

---

## 🔎 Review Process

AI-assisted PRs follow the same review process as all other PRs (see [CONTRIBUTING.md](CONTRIBUTING.md)), with the following additional considerations:

### Additional Review Focus Areas

- **Test quality** — AI-generated tests may appear comprehensive but miss edge cases, test only happy paths, or contain incorrect assertions. Review assertions carefully.
- **Hallucinated APIs** — AI may reference methods, classes, or modules that do not exist in this project. Verify all API references against the actual codebase.
- **Naming conventions** — AI-generated names may not match project conventions (e.g., `DataVersion` vs. `Version`, `TypeReference` vs. `TypeRef`). Enforce consistency.
- **Thread safety** — Verify that AI-generated code maintains the project's immutability and thread-safety invariants.
- **Dependency additions** — AI may suggest adding external libraries. Verify necessity and license compatibility.
- **Subtle logic errors** — AI-generated code can appear correct at first glance while containing subtle bugs. Review logic paths carefully.

### Human Review Requirement

- At least **one human maintainer** must review and approve every PR — no exceptions.
- Reviewers may request the contributor to **explain** any AI-generated code.
- Reviewers are not expected to detect undisclosed AI usage. The disclosure responsibility lies with the contributor.

---

## 🛡️ Maintainer Guidelines

Maintainers are held to the **same disclosure and quality standards** as contributors, plus the following additional responsibilities:

### AI Agent PRs

When reviewing and merging AI agent PRs (e.g., from `copilot-swe-agent[bot]`):

- 🔍 Review the **actual code changes**, not just the bot's summary
- 🧪 Run tests **locally** if the changes touch core logic
- ✅ Verify the PR addresses the **linked issue** correctly
- ⚠️ Pay extra attention to changes in security-sensitive or public API areas

### Configuration and Standards

- AI agent configurations (e.g., `CLAUDE.md`, Copilot workspace settings) must **not** bypass project standards such as tests, Checkstyle, or review requirements.
- New AI agents must receive **maintainer consensus** before being added to the `.github/dco.yml` bot exemption list.
- Do not use AI tools to **rubber-stamp** reviews. Every approval must reflect genuine human assessment of the code.

---

## ⚖️ Intellectual Property & Licensing

All contributions — whether human-written or AI-assisted — must be compatible with the project's [MIT License](LICENSE).

### DCO and AI

By submitting AI-assisted code and signing off with the [Developer Certificate of Origin](DCO), you certify that:

1. You have the **right to submit** the work under the MIT License.
2. The AI-generated content does not, to the best of your knowledge, **infringe on third-party copyrights** or include code from proprietary or incompatibly licensed sources.

### Provenance Guidelines

- ⚠️ Avoid using AI tools configured to train on or reproduce code from repositories with **incompatible licenses** (e.g., GPL, AGPL) when generating contributions to this project.
- When in doubt about the **provenance** of AI-generated code, err on the side of rewriting the contribution by hand.
- The human contributor is listed as the **author**; the AI tool is acknowledged via `Co-Authored-By`.

---

## 🔒 Accountability

### Responsibility

- The **human contributor** who submits AI-assisted work is **fully responsible** for it — including bugs, security vulnerabilities, test failures, and convention violations.
- For **AI agent PRs** (where no human is the commit author), the maintainer who requested the agent's work and/or merged the PR assumes responsibility.
- "The AI generated it" is **not** a mitigating factor for quality issues or policy violations.

### Enforcement

- Violations of this policy — particularly **undisclosed AI usage** or **submitting unreviewed AI output** — will be handled constructively on a case-by-case basis.
- Repeated or intentional violations may be escalated through the project's [Code of Conduct](CODE_OF_CONDUCT.md) enforcement process.
- Serious violations (e.g., knowingly submitting AI-generated code with license conflicts) may result in **contribution restrictions**.

---

## 📝 Updates to This Policy

This policy is a **living document**. As AI tools and practices evolve, so will these guidelines.

- Significant changes will be proposed via a **Pull Request** and discussed before merging.
- Contributors are encouraged to suggest improvements by opening an **Issue** or **Discussion**.
- Minor clarifications may be made directly by maintainers.

**Last updated:** March 2026

---

Thank you for helping keep **Aether Datafixers** transparent, secure, and high-quality. 🚀