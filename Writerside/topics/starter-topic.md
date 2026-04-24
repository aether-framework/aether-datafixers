# Welcome to %product%

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p><b>%tagline%</b></p>
    <p>Evolve serialized data as your code evolves &mdash; across JSON, YAML, TOML, XML, or any
       custom format &mdash; through one composable, format-agnostic API.</p>
    <p>Current release: <b>%latest_version%</b> · Requires Java %java_min%+ and Maven %maven_min%+.</p>
</tldr>

## What is %product%?

%product% lets you migrate persisted data as your code evolves. You define a
**schema per version** and the **fixes between them** &mdash; the framework picks the right
fixes, runs them in the right order, and hands back data at the target version.

It is inspired by Minecraft's *DataFixer Upper (DFU)*, but designed from the ground
up for **simplicity**, **clarity**, and **developer ergonomics** on the modern JVM.

<deflist type="medium">
    <def title="Forward patching">
        Data migrates old &rarr; new through a chain of <code>DataFix</code> instances.
        No backward patching, no rollbacks &mdash; one direction keeps the mental model clean.
    </def>
    <def title="Format agnostic">
        Works with JSON, YAML, TOML, XML, or any custom format via a single
        <code>DynamicOps&lt;T&gt;</code> abstraction. The same fix runs everywhere.
    </def>
    <def title="Immutable &amp; thread-safe">
        Core types are immutable; fixers are safe for concurrent, high-throughput use.
    </def>
    <def title="Modular by design">
        Pick only what you need &mdash; <code>%artifact_core%</code>, a codec, the Spring Boot
        starter, the CLI, the testkit, or schema tools.
    </def>
    <def title="Testable out of the box">
        Fluent test builders, custom AssertJ assertions, and dedicated migration harnesses
        ship in <code>%artifact_testkit%</code>.
    </def>
</deflist>

## Install

<tabs group="build">
<tab title="Maven" group-key="maven">

Use the Bill&nbsp;of&nbsp;Materials to keep every module on a matching version.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>%group_id%</groupId>
            <artifactId>%artifact_bom%</artifactId>
            <version>%latest_version%</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>%group_id%</groupId>
        <artifactId>%artifact_core%</artifactId>
    </dependency>
    <dependency>
        <groupId>%group_id%</groupId>
        <artifactId>%artifact_codec%</artifactId>
    </dependency>
</dependencies>
```

</tab>
<tab title="Gradle (Kotlin)" group-key="gradle-kts">

```kotlin
dependencies {
    implementation(platform("%group_id%:%artifact_bom%:%latest_version%"))
    implementation("%group_id%:%artifact_core%")
    implementation("%group_id%:%artifact_codec%")
    testImplementation("%group_id%:%artifact_testkit%")
}
```

</tab>
<tab title="Gradle (Groovy)" group-key="gradle">

```groovy
dependencies {
    implementation platform("%group_id%:%artifact_bom%:%latest_version%")
    implementation "%group_id%:%artifact_core%"
    implementation "%group_id%:%artifact_codec%"
    testImplementation "%group_id%:%artifact_testkit%"
}
```

</tab>
</tabs>

> Artifacts are GPG-signed and published to [Maven Central](%maven_central_url%).
> {style="note"}

## Your First Migration

<procedure title="From raw data to migrated data" id="four-step-migration">
    <step>
        <p>Add <code>%artifact_core%</code> and a codec of your choice (e.g.
           <code>%artifact_codec%</code>) to your build.</p>
    </step>
    <step>
        <p>Implement a <code>DataFixerBootstrap</code> that registers your
           <code>Schema</code>&apos;s and <code>DataFix</code>&apos;es.</p>
    </step>
    <step>
        <p>Build the fixer via
           <code>new DataFixerRuntimeFactory().create(currentVersion, bootstrap)</code>.</p>
    </step>
    <step>
        <p>Call <code>fixer.update(type, dynamic, fromVersion, toVersion)</code> and
           receive migrated data at the target version.</p>
    </step>
</procedure>

A minimal end-to-end example using Gson:

```java
// 1. Build the fixer from your bootstrap
AetherDataFixer fixer = new DataFixerRuntimeFactory()
        .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

// 2. Wrap raw JSON in a Dynamic
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, rawJson);

// 3. Migrate from the saved version up to the current one
Dynamic<JsonElement> migrated = fixer.update(
        TypeReferences.PLAYER,
        dynamic,
        new DataVersion(100),   // stored in the save file
        fixer.currentVersion()
);
```

> Prefer working with `TaggedDynamic`? `AetherDataFixer` also accepts
> `fixer.update(TaggedDynamic, fromVersion, toVersion)` as a convenience overload.
> {style="tip"}

## Core Vocabulary {id="vocabulary"}

<deflist type="full">
    <def title="DataVersion">
        An integer-based version identifier. Higher values are newer. Conventionally
        SemVer-encoded (<code>100</code> = 1.0.0, <code>110</code> = 1.1.0, <code>200</code> = 2.0.0).
    </def>
    <def title="TypeReference">
        A string key that routes data to the correct fixes &mdash; e.g. <code>"player"</code>,
        <code>"world"</code>, <code>"block_entity"</code>.
    </def>
    <def title="Schema">
        A snapshot of your type definitions at a given <code>DataVersion</code>. Schemas
        inherit from parents; children only register types that changed.
    </def>
    <def title="DataFix">
        A single migration step that transforms data from one version to the next.
        Most fixes extend <code>SchemaDataFix</code> and express themselves as a
        <code>TypeRewriteRule</code>.
    </def>
    <def title="Dynamic&lt;T&gt;">
        A format-agnostic wrapper around your data. Carries a reference to the format
        (<code>DynamicOps&lt;T&gt;</code>) so the same fix logic works everywhere.
    </def>
    <def title="DynamicOps&lt;T&gt;">
        The operations interface for a concrete format &mdash; <code>GsonOps</code>,
        <code>JacksonJsonOps</code>, <code>SnakeYamlOps</code>, <code>JacksonYamlOps</code>,
        <code>JacksonTomlOps</code>, <code>JacksonXmlOps</code>, or your own implementation.
    </def>
</deflist>

## Module Map

| Module                               | Role                                                                           |
|--------------------------------------|--------------------------------------------------------------------------------|
| <code>%artifact_api%</code>          | Core interfaces and API contracts (no implementation).                         |
| <code>%artifact_core%</code>         | Default implementations &mdash; <code>AetherDataFixer</code> &amp; bootstrap.  |
| <code>%artifact_codec%</code>        | <code>DynamicOps</code> for Gson, Jackson (JSON/YAML/TOML/XML), and SnakeYAML. |
| <code>%artifact_testkit%</code>      | Fluent test data builders, AssertJ assertions, migration harnesses.            |
| <code>%artifact_spring_boot%</code>  | Spring Boot auto-configuration, Actuator endpoint, metrics.                    |
| <code>%artifact_cli%</code>          | Command-line runner for one-off migrations.                                    |
| <code>%artifact_schema_tools%</code> | Schema inspection and diffing utilities.                                       |
| <code>%artifact_bom%</code>          | Bill of Materials for aligned versions.                                        |

## Where to Go Next

<seealso style="cards">
    <category ref="wrs">
        <a href="overview.topic"
           summary="Schemas, DataFixes, Dynamic, Codecs, Optics, DataResult &mdash; the full model.">
           Core Concepts</a>
    </category>
    <category ref="aether-project">
        <a href="%github_url%"
           summary="Source code, issues, and releases on GitHub.">
           %product% on GitHub</a>
        <a href="%maven_central_url%"
           summary="GPG-signed artifacts on Maven Central.">
           Maven Central</a>
        <a href="%issues_url%"
           summary="Report bugs, request features, or ask questions.">
           Issue tracker</a>
    </category>
</seealso>

<note>
    <p>
        This documentation is in active development. More topics &mdash; Getting Started,
        How-To Guides, Codec Reference, Spring Boot, Testkit, and Migration Playbooks
        &mdash; will land here as the tree fills in.
    </p>
</note>
