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

package de.splatgames.aether.datafixers.api.rewrite;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.TestOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BatchTransform}.
 */
@DisplayName("BatchTransform")
class BatchTransformTest {

    private static final TestOps OPS = TestOps.INSTANCE;
    private BatchTransform<Object> batch;

    @BeforeEach
    void setUp() {
        batch = new BatchTransform<>(OPS);
    }

    @Nested
    @DisplayName("rename()")
    class RenameTests {

        @Test
        @DisplayName("renames existing field")
        void renamesExistingField() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("oldName", "value"));

            final Dynamic<Object> result = batch.rename("oldName", "newName").apply(input);

            assertThat(result.get("oldName")).isNull();
            assertThat(result.get("newName")).isNotNull();
            assertThat(result.get("newName").asString().result()).contains("value");
        }

        @Test
        @DisplayName("does nothing if field does not exist")
        void doesNothingIfFieldDoesNotExist() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("other", "value"));

            final Dynamic<Object> result = batch.rename("missing", "newName").apply(input);

            assertThat(result.get("missing")).isNull();
            assertThat(result.get("newName")).isNull();
            assertThat(result.get("other")).isNotNull();
        }
    }

    @Nested
    @DisplayName("remove()")
    class RemoveTests {

        @Test
        @DisplayName("removes existing field")
        void removesExistingField() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("toRemove", "value", "keep", "other"));

            final Dynamic<Object> result = batch.remove("toRemove").apply(input);

            assertThat(result.get("toRemove")).isNull();
            assertThat(result.get("keep")).isNotNull();
        }

        @Test
        @DisplayName("does nothing if field does not exist")
        void doesNothingIfFieldDoesNotExist() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("keep", "value"));

            final Dynamic<Object> result = batch.remove("missing").apply(input);

            assertThat(result.get("keep")).isNotNull();
        }
    }

    @Nested
    @DisplayName("set()")
    class SetTests {

        @Test
        @DisplayName("sets field with computed value")
        void setsFieldWithComputedValue() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("name", "Alice"));

            final Dynamic<Object> result = batch
                    .set("greeting", d -> d.createString("Hello, " + d.get("name").asString().result().orElse("unknown")))
                    .apply(input);

            assertThat(result.get("greeting")).isNotNull();
            assertThat(result.get("greeting").asString().result()).contains("Hello, Alice");
        }

        @Test
        @DisplayName("overwrites existing value")
        void overwritesExistingValue() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("version", 1));

            final Dynamic<Object> result = batch
                    .set("version", d -> d.createInt(2))
                    .apply(input);

            assertThat(result.get("version").asInt().result()).contains(2);
        }
    }

    @Nested
    @DisplayName("setStatic()")
    class SetStaticTests {

        @Test
        @DisplayName("sets field with static value")
        void setsFieldWithStaticValue() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("name", "Alice"));
            final Dynamic<Object> staticValue = new Dynamic<>(OPS, 42);

            final Dynamic<Object> result = batch.setStatic("answer", staticValue).apply(input);

            assertThat(result.get("answer")).isNotNull();
            assertThat(result.get("answer").asInt().result()).contains(42);
        }
    }

    @Nested
    @DisplayName("transform()")
    class TransformTests {

        @Test
        @DisplayName("transforms existing field value")
        void transformsExistingFieldValue() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("count", 10));

            final Dynamic<Object> result = batch
                    .transform("count", d -> d.createInt(d.asInt().result().orElse(0) * 2))
                    .apply(input);

            assertThat(result.get("count").asInt().result()).contains(20);
        }

        @Test
        @DisplayName("does nothing if field does not exist")
        void doesNothingIfFieldDoesNotExist() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("other", "value"));

            final Dynamic<Object> result = batch
                    .transform("missing", d -> d.createInt(999))
                    .apply(input);

            assertThat(result.get("missing")).isNull();
            assertThat(result.get("other")).isNotNull();
        }
    }

    @Nested
    @DisplayName("addIfMissing()")
    class AddIfMissingTests {

        @Test
        @DisplayName("adds field if missing")
        void addsFieldIfMissing() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("name", "Alice"));

            final Dynamic<Object> result = batch
                    .addIfMissing("version", d -> d.createInt(1))
                    .apply(input);

            assertThat(result.get("version")).isNotNull();
            assertThat(result.get("version").asInt().result()).contains(1);
        }

        @Test
        @DisplayName("does not overwrite existing field")
        void doesNotOverwriteExistingField() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("version", 5));

            final Dynamic<Object> result = batch
                    .addIfMissing("version", d -> d.createInt(1))
                    .apply(input);

            assertThat(result.get("version").asInt().result()).contains(5);
        }
    }

    @Nested
    @DisplayName("chained operations")
    class ChainedOperationsTests {

        @Test
        @DisplayName("applies multiple operations in order")
        void appliesMultipleOperationsInOrder() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of(
                    "playerName", "Alice",
                    "xp", 100,
                    "deprecated", true
            ));

            final Dynamic<Object> result = batch
                    .rename("playerName", "name")
                    .rename("xp", "experience")
                    .remove("deprecated")
                    .set("version", d -> d.createInt(2))
                    .apply(input);

            assertThat(result.get("playerName")).isNull();
            assertThat(result.get("xp")).isNull();
            assertThat(result.get("deprecated")).isNull();
            assertThat(result.get("name")).isNotNull();
            assertThat(result.get("name").asString().result()).contains("Alice");
            assertThat(result.get("experience")).isNotNull();
            assertThat(result.get("experience").asInt().result()).contains(100);
            assertThat(result.get("version")).isNotNull();
            assertThat(result.get("version").asInt().result()).contains(2);
        }

        @Test
        @DisplayName("operations can reference results of previous operations")
        void operationsCanReferenceResultsOfPreviousOperations() {
            final Dynamic<Object> input = new Dynamic<>(OPS, Map.of("count", 5));

            // First double, then add 10 to the result
            final Dynamic<Object> result = batch
                    .transform("count", d -> d.createInt(d.asInt().result().orElse(0) * 2))
                    .transform("count", d -> d.createInt(d.asInt().result().orElse(0) + 10))
                    .apply(input);

            // 5 * 2 = 10, then 10 + 10 = 20
            assertThat(result.get("count").asInt().result()).contains(20);
        }
    }

    @Nested
    @DisplayName("size() and isEmpty()")
    class SizeAndEmptyTests {

        @Test
        @DisplayName("empty batch has size 0")
        void emptyBatchHasSizeZero() {
            assertThat(batch.size()).isEqualTo(0);
            assertThat(batch.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("batch with operations has correct size")
        void batchWithOperationsHasCorrectSize() {
            batch.rename("a", "b")
                    .remove("c")
                    .set("d", d -> d.createInt(1));

            assertThat(batch.size()).isEqualTo(3);
            assertThat(batch.isEmpty()).isFalse();
        }
    }
}
