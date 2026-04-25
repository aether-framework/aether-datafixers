---
name: aether-javadoc-thorough
description: Enforces the Aether JavaDoc Gold Standard – extremely thorough, structured, example-heavy Javadocs for every class and public member. No missing @tags, no short docs, always includes concrete examples, performance notes, thread safety, diagnostics, @since, @see etc.
version: 1.0.0
triggers:
- java
- javadoc
- class
- method
- generate
- edit
- review
  priority: high
  alwaysRelevant: true
---

# Aether JavaDoc Gold Standard

**This skill is strictly enforced project-wide.** Every class and every public/protected element must follow the Aether JavaDoc Gold Standard: extremely thorough, well-structured, and example-rich documentation.

## Structure of a perfect Class Javadoc (always like this)

- Start with a precise yet comprehensive description (1–2 paragraphs).
- Use `<h2>` headings for logical sections (e.g. "Combinator Categories", "Traversal Combinators", "Field Operations" etc.).
- Detailed `<ul>` lists with links to the methods.
- Dedicated sections such as:
    - **Field-Level Diagnostics** (when relevant)
    - **Performance Notes**
    - **Thread Safety**
    - **Sequencing vs Choice – Quick Reference**
    - **Traversal Strategies – Quick Reference**
- Always include a realistic `<h2>Putting It Together — A Realistic Example</h2>` with a complete, copy-pasteable code example.
- End with `@author Erik Pförtner`, `@see`, `@since 0.x.0`

## Structure of a perfect Method Javadoc (always like this)

Every method **must** contain:
- Thorough description (multiple paragraphs when needed)
- `<h4>Example</h4>` with a concrete, runnable code example
- `<h4>Field-Level Diagnostics</h4>` (if the method affects fields)
- Complete `@param` tags (with type + precise meaning)
- `@return` (detailed description, even for void)
- `@throws` (when applicable)
- `@since`
- Where appropriate: `@see` and notes on performance/thread-safety

**Strictly forbidden:**
- One-liner Javadocs
- Missing `@tags`
- Generic or vague examples
- Missing Performance Notes / Thread Safety where relevant
- No `<h4>` example sections

## Default Behaviour

- On **every** code generation, edit, or refactoring (new class, new method, etc.) automatically apply this exact style.
- During code reviews (`/java-code-review`) always check Javadoc quality and demand missing parts.
- When touching existing code: upgrade it to this standard.

This skill is the **project default** for all of Aether. Claude must never deviate from it unless the user explicitly says "short Javadoc" (which will almost never happen).