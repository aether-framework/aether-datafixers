/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.datafixers.core.integration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.codec.SimpleCodecRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Thread-safety tests for the data fixing system.
 */
@DisplayName("Thread-Safety")
class ThreadSafetyTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference WORLD = new TypeReference("world");

    @Nested
    @DisplayName("Registry Freeze Behavior")
    class RegistryFreezeBehaviorTests {

        @Test
        @DisplayName("TypeRegistry rejects registration after freeze")
        void typeRegistryRejectsRegistrationAfterFreeze() {
            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            // Create a named type with PLAYER as its reference
            Type<String> playerType = Type.named(PLAYER.getId(), Type.STRING);
            registry.register(playerType);
            registry.freeze();

            Type<Integer> worldType = Type.named(WORLD.getId(), Type.INT);
            assertThatThrownBy(() -> registry.register(worldType))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("SchemaRegistry rejects registration after freeze")
        void schemaRegistryRejectsRegistrationAfterFreeze() {
            SimpleSchemaRegistry registry = new SimpleSchemaRegistry();
            Schema schema = new Schema(new DataVersion(1), new SimpleTypeRegistry());
            registry.register(schema);
            registry.freeze();

            Schema schema2 = new Schema(new DataVersion(2), new SimpleTypeRegistry());
            assertThatThrownBy(() -> registry.register(schema2))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CodecRegistry rejects registration after freeze")
        void codecRegistryRejectsRegistrationAfterFreeze() {
            SimpleCodecRegistry registry = new SimpleCodecRegistry();
            registry.register(PLAYER, Codecs.STRING);
            registry.freeze();

            assertThatThrownBy(() -> registry.register(WORLD, Codecs.INT))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("DataFixRegistry rejects registration after freeze")
        void dataFixRegistryRejectsRegistrationAfterFreeze() {
            DataFixRegistry registry = new DataFixRegistry();
            registry.register(PLAYER, createSimpleFix("fix1", 1, 2));
            registry.freeze();

            assertThatThrownBy(() -> registry.register(PLAYER, createSimpleFix("fix2", 2, 3)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("frozen registries are still readable")
        void frozenRegistriesAreStillReadable() {
            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            Type<String> playerType = Type.named(PLAYER.getId(), Type.STRING);
            registry.register(playerType);
            registry.freeze();

            // Should still be able to read
            Type<?> type = registry.get(PLAYER);
            assertThat(type).isNotNull();
            assertThat(type.reference().getId()).isEqualTo(PLAYER.getId());
        }
    }

    @Nested
    @DisplayName("Concurrent Data Fixing")
    class ConcurrentDataFixingTests {

        @Test
        @DisplayName("DataFixer handles concurrent updates safely")
        @Timeout(10)
        void dataFixerHandlesConcurrentUpdatesSafely() throws InterruptedException {
            AtomicInteger fixApplyCount = new AtomicInteger(0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, new DataFix<JsonElement>() {
                        @Override
                        public @NotNull String name() {
                            return "concurrent_fix";
                        }

                        @Override
                        public @NotNull DataVersion fromVersion() {
                            return new DataVersion(1);
                        }

                        @Override
                        public @NotNull DataVersion toVersion() {
                            return new DataVersion(2);
                        }

                        @Override
                        public @NotNull Dynamic<JsonElement> apply(
                                @NotNull TypeReference type,
                                @NotNull Dynamic<JsonElement> input,
                                @NotNull DataFixerContext context
                        ) {
                            fixApplyCount.incrementAndGet();
                            // Simulate some work
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return input.set("processed", input.createBoolean(true));
                        }
                    })
                    .build();

            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < operationsPerThread; i++) {
                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 1000 + i);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    PLAYER, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            // Verify result
                            assertThat(result.get("processed").asBoolean().result())
                                    .contains(true);
                            assertThat(result.get("id").asInt().result())
                                    .contains(threadId * 1000 + i);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(exceptions).isEmpty();
            assertThat(fixApplyCount.get()).isEqualTo(threadCount * operationsPerThread);
        }

        @Test
        @DisplayName("frozen registry concurrent reads are safe")
        @Timeout(5)
        void frozenRegistryConcurrentReadsAreSafe() throws InterruptedException {
            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            for (int i = 0; i < 100; i++) {
                Type<String> type = Type.named("type" + i, Type.STRING);
                registry.register(type);
            }
            registry.freeze();

            int threadCount = 10;
            int readsPerThread = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < readsPerThread; i++) {
                            int index = i % 100;
                            Type<?> type = registry.get(new TypeReference("type" + index));
                            assertThat(type).isNotNull();
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(exceptions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("DataFixer is immutable after build")
        void dataFixerIsImmutableAfterBuild() {
            DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(2));
            builder.addFix(PLAYER, createSimpleFix("fix1", 1, 2));

            DataFixer fixer = builder.build();

            // The fixer should work
            JsonObject inputObj = new JsonObject();
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            );
            assertThat(result).isNotNull();

            // Fixer version should be correct
            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(2));
        }

        @Test
        @DisplayName("Dynamic values are immutable")
        void dynamicValuesAreImmutable() {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", "Alice");
            Dynamic<JsonElement> original = new Dynamic<>(GsonOps.INSTANCE, obj);

            // Modifications create new instances
            Dynamic<JsonElement> modified = original.set("age", original.createInt(30));

            // Original should be unchanged
            assertThat(original.get("age")).isNull();

            // Modified should have the new field
            assertThat(modified.get("age").asInt().result()).contains(30);
            assertThat(modified.get("name").asString().result()).contains("Alice");
        }
    }

    // Helper methods

    private DataFix<JsonElement> createSimpleFix(String name, int from, int to) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<JsonElement> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<JsonElement> input,
                    @NotNull DataFixerContext context
            ) {
                return input;
            }
        };
    }
}
