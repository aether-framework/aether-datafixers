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

package de.splatgames.aether.datafixers.api.optic;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Finder}.
 */
@DisplayName("Finder")
class FinderTest {

    private Dynamic<Object> dynamic(final Object value) {
        return new Dynamic<>(TestOps.INSTANCE, value);
    }

    private Map<String, Object> map(final Object... keysAndValues) {
        final var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }

    @Nested
    @DisplayName("field() finder")
    class FieldFinder {

        @Test
        @DisplayName("get() returns field value when present")
        void getReturnsFieldValue() {
            final Dynamic<?> data = dynamic(map("name", "Alice", "age", 30));
            final Finder<?> finder = Finder.field("name");

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("get() returns null when field is missing")
        void getReturnsNullWhenMissing() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("address");

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("set() replaces field value")
        void setReplacesFieldValue() {
            final Dynamic<?> data = dynamic(map("name", "Alice", "age", 30));
            final Finder<?> finder = Finder.field("name");

            final Dynamic<?> result = finder.set(data, dynamic("Bob"));

            assertThat(result.get("name").value()).isEqualTo("Bob");
            assertThat(result.get("age").value()).isEqualTo(30);
        }

        @Test
        @DisplayName("set() adds new field when missing")
        void setAddsNewField() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("city");

            final Dynamic<?> result = finder.set(data, dynamic("Boston"));

            assertThat(result.get("name").value()).isEqualTo("Alice");
            assertThat(result.get("city").value()).isEqualTo("Boston");
        }

        @Test
        @DisplayName("id() returns descriptive identifier")
        void idReturnsDescriptiveIdentifier() {
            final Finder<?> finder = Finder.field("username");

            assertThat(finder.id()).isEqualTo("field[username]");
        }
    }

    @Nested
    @DisplayName("index() finder")
    class IndexFinder {

        @Test
        @DisplayName("get() returns element at index")
        void getReturnsElementAtIndex() {
            final Dynamic<?> data = dynamic(List.of(10, 20, 30));
            final Finder<?> finder = Finder.index(1);

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo(20);
        }

        @Test
        @DisplayName("get() returns null for out-of-bounds index")
        void getReturnsNullForOutOfBounds() {
            final Dynamic<?> data = dynamic(List.of(10, 20));
            final Finder<?> finder = Finder.index(5);

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("set() replaces element at index")
        void setReplacesElementAtIndex() {
            final Dynamic<?> data = dynamic(List.of(10, 20, 30));
            final Finder<?> finder = Finder.index(1);

            final Dynamic<?> result = finder.set(data, dynamic(99));

            assertThat(result.value()).isEqualTo(List.of(10, 99, 30));
        }

        @Test
        @DisplayName("set() returns unchanged for out-of-bounds index")
        void setReturnsUnchangedForOutOfBounds() {
            final Dynamic<?> data = dynamic(List.of(10, 20));
            final Finder<?> finder = Finder.index(5);

            final Dynamic<?> result = finder.set(data, dynamic(99));

            assertThat(result.value()).isEqualTo(List.of(10, 20));
        }

        @Test
        @DisplayName("id() returns descriptive identifier")
        void idReturnsDescriptiveIdentifier() {
            final Finder<?> finder = Finder.index(3);

            assertThat(finder.id()).isEqualTo("index[3]");
        }
    }

    @Nested
    @DisplayName("identity() finder")
    class IdentityFinder {

        @Test
        @DisplayName("get() returns the root itself")
        void getReturnsRoot() {
            final Dynamic<?> data = dynamic(map("key", "value"));
            final Finder<?> finder = Finder.identity();

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isSameAs(data);
        }

        @Test
        @DisplayName("set() replaces the entire root")
        void setReplacesEntireRoot() {
            final Dynamic<?> data = dynamic(map("key", "value"));
            final Dynamic<?> newValue = dynamic("replaced");
            final Finder<?> finder = Finder.identity();

            final Dynamic<?> result = finder.set(data, newValue);

            assertThat(result).isSameAs(newValue);
        }

        @Test
        @DisplayName("id() returns 'identity'")
        void idReturnsIdentity() {
            assertThat(Finder.identity().id()).isEqualTo("identity");
        }
    }

    @Nested
    @DisplayName("remainder() finder")
    class RemainderFinder {

        @Test
        @DisplayName("get() returns all fields except excluded ones")
        void getReturnsAllExceptExcluded() {
            final Dynamic<?> data = dynamic(map(
                    "name", "Alice",
                    "age", 30,
                    "city", "Boston",
                    "country", "USA"
            ));
            final Finder<?> finder = Finder.remainder("name", "age");

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNotNull();
            assertThat(result.get("city").value()).isEqualTo("Boston");
            assertThat(result.get("country").value()).isEqualTo("USA");
            assertThat(result.get("name")).isNull();
            assertThat(result.get("age")).isNull();
        }

        @Test
        @DisplayName("get() returns null for non-map values")
        void getReturnsNullForNonMap() {
            final Dynamic<?> data = dynamic("not a map");
            final Finder<?> finder = Finder.remainder("name");

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("get() with no exclusions returns all fields")
        void getWithNoExclusionsReturnsAll() {
            final Dynamic<?> data = dynamic(map("a", 1, "b", 2));
            final Finder<?> finder = Finder.remainder();

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNotNull();
            assertThat(result.get("a").value()).isEqualTo(1);
            assertThat(result.get("b").value()).isEqualTo(2);
        }

        @Test
        @DisplayName("set() merges excluded fields with new remainder")
        void setMergesExcludedWithNewRemainder() {
            final Dynamic<?> data = dynamic(map(
                    "name", "Alice",
                    "age", 30,
                    "city", "Boston",
                    "country", "USA"
            ));
            final Finder<?> finder = Finder.remainder("name", "age");
            final Dynamic<?> newRemainder = dynamic(map(
                    "city", "New York",
                    "zip", "10001"
            ));

            final Dynamic<?> result = finder.set(data, newRemainder);

            // Excluded fields should be preserved from root
            assertThat(result.get("name").value()).isEqualTo("Alice");
            assertThat(result.get("age").value()).isEqualTo(30);
            // New remainder fields should be present
            assertThat(result.get("city").value()).isEqualTo("New York");
            assertThat(result.get("zip").value()).isEqualTo("10001");
            // Old remainder field "country" should be gone (replaced by new remainder)
            assertThat(result.get("country")).isNull();
        }

        @Test
        @DisplayName("set() returns root unchanged for non-map root")
        void setReturnsRootForNonMap() {
            final Dynamic<?> data = dynamic("not a map");
            final Finder<?> finder = Finder.remainder("name");

            final Dynamic<?> result = finder.set(data, dynamic(map("new", "value")));

            assertThat(result.value()).isEqualTo("not a map");
        }

        @Test
        @DisplayName("set() with empty new remainder keeps only excluded fields")
        void setWithEmptyRemainderKeepsOnlyExcluded() {
            final Dynamic<?> data = dynamic(map(
                    "name", "Alice",
                    "age", 30,
                    "city", "Boston"
            ));
            final Finder<?> finder = Finder.remainder("name", "age");
            final Dynamic<?> emptyRemainder = dynamic(map());

            final Dynamic<?> result = finder.set(data, emptyRemainder);

            assertThat(result.get("name").value()).isEqualTo("Alice");
            assertThat(result.get("age").value()).isEqualTo(30);
            assertThat(result.get("city")).isNull();
        }

        @Test
        @DisplayName("set() with overlapping keys prefers new remainder values")
        void setWithOverlappingKeysPrefersNewRemainder() {
            // Edge case: if newRemainder contains a key that matches an excluded field,
            // the excluded field from root wins (because we merge excluded first, then newRemainder)
            // Actually, we merge excluded INTO newRemainder, so newRemainder values should win
            // Let me check the implementation...
            // The impl does: mergeToMap(excludedMap, newValue) where excludedMap is base and newValue is merged in
            // So newValue (newRemainder) should overwrite excludedMap entries
            // But "name" and "age" are excluded, so they should come from root regardless
            final Dynamic<?> data = dynamic(map(
                    "name", "Alice",
                    "age", 30,
                    "city", "Boston"
            ));
            final Finder<?> finder = Finder.remainder("name");
            final Dynamic<?> newRemainder = dynamic(map(
                    "age", 99,
                    "city", "Chicago"
            ));

            final Dynamic<?> result = finder.set(data, newRemainder);

            // "name" is excluded, so it comes from root
            assertThat(result.get("name").value()).isEqualTo("Alice");
            // "age" is NOT excluded, so it comes from newRemainder
            assertThat(result.get("age").value()).isEqualTo(99);
            // "city" comes from newRemainder
            assertThat(result.get("city").value()).isEqualTo("Chicago");
        }

        @Test
        @DisplayName("id() returns 'remainder'")
        void idReturnsRemainder() {
            assertThat(Finder.remainder("x", "y").id()).isEqualTo("remainder");
        }
    }

    @Nested
    @DisplayName("then() composition")
    class ThenComposition {

        @Test
        @DisplayName("composes finders for nested access")
        void composesForNestedAccess() {
            final Dynamic<?> data = dynamic(map(
                    "user", map(
                            "address", map(
                                    "city", "Boston"
                            )
                    )
            ));
            final Finder<?> cityFinder = Finder.field("user")
                    .then(Finder.field("address"))
                    .then(Finder.field("city"));

            final Dynamic<?> result = cityFinder.get(data);

            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo("Boston");
        }

        @Test
        @DisplayName("composed set() updates nested value")
        void composedSetUpdatesNestedValue() {
            final Dynamic<?> data = dynamic(map(
                    "user", map(
                            "name", "Alice",
                            "age", 30
                    )
            ));
            final Finder<?> nameFinder = Finder.field("user").then(Finder.field("name"));

            final Dynamic<?> result = nameFinder.set(data, dynamic("Bob"));

            assertThat(result.get("user").get("name").value()).isEqualTo("Bob");
            assertThat(result.get("user").get("age").value()).isEqualTo(30);
        }

        @Test
        @DisplayName("composed id() concatenates with dots")
        void composedIdConcatenatesWithDots() {
            final Finder<?> finder = Finder.field("user")
                    .then(Finder.field("address"))
                    .then(Finder.field("city"));

            assertThat(finder.id()).isEqualTo("field[user].field[address].field[city]");
        }

        @Test
        @DisplayName("get() returns null when intermediate path is missing")
        void getReturnsNullWhenIntermediateMissing() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("user").then(Finder.field("name"));

            final Dynamic<?> result = finder.get(data);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("update() operation")
    class UpdateOperation {

        @Test
        @DisplayName("update() transforms focused value")
        void updateTransformsFocusedValue() {
            final Dynamic<?> data = dynamic(map("count", 10));
            final Finder<?> finder = Finder.field("count");

            final Dynamic<?> result = finder.update(data, d -> {
                final int value = d.asNumber().result().map(Number::intValue).orElse(0);
                return d.createInt(value * 2);
            });

            assertThat(result.get("count").value()).isEqualTo(20);
        }

        @Test
        @DisplayName("update() returns unchanged when focus is missing")
        void updateReturnsUnchangedWhenMissing() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("missing");

            final Dynamic<?> result = finder.update(data, d -> dynamic("updated"));

            assertThat(result.value()).isEqualTo(data.value());
        }
    }

    @Nested
    @DisplayName("getOptional() operation")
    class GetOptionalOperation {

        @Test
        @DisplayName("getOptional() returns Optional with value when present")
        void getOptionalReturnsValueWhenPresent() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("name");

            final var result = finder.getOptional(data);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("getOptional() returns empty when missing")
        void getOptionalReturnsEmptyWhenMissing() {
            final Dynamic<?> data = dynamic(map("name", "Alice"));
            final Finder<?> finder = Finder.field("address");

            final var result = finder.getOptional(data);

            assertThat(result).isEmpty();
        }
    }
}
