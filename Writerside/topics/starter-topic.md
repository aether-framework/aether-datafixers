# Welcome to %product%

<tldr>
    <p>%tagline% Forward-patch serialized data across schema versions — in JSON, YAML, TOML, XML, or any custom format — through a single composable API.</p>
    <p>Current release: <b>%latest_version%</b> · Requires Java %java_min%+ and Maven %maven_min%+.</p>
</tldr>

## What is %product%?

%product% lets you evolve persisted data as your code evolves. You define a
**schema per version** and the **fixes between them** — the framework picks the right
fixes, runs them in the right order, and hands back data at the target version.

It is inspired by Minecraft's *DataFixer Upper (DFU)*, but designed from the ground
up for **simplicity**, **clarity**, and **developer ergonomics**.

## Highlights

- **Forward patching** — data migrates old → new through a chain of `DataFix` instances.
- **Format-agnostic** — works with JSON, YAML, TOML, XML, or any custom format via `DynamicOps<T>`.
- **Immutable & thread-safe** — safe for concurrent, high-throughput environments.
- **Modular** — pick only what you need: core, codec, CLI, Spring Boot starter, testkit, schema tools.
- **Testable** — fluent test builders, custom AssertJ assertions, and dedicated migration harnesses.

## A Migration in Four Steps

<procedure title="From raw data to migrated data" id="four-step-migration">
    <step>
        <p>Add <code>aether-datafixers-core</code> (and a codec of your choice) to your build.</p>
    </step>
    <step>
        <p>Implement a <code>DataFixerBootstrap</code> that registers your <code>Schema</code>s and <code>DataFix</code>es.</p>
    </step>
    <step>
        <p>Create the fixer via <code>DataFixerRuntimeFactory.create(currentVersion, bootstrap)</code>.</p>
    </step>
    <step>
        <p>Call <code>fixer.update(tagged, fromVersion, toVersion)</code> — receive the migrated <code>Dynamic&lt;T&gt;</code>.</p>
    </step>
</procedure>

## At a Glance

| Concept             | Role                                                                           |
|---------------------|--------------------------------------------------------------------------------|
| **DataVersion**     | Integer-based version identifier for a schema snapshot.                        |
| **TypeReference**   | String-based key that routes data to the correct fixes (e.g. `"player"`).      |
| **Schema**          | Associates a `DataVersion` with a `TypeRegistry` of typed references.          |
| **DataFix**         | A single migration step from one version to the next.                          |
| **Dynamic\<T\>**    | Format-agnostic wrapper around your data.                                      |
| **DynamicOps\<T\>** | Operations interface for a specific format (Gson, Jackson, SnakeYAML, …).      |

## Where to Go Next

<note>
    <p>
        This documentation is being set up. More topics will land here as the Writerside
        tree fills in — Getting Started, Core Concepts, How-To Guides, and integrations.
    </p>
</note>

<seealso style="cards">
    <category ref="aether-project">
        <a href="https://github.com/aether-framework/aether-datafixers"
           summary="Source code, issue tracker, and releases.">%product% on GitHub</a>
        <a href="https://central.sonatype.com/artifact/de.splatgames.aether.datafixers/aether-datafixers"
           summary="GPG-signed artifacts published to Maven Central.">Maven Central</a>
    </category>
    <category ref="aether-docs">
        <a href="https://github.com/aether-framework/aether-datafixers/blob/main/docs/getting-started/quick-start.md"
           summary="Installation, bootstrapping, and your first migration.">Getting Started</a>
        <a href="https://github.com/aether-framework/aether-datafixers/blob/main/docs/concepts/index.md"
           summary="Schemas, DataFixes, Dynamic, Codecs, and Optics explained.">Core Concepts</a>
        <a href="https://github.com/aether-framework/aether-datafixers/blob/main/docs/testkit/index.md"
           summary="Fluent builders, AssertJ assertions, and migration test harnesses.">Testkit</a>
        <a href="https://github.com/aether-framework/aether-datafixers/blob/main/docs/spring-boot/index.md"
           summary="Auto-configuration, MigrationService, Actuator, and metrics.">Spring Boot Starter</a>
    </category>
</seealso>
