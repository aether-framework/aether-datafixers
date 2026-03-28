## Contributing to Aether Datafixers

Thank you for your interest in contributing to Aether Datafixers! 🎉

We welcome all contributions, whether it's fixing bugs, adding features, improving documentation, or helping with discussions.

---

## 🌿 Branching Strategy

This project follows a **Git Flow**-inspired branching model with two long-lived branches and several short-lived branch types.

### Long-Lived Branches

| Branch    | Purpose                                                                                                                                                                                              |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `main`    | 🔒 **Production-ready code only.** Represents the latest stable release. Direct commits and PRs from feature/fix branches are **not allowed** — only `release/*` branches may be merged into `main`. |
| `develop` | 🔧 **Integration branch.** All feature and fix contributions are merged here. This is the **default target** for your PRs.                                                                           |

### Short-Lived Branches

| Branch Pattern | Base Branch | Merges Into        | Purpose                                                                                             |
|----------------|-------------|--------------------|-----------------------------------------------------------------------------------------------------|
| `feature/*`    | `develop`   | `develop`          | New features (e.g., `feature/cli-module`, `feature/migration-diagnostics`)                          |
| `bugfix/*`     | `develop`   | `develop`          | Bug fixes (e.g., `bugfix/javadoc-site-compiling-fixes`)                                             |
| `release/*`    | `develop`   | `main` + `develop` | Release preparation (e.g., `release/0.5.x`). The **only** branch type allowed to merge into `main`. |
| `hotfix/*`     | `main`      | `main` + `develop` | Critical production fixes that cannot wait for the next release cycle.                              |

### Branch Flow Diagram

```
feature/* ──→ develop ──→ release/* ──→ main
bugfix/*  ──→ develop ──↗               ↑
                                    hotfix/*
```

### ⚠️ Important Rules

- **Never target `main` with feature or bugfix PRs.** PRs that accidentally target `main` are [automatically retargeted to `develop`](.github/workflows/retarget-pr-to-develop.yml). PRs from non-`release/*` branches to `main` are [blocked](.github/workflows/block-main-non-release.yml).
- **Always branch off `develop`** for features and bug fixes.
- **Keep branches short-lived.** Merge early and often to avoid painful conflicts.
- **Delete branches after merging.** Stale branches clutter the repository.

---

## 🚀 How to Contribute

### 1️⃣ Fork the Repository
1. Click the **Fork** button at the top of the repository.
2. Clone your fork:
```sh
git clone https://github.com/aether-framework/aether-datafixers.git
cd aether-datafixers
```

### 2️⃣ Create a Branch

Branch off `develop` and use the correct prefix:

```sh
git checkout develop
git pull origin develop
git checkout -b feature/new-awesome-feature
```

Use a meaningful branch name with the appropriate prefix:
- `feature/` — for new features (e.g., `feature/optic-integration`)
- `bugfix/` — for bug fixes (e.g., `bugfix/builder-null-pointer`)

### 3️⃣ Implement Your Changes
- Follow the coding style of the project.
- Write tests if applicable (`src/test/java`).
- Ensure the build is successful (`mvn clean package`).

### 4️⃣ Commit and Push
```sh
git add .
git commit -s -m "Add new awesome feature"
git push origin feature/new-awesome-feature
```

### 5️⃣ Open a Pull Request (PR)
1. Go to the **Pull Requests** tab in the repository.
2. Click **New Pull Request**.
3. Set the **base branch** to `develop` (not `main`!).
4. Describe your changes and fill out the PR template.
5. Wait for a review and feedback!

---

## 📜 Contribution Guidelines

- ✅ Keep PRs small and focused.
- ✅ Include clear commit messages.
- ✅ Make sure tests pass before submitting.
- ✅ Discuss major changes in an issue before implementation.
- ✅ Be respectful in discussions and code reviews.

---

## 📢 Need Help?
If you have any questions, feel free to open an **Issue** or join our **Discussions** section.

## ✨ Developer Certificate of Origin (DCO)

This project uses the [Developer Certificate of Origin (DCO)](DCO) instead of a traditional CLA. By contributing, you certify that you have the right to submit your work under the project's [MIT License](LICENSE).

To sign off on your commits, add the `-s` flag:

```sh
git commit -s -m "Your commit message"
```

This adds a `Signed-off-by: Your Name <your.email@example.com>` line to your commit, confirming that you agree to the DCO.

## 🤖 AI-Assisted Contributions

We allow AI-assisted contributions under strict conditions. If you use AI tools (GitHub Copilot, Claude, ChatGPT, etc.), you **must** follow our [AI Usage Guidelines](AI_USAGE.md) — including mandatory disclosure and quality standards.

---

🚀 **Happy Coding!**
