# Performance Tuning Guide

This guide provides concrete benchmark data, format performance comparisons, and optimization strategies for Aether Datafixers. All numbers are from the v1.0.0 JMH benchmark suite running on a dedicated server.

## Table of Contents

- [Benchmark Baseline](#benchmark-baseline-v100)
- [Format Performance Comparison](#format-performance-comparison)
- [Codec Performance](#codec-performance)
- [Concurrent Performance](#concurrent-performance)
- [Memory Optimization](#memory-optimization)
- [Batch Processing Patterns](#batch-processing-patterns)
- [Optimization Tips](#optimization-tips)

---

## Benchmark Baseline (v1.0.0)

### Test Environment

| Parameter  | Value                                                         |
|------------|---------------------------------------------------------------|
| CPU        | Intel Core i9-9900K @ 3.60 GHz (4 cores allocated)            |
| Memory     | 8 GB RAM, 4 GB JVM heap                                       |
| JVM        | OpenJDK 17.0.18 (HotSpot, mixed mode)                         |
| OS         | Ubuntu 22.04, Linux 5.15                                      |
| JVM flags  | `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+AlwaysPreTouch`              |
| JMH        | 1.37, 3 forks, 5 warmup iterations, 10 measurement iterations |

All benchmarks use [PayloadSize](../../aether-datafixers-benchmarks/src/main/java/de/splatgames/aether/datafixers/benchmarks/util/PayloadSize.java) definitions:
- **SMALL**: 5 fields, 2 nesting levels (~2 KB)
- **MEDIUM**: 20 fields, 4 nesting levels (~20 KB)
- **LARGE**: 50 fields, 6 nesting levels (~200 KB)

### Single Fix Performance

Measures the cost of applying one `DataFix` to a `Dynamic` value.

| Benchmark             | Payload  |  Throughput (ops/µs) |   Latency (µs/op) |
|-----------------------|----------|---------------------:|------------------:|
| identityFix (no-op)   | SMALL    |                4.035 |             0.249 |
| identityFix (no-op)   | MEDIUM   |                4.030 |             0.250 |
| identityFix (no-op)   | LARGE    |                3.996 |             0.256 |
| singleRenameFix       | SMALL    |                3.633 |             0.281 |
| singleRenameFix       | MEDIUM   |                3.376 |             0.300 |
| singleRenameFix       | LARGE    |                3.233 |             0.311 |
| playerDataFix         | —        |                0.109 |             9.147 |
| playerDataFixEndToEnd | —        |                0.032 |            31.194 |

**Key takeaways:**

- **Framework overhead is ~0.25 µs** per fix invocation (identity fix baseline).
- A simple rename adds only ~0.03–0.06 µs on top of the framework overhead.
- Payload size has minimal impact on simple field operations — the cost scales with fix complexity, not data size.
- A realistic domain fix (player data with multiple field transformations) takes ~9 µs.
- End-to-end migration (including schema lookup and type routing) adds ~22 µs overhead on top of the raw fix.

### Fix Chain Scaling

Measures how performance scales when chaining multiple fixes in sequence.

#### Rename Chain (homogeneous fixes)

|  Fix Count | Payload  |  Throughput (ops/µs) |   Latency (µs/op) |
|-----------:|----------|---------------------:|------------------:|
|          1 | SMALL    |                3.727 |             0.264 |
|          1 | MEDIUM   |                3.590 |             0.279 |
|          5 | SMALL    |                1.023 |             0.963 |
|          5 | MEDIUM   |                0.954 |             1.043 |
|         10 | SMALL    |                0.559 |             1.775 |
|         10 | MEDIUM   |                0.517 |             1.947 |
|         25 | SMALL    |                0.206 |             4.512 |
|         25 | MEDIUM   |                0.188 |             5.034 |
|         50 | SMALL    |                0.112 |             8.807 |
|         50 | MEDIUM   |                0.104 |             9.627 |

#### Mixed Chain (rename + add + remove + transform)

|  Fix Count | Payload  |  Throughput (ops/µs) |   Latency (µs/op) |
|-----------:|----------|---------------------:|------------------:|
|          1 | SMALL    |                3.739 |             0.267 |
|          1 | MEDIUM   |                3.583 |             0.277 |
|          5 | SMALL    |                0.231 |             4.341 |
|          5 | MEDIUM   |                0.040 |            25.091 |
|         10 | SMALL    |                0.095 |            10.582 |
|         10 | MEDIUM   |                0.016 |            63.693 |
|         25 | SMALL    |                0.037 |            26.926 |
|         25 | MEDIUM   |                0.007 |           151.686 |
|         50 | SMALL    |                0.016 |            60.748 |
|         50 | MEDIUM   |                0.003 |           320.922 |

**Key takeaways:**

- **Rename chains scale linearly**: 50 renames take ~50x the time of a single rename. Each fix adds a constant ~0.17 µs.
- **Mixed chains are significantly more expensive**: At 50 fixes with MEDIUM payload, mixed chains are **33x slower** than rename chains (320 µs vs 9.6 µs). This is because add/remove/transform operations involve structural changes to the `Dynamic` tree.
- MEDIUM payloads amplify the cost of structural operations (add/remove fields iterate over more data), while rename operations show only ~10% overhead between SMALL and MEDIUM.

### Schema Lookup Performance

Measures `SchemaRegistry` lookup speed with varying registry sizes.

| Lookup Type      | 10 Schemas |  50 Schemas |  100 Schemas |  500 Schemas |
|------------------|-----------:|------------:|-------------:|-------------:|
| latestLookup     |     5.7 ns |      6.9 ns |       7.2 ns |       8.3 ns |
| exactLookup      |     9.3 ns |     24.0 ns |      33.3 ns |      51.6 ns |
| floorLookup      |    16.1 ns |     29.7 ns |      35.9 ns |      53.5 ns |
| sequentialLookup |    67.4 ns |    494.1 ns |     1,090 ns |    10,783 ns |

**Key takeaways:**

- **latestLookup is O(1)**: Near-constant time regardless of registry size. Always use `getCurrentVersion()` when you know you need the latest schema.
- **exactLookup and floorLookup are O(log n)**: Based on `TreeMap` lookups. Even with 500 schemas, lookup takes only ~50 ns.
- **sequentialLookup is O(n)**: Iterates through all schemas — 10.8 µs at 500 schemas. Avoid sequential iteration in hot paths.

---

## Format Performance Comparison

### DynamicOps Field Operations

Measures raw field read, set, and object generation performance per `DynamicOps` implementation.

#### Field Read (ops/µs, higher is better)

| Format         |  SMALL |  MEDIUM |  LARGE |
|----------------|-------:|--------:|-------:|
| SnakeYamlOps   |  116.5 |   108.3 |  116.1 |
| JacksonTomlOps |   62.3 |    62.4 |      — |
| JacksonXmlOps  |   61.0 |    62.3 |      — |
| JacksonJsonOps |   60.1 |    60.7 |   62.0 |
| JacksonYamlOps |   59.1 |    61.6 |   60.9 |
| GsonOps        |   37.1 |    38.9 |   33.4 |

#### Field Set (ops/µs, higher is better)

| Format         |  SMALL |  MEDIUM |  LARGE |
|----------------|-------:|--------:|-------:|
| SnakeYamlOps   |  5.597 |   1.205 |  0.525 |
| JacksonXmlOps  |  0.891 |   0.127 |      — |
| JacksonYamlOps |  0.888 |   0.126 |  0.014 |
| JacksonJsonOps |  0.883 |   0.126 |  0.014 |
| JacksonTomlOps |  0.880 |   0.129 |      — |
| GsonOps        |  0.650 |   0.086 |  0.011 |

#### Object Generation (ops/µs, higher is better)

| Format         |  SMALL |  MEDIUM |  LARGE |
|----------------|-------:|--------:|-------:|
| JacksonXmlOps  |  0.150 |   0.015 |      — |
| JacksonYamlOps |  0.149 |   0.015 | 0.0019 |
| JacksonJsonOps |  0.148 |   0.015 | 0.0019 |
| JacksonTomlOps |  0.148 |   0.015 |      — |
| GsonOps        |  0.106 |   0.007 | 0.0008 |
| SnakeYamlOps   |  0.107 |   0.012 | 0.0014 |

**Key takeaways:**

- **SnakeYamlOps is the fastest for field reads** — 2–3x faster than Jackson-based implementations. This is because SnakeYaml uses native Java `Map`/`List` types with direct `HashMap.get()` lookups, while Jackson and Gson use tree node wrappers (`ObjectNode`, `JsonObject`).
- **SnakeYamlOps is also the fastest for field sets** — 6x faster than Jackson at SMALL payloads, because Java `HashMap.put()` is an in-place mutation, while Jackson's `ObjectNode.set()` involves tree copying.
- **All Jackson-based formats perform identically** for in-memory operations. JacksonJsonOps, JacksonYamlOps, JacksonTomlOps, and JacksonXmlOps share the same `ObjectNode`/`ArrayNode` tree model — format differences only matter during serialization/deserialization.
- **GsonOps is consistently the slowest** for field operations due to `JsonObject.deepCopy()` on mutations.

### Migration Throughput

Measures end-to-end `DataFixer.update()` throughput per format (single rename fix).

| Format         |  SMALL (ops/µs) |  MEDIUM (ops/µs) |  LARGE (ops/µs) |
|----------------|----------------:|-----------------:|----------------:|
| JacksonJsonOps |           3.709 |            3.759 |           3.734 |
| JacksonYamlOps |           3.728 |            3.733 |           3.585 |
| SnakeYamlOps   |           3.730 |            3.726 |           3.636 |
| JacksonTomlOps |           3.633 |            3.636 |               — |
| GsonOps        |           3.628 |            3.314 |           3.231 |
| JacksonXmlOps  |           3.620 |            3.644 |               — |

**Key takeaway:** Migration throughput is nearly identical across all formats (~3.6–3.7 ops/µs). The DataFixer framework overhead dominates over format-specific differences. Choose your format based on your application's needs, not migration speed.

### Cross-Format Conversion

Measures converting a `Dynamic` from one `DynamicOps` to another.

| Conversion                  |  SMALL (ops/µs) |  MEDIUM (ops/µs) |
|-----------------------------|----------------:|-----------------:|
| SnakeYaml → Gson            |          0.0360 |           0.0045 |
| SnakeYaml → Jackson YAML    |          0.0367 |           0.0046 |
| Jackson YAML → Jackson JSON |          0.0172 |           0.0023 |
| Jackson YAML → SnakeYaml    |          0.0174 |           0.0023 |
| Jackson JSON → YAML         |          0.0172 |           0.0023 |
| Jackson → Gson              |          0.0169 |           0.0023 |
| Gson → SnakeYaml            |          0.0164 |           0.0021 |
| Gson → Jackson              |          0.0163 |           0.0021 |

**Key takeaway:** Conversions **from** SnakeYaml are ~2x faster than conversions from tree-node-based formats. If you need to convert between formats frequently, consider using SnakeYaml as the intermediate representation.

### Format Selection Guide

| Format             | Best For                                                          | Avoid When                                                                          | Relative Speed      |
|--------------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------------|---------------------|
| **SnakeYamlOps**   | High-throughput processing, large datasets, read-heavy workloads  | You need YAML serialization features (anchors, tags) in a performance-critical path | Fastest (field ops) |
| **JacksonJsonOps** | General-purpose JSON, large file streaming, ecosystem integration | Minimal dependencies are critical                                                   | Fast                |
| **JacksonYamlOps** | Config files, human-editable data, YAML interop                   | High-throughput field mutations                                                     | Fast                |
| **JacksonTomlOps** | Configuration files, structured settings                          | Deeply nested data, large arrays                                                    | Fast                |
| **JacksonXmlOps**  | Legacy XML integration, SOAP/enterprise systems                   | Performance-critical paths                                                          | Fast                |
| **GsonOps**        | Simple JSON, small payloads, minimal setup                        | Large datasets, write-heavy workloads                                               | Slowest             |

---

## Codec Performance

### Primitive Type Operations

Measures encode and decode throughput for individual primitive values.

| Type    |  Encode (ops/ns) |  Decode (ops/ns) |
|---------|-----------------:|-----------------:|
| String  |            0.255 |            0.217 |
| Integer |            0.250 |            0.233 |
| Boolean |            0.241 |            0.224 |
| Float   |            0.156 |            0.152 |
| Long    |            0.147 |            0.143 |
| Double  |            0.149 |            0.139 |

**Key takeaways:**

- String, Integer, and Boolean codecs operate at ~4 ns per operation — effectively free in the context of a migration.
- Float, Long, and Double are ~40% slower due to boxing and number conversion overhead but still under 7 ns per operation.
- Encoding is consistently ~5–10% faster than decoding.

### Collection Scaling

Measures how list codec performance scales with collection size.

#### Integer Lists

|  List Size |   Decode (ops/µs) |   Encode (ops/µs) |  Round-Trip (ops/µs) |
|-----------:|------------------:|------------------:|---------------------:|
|         10 |             14.38 |              8.34 |                 5.57 |
|        100 |              1.78 |              0.76 |                 0.69 |
|      1,000 |              0.15 |              0.11 |                0.063 |

#### String Lists

|  List Size |   Decode (ops/µs) |   Encode (ops/µs) |  Round-Trip (ops/µs) |
|-----------:|------------------:|------------------:|---------------------:|
|         10 |             18.79 |              9.08 |                 6.00 |
|        100 |              2.45 |              0.93 |                 0.79 |
|      1,000 |              0.24 |              0.14 |                0.078 |

**Key takeaways:**

- Collection codecs scale linearly — 10x the items costs ~10x the time.
- Decoding is ~1.7x faster than encoding for both types.
- String lists are slightly faster than integer lists due to avoiding unboxing overhead.
- **Functional vs. direct round-trip**: Performance is identical for integer lists. For string lists, the functional API is ~15% slower at small sizes but converges at larger sizes.

---

## Concurrent Performance

### Thread Scaling

Measures migration throughput under concurrent load.

| Operation                |   2 Threads (ops/µs) |   4 Threads (ops/µs) |   8 Threads (ops/µs) |
|--------------------------|---------------------:|---------------------:|---------------------:|
| Single fix (SMALL)       |                12.32 |                12.32 |                12.54 |
| Single fix (MEDIUM)      |                11.54 |                11.49 |                11.47 |
| Chain migration (SMALL)  |                 1.91 |                    — |                    — |
| Chain migration (MEDIUM) |                 1.77 |                    — |                    — |

### Concurrent Registry Operations

| Operation       |   2 Threads (ops/µs) |
|-----------------|---------------------:|
| Latest lookup   |        481.9 – 490.7 |
| Registry lookup |                111.1 |

**Key takeaways:**

- **DataFixer is fully thread-safe** with zero lock contention. Throughput stays constant from 2 to 8 threads — the framework scales linearly with available cores.
- **Registry lookups are lock-free**: 481 ops/µs for latest lookup even under contention (2+ threads reading concurrently).
- Migration throughput at 8 threads: **~12 million SMALL fix applications per second** and **~11.5 million MEDIUM fix applications per second**.
- Payload size matters more than thread count: SMALL → MEDIUM causes ~7% throughput reduction, while doubling threads has no measurable impact.

---

## Memory Optimization

### Heap Sizing Recommendations

| Workload   |             Records |  Recommended Heap | JVM Flags                                             |
|------------|--------------------:|------------------:|-------------------------------------------------------|
| Small      |             < 1,000 |   256 MB – 512 MB | `-Xms256m -Xmx512m`                                   |
| Medium     |     1,000 – 100,000 |       1 GB – 2 GB | `-Xms1g -Xmx2g`                                       |
| Large      | 100,000 – 1,000,000 |       2 GB – 4 GB | `-Xms2g -Xmx4g -XX:+UseG1GC`                          |
| Very large |         > 1,000,000 |             4 GB+ | `-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200` |

### GC Tuning for Migration Workloads

Migrations create many short-lived intermediate objects (Dynamic wrappers, field copies). G1GC handles this well:

```bash
java \
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:+AlwaysPreTouch \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -jar my-app.jar
```

- **`AlwaysPreTouch`**: Pre-allocates heap memory at startup, avoiding page faults during migration.
- **`MaxGCPauseMillis=200`**: Keeps GC pauses under 200 ms — suitable for batch processing.
- **`G1HeapRegionSize=16m`**: Larger regions reduce overhead for workloads with many medium-sized objects.

### Streaming vs. In-Memory Processing

For very large datasets, avoid loading all records into memory at once:

```java
// In-memory: loads everything — fine for < 100k records
List<TaggedDynamic> all = loadAll();
List<TaggedDynamic> migrated = all.stream()
    .map(item -> fixer.update(item, fromVersion, toVersion))
    .toList();

// Streaming: processes one record at a time — use for > 100k records
try (Stream<TaggedDynamic> stream = loadStream()) {
    stream
        .map(item -> fixer.update(item, fromVersion, toVersion))
        .forEach(this::save);
}
```

---

## Batch Processing Patterns

### Simple Batching

Process records in fixed-size batches to bound memory usage:

```java
List<JsonObject> batch = new ArrayList<>(BATCH_SIZE);

for (JsonObject item : largeDataset) {
    batch.add(item);
    if (batch.size() >= BATCH_SIZE) {
        processBatch(batch);
        batch.clear();
    }
}
processBatch(batch); // remaining items
```

### Parallel Processing

DataFixer is thread-safe — use parallel streams for CPU-bound migrations:

```java
List<TaggedDynamic> items = loadItems();

List<TaggedDynamic> migrated = items.parallelStream()
    .map(item -> fixer.update(item, fromVersion, toVersion))
    .toList();
```

For controlling parallelism, use a custom `ForkJoinPool`:

```java
ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

List<TaggedDynamic> migrated = pool.submit(() ->
    items.parallelStream()
        .map(item -> fixer.update(item, fromVersion, toVersion))
        .toList()
).get();
```

### Chunked File Processing

For large files that cannot fit in memory, process in chunks:

```java
int chunkSize = 10_000;

try (BufferedReader reader = Files.newBufferedReader(path)) {
    List<String> chunk = new ArrayList<>(chunkSize);
    String line;

    while ((line = reader.readLine()) != null) {
        chunk.add(line);
        if (chunk.size() >= chunkSize) {
            migrateChunk(chunk, fixer, fromVersion, toVersion);
            chunk.clear();
        }
    }
    migrateChunk(chunk, fixer, fromVersion, toVersion);
}
```

---

## Optimization Tips

### Lazy Schema Initialization

Schemas are initialized lazily by default. Only schemas accessed during migration are fully constructed — no upfront cost for unused versions.

### Minimize Dynamic Operations

```java
// Avoid: Multiple individual field accesses
String name = data.get("name").asString().orElse("");
int level = data.get("level").asInt().orElse(0);
int health = data.get("health").asInt().orElse(100);

// Better: Single transformation pass
return data.update("stats", this::transformStats);
```

### Cache Reused Values

```java
private final DataVersion targetVersion = fixer.getCurrentVersion();

public void migrateItem(JsonObject item, int version) {
    if (version >= targetVersion.version()) {
        return; // skip current version
    }
    // migrate...
}
```

### Skip Unchanged Data

```java
public TaggedDynamic migrateIfNeeded(TaggedDynamic data, DataVersion version) {
    if (!version.isOlderThan(fixer.getCurrentVersion())) {
        return data; // already current
    }
    return fixer.update(data, version, fixer.getCurrentVersion());
}
```

### Profiling

```java
long start = System.nanoTime();
TaggedDynamic result = fixer.update(data, from, to);
long elapsed = System.nanoTime() - start;

if (elapsed > THRESHOLD_NANOS) {
    logger.warn("Slow migration: {}ms for {}",
        elapsed / 1_000_000, data.type().id());
}
```

### Choose the Right Lookup Strategy

Based on the [schema lookup benchmarks](#schema-lookup-performance):

- Use `getCurrentVersion()` for the latest schema — **O(1), 5.7 ns**
- Use `getSchema(version)` for a specific version — **O(log n), 9–52 ns**
- Avoid iterating schemas sequentially — **O(n), up to 10.8 µs at 500 schemas**

---

## Related

- [Concurrent Migrations](concurrent-migrations.md)
- [Debug Migrations](../how-to/debug-migrations.md)
- [Monitoring & Alerting](../operations/monitoring-alerting.md)
- [Benchmark Module](../../aether-datafixers-benchmarks/) — JMH benchmark source code
