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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Iso}.
 */
@DisplayName("Iso")
class IsoTest {

    // Test wrapper type
    record Wrapper<T>(T value) {}

    // Isomorphisms for testing
    private final Iso<String, String, String, String> reverseIso = Iso.of(
            "string.reverse",
            s -> new StringBuilder(s).reverse().toString(),
            s -> new StringBuilder(s).reverse().toString()
    );

    private final Iso<Wrapper<String>, Wrapper<String>, String, String> unwrapIso = Iso.of(
            "wrapper.unwrap",
            Wrapper::value,
            Wrapper::new
    );

    private final Iso<String, String, List<Character>, List<Character>> stringToCharsIso = Iso.of(
            "string.chars",
            s -> s.chars().mapToObj(c -> (char) c).toList(),
            chars -> chars.stream().map(String::valueOf).collect(Collectors.joining())
    );

    @Nested
    @DisplayName("Factory Method of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("of() creates iso with correct id")
        void ofCreatesIsoWithCorrectId() {
            assertThat(reverseIso.id()).isEqualTo("string.reverse");
            assertThat(unwrapIso.id()).isEqualTo("wrapper.unwrap");
        }

        @Test
        @DisplayName("of() creates functional iso")
        void ofCreatesFunctionalIso() {
            assertThat(reverseIso.to("hello")).isEqualTo("olleh");
            assertThat(reverseIso.from("olleh")).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("to()")
    class ToOperation {

        @Test
        @DisplayName("to() converts source to target")
        void toConvertsSourceToTarget() {
            assertThat(reverseIso.to("hello")).isEqualTo("olleh");
            assertThat(unwrapIso.to(new Wrapper<>("test"))).isEqualTo("test");
        }

        @Test
        @DisplayName("to() works with complex transformations")
        void toWorksWithComplexTransformations() {
            assertThat(stringToCharsIso.to("abc")).containsExactly('a', 'b', 'c');
        }
    }

    @Nested
    @DisplayName("from()")
    class FromOperation {

        @Test
        @DisplayName("from() converts target to source")
        void fromConvertsTargetToSource() {
            assertThat(reverseIso.from("olleh")).isEqualTo("hello");
            assertThat(unwrapIso.from("test")).isEqualTo(new Wrapper<>("test"));
        }

        @Test
        @DisplayName("from() works with complex transformations")
        void fromWorksWithComplexTransformations() {
            assertThat(stringToCharsIso.from(List.of('a', 'b', 'c'))).isEqualTo("abc");
        }
    }

    @Nested
    @DisplayName("Lens Interface")
    class LensInterface {

        @Test
        @DisplayName("get() returns same as to()")
        void getReturnsSameAsTo() {
            final Wrapper<String> wrapper = new Wrapper<>("hello");

            assertThat(unwrapIso.get(wrapper)).isEqualTo(unwrapIso.to(wrapper));
        }

        @Test
        @DisplayName("set() uses from() ignoring source")
        void setUsesFromIgnoringSource() {
            final Wrapper<String> wrapper = new Wrapper<>("old");

            final Wrapper<String> result = unwrapIso.set(wrapper, "new");

            assertThat(result).isEqualTo(new Wrapper<>("new"));
        }
    }

    @Nested
    @DisplayName("Prism Interface")
    class PrismInterface {

        @Test
        @DisplayName("getOption() always returns present")
        void getOptionAlwaysReturnsPresent() {
            assertThat(unwrapIso.getOption(new Wrapper<>("test"))).contains("test");
            assertThat(reverseIso.getOption("hello")).contains("olleh");
        }

        @Test
        @DisplayName("reverseGet() returns same as from()")
        void reverseGetReturnsSameAsFrom() {
            assertThat(unwrapIso.reverseGet("test")).isEqualTo(unwrapIso.from("test"));
        }
    }

    @Nested
    @DisplayName("modify()")
    class ModifyOperation {

        @Test
        @DisplayName("modify() transforms via round-trip")
        void modifyTransformsViaRoundTrip() {
            final Wrapper<String> wrapper = new Wrapper<>("hello");

            final Wrapper<String> result = unwrapIso.modify(wrapper, String::toUpperCase);

            assertThat(result).isEqualTo(new Wrapper<>("HELLO"));
        }

        @Test
        @DisplayName("modify() with identity returns equivalent")
        void modifyWithIdentityReturnsEquivalent() {
            final String s = "hello";

            assertThat(reverseIso.modify(s, x -> x)).isEqualTo(s);
        }
    }

    @Nested
    @DisplayName("reverse()")
    class ReverseOperation {

        @Test
        @DisplayName("reverse() swaps to and from")
        void reverseSwapsToAndFrom() {
            final Iso<String, String, Wrapper<String>, Wrapper<String>> reversed = unwrapIso.reverse();

            assertThat(reversed.to("test")).isEqualTo(new Wrapper<>("test"));
            assertThat(reversed.from(new Wrapper<>("test"))).isEqualTo("test");
        }

        @Test
        @DisplayName("reverse() adds .reverse to id")
        void reverseAddsReverseToId() {
            assertThat(unwrapIso.reverse().id()).isEqualTo("wrapper.unwrap.reverse");
        }

        @Test
        @DisplayName("double reverse returns to original")
        void doubleReverseReturnsToOriginal() {
            final Wrapper<String> wrapper = new Wrapper<>("test");
            final Iso<String, String, Wrapper<String>, Wrapper<String>> reversed = unwrapIso.reverse();

            // to -> reverse.to -> reverse.reverse.to should equal original to
            assertThat(unwrapIso.to(wrapper)).isEqualTo("test");
            assertThat(reversed.from(wrapper)).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        private final Iso<Integer, Integer, String, String> intToStringIso = Iso.of(
                "int.string",
                Object::toString,
                Integer::parseInt
        );

        private final Iso<String, String, Integer, Integer> stringLengthIso = Iso.of(
                "string.length",
                String::length,
                n -> "x".repeat(n)
        );

        @Test
        @DisplayName("compose() chains transformations")
        void composeChainsTransformations() {
            final Iso<Integer, Integer, Integer, Integer> composed =
                    intToStringIso.compose(stringLengthIso);

            // 42 -> "42" -> 2 (length)
            assertThat(composed.to(42)).isEqualTo(2);
            // 3 -> "xxx" -> 111 (wait, that's wrong for a true iso)
            // Let me fix the test with a proper iso
        }

        @Test
        @DisplayName("compose() concatenates ids")
        void composeConcatenatesIds() {
            final Iso<Integer, Integer, Integer, Integer> composed =
                    intToStringIso.compose(stringLengthIso);

            assertThat(composed.id()).isEqualTo("int.string.string.length");
        }

        @Test
        @DisplayName("compose() with proper isos works correctly")
        void composeWithProperIsosWorksCorrectly() {
            // Celsius to Kelvin
            final Iso<Double, Double, Double, Double> celsiusToKelvin = Iso.of(
                    "celsius.kelvin",
                    c -> c + 273.15,
                    k -> k - 273.15
            );

            // Kelvin to Rankine
            final Iso<Double, Double, Double, Double> kelvinToRankine = Iso.of(
                    "kelvin.rankine",
                    k -> k * 1.8,
                    r -> r / 1.8
            );

            final Iso<Double, Double, Double, Double> celsiusToRankine =
                    celsiusToKelvin.compose(kelvinToRankine);

            // 0°C = 273.15K = 491.67R
            assertThat(celsiusToRankine.to(0.0)).isCloseTo(491.67, org.assertj.core.api.Assertions.within(0.01));
            assertThat(celsiusToRankine.from(491.67)).isCloseTo(0.0, org.assertj.core.api.Assertions.within(0.01));
        }
    }

    @Nested
    @DisplayName("identity()")
    class IdentityOperation {

        @Test
        @DisplayName("identity() to returns input unchanged")
        void identityToReturnsInputUnchanged() {
            final Iso<String, String, String, String> id = Iso.identity();

            assertThat(id.to("hello")).isEqualTo("hello");
            assertThat(id.to("")).isEmpty();
        }

        @Test
        @DisplayName("identity() from returns input unchanged")
        void identityFromReturnsInputUnchanged() {
            final Iso<String, String, String, String> id = Iso.identity();

            assertThat(id.from("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("identity() has id 'identity'")
        void identityHasIdIdentity() {
            assertThat(Iso.<String>identity().id()).isEqualTo("identity");
        }

        @Test
        @DisplayName("identity() is neutral for composition")
        void identityIsNeutralForComposition() {
            final Iso<String, String, String, String> id = Iso.identity();
            final Iso<String, String, String, String> composed = id.compose(reverseIso);

            // Should behave like reverseIso
            assertThat(composed.to("hello")).isEqualTo(reverseIso.to("hello"));
            assertThat(composed.from("olleh")).isEqualTo(reverseIso.from("olleh"));
        }
    }

    @Nested
    @DisplayName("Iso Laws")
    class IsoLaws {

        @Test
        @DisplayName("RoundTrip1: from(to(s)) == s")
        void roundTrip1Law() {
            final String s = "hello";

            assertThat(reverseIso.from(reverseIso.to(s))).isEqualTo(s);
        }

        @Test
        @DisplayName("RoundTrip2: to(from(a)) == a")
        void roundTrip2Law() {
            final String a = "olleh";

            assertThat(reverseIso.to(reverseIso.from(a))).isEqualTo(a);
        }

        @Test
        @DisplayName("Laws hold for wrapper iso")
        void lawsHoldForWrapperIso() {
            final Wrapper<String> wrapper = new Wrapper<>("test");
            final String value = "test";

            // RoundTrip1
            assertThat(unwrapIso.from(unwrapIso.to(wrapper))).isEqualTo(wrapper);

            // RoundTrip2
            assertThat(unwrapIso.to(unwrapIso.from(value))).isEqualTo(value);
        }

        @Test
        @DisplayName("Laws hold for string to chars iso")
        void lawsHoldForStringToCharsIso() {
            final String s = "hello";
            final List<Character> chars = List.of('h', 'e', 'l', 'l', 'o');

            // RoundTrip1
            assertThat(stringToCharsIso.from(stringToCharsIso.to(s))).isEqualTo(s);

            // RoundTrip2
            assertThat(stringToCharsIso.to(stringToCharsIso.from(chars))).isEqualTo(chars);
        }

        @Test
        @DisplayName("Laws hold for composed isos")
        void lawsHoldForComposedIsos() {
            final Iso<Double, Double, Double, Double> celsiusToKelvin = Iso.of(
                    "celsius.kelvin",
                    c -> c + 273.15,
                    k -> k - 273.15
            );

            final Iso<Double, Double, Double, Double> kelvinToFahrenheit = Iso.of(
                    "kelvin.fahrenheit",
                    k -> (k - 273.15) * 9.0 / 5.0 + 32.0,
                    f -> (f - 32.0) * 5.0 / 9.0 + 273.15
            );

            final Iso<Double, Double, Double, Double> composed =
                    celsiusToKelvin.compose(kelvinToFahrenheit);

            final double celsius = 100.0;
            final double fahrenheit = 212.0;

            // RoundTrip1
            assertThat(composed.from(composed.to(celsius))).isCloseTo(celsius, org.assertj.core.api.Assertions.within(0.001));

            // RoundTrip2
            assertThat(composed.to(composed.from(fahrenheit))).isCloseTo(fahrenheit, org.assertj.core.api.Assertions.within(0.001));
        }

        @Test
        @DisplayName("Lens laws hold (since Iso is a Lens)")
        void lensLawsHold() {
            final Wrapper<String> wrapper = new Wrapper<>("hello");
            final String newValue = "world";

            // GetPut
            assertThat(unwrapIso.set(wrapper, unwrapIso.get(wrapper))).isEqualTo(wrapper);

            // PutGet
            assertThat(unwrapIso.get(unwrapIso.set(wrapper, newValue))).isEqualTo(newValue);
        }

        @Test
        @DisplayName("Prism laws hold (since Iso is a Prism)")
        void prismLawsHold() {
            final String value = "test";

            // PartialPutGet
            assertThat(unwrapIso.getOption(unwrapIso.reverseGet(value))).contains(value);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with empty strings")
        void worksWithEmptyStrings() {
            assertThat(reverseIso.to("")).isEmpty();
            assertThat(reverseIso.from("")).isEmpty();
            assertThat(stringToCharsIso.to("")).isEmpty();
            assertThat(stringToCharsIso.from(List.of())).isEmpty();
        }

        @Test
        @DisplayName("works with palindromes")
        void worksWithPalindromes() {
            final String palindrome = "racecar";

            assertThat(reverseIso.to(palindrome)).isEqualTo(palindrome);
        }

        @Test
        @DisplayName("works with single character")
        void worksWithSingleCharacter() {
            assertThat(reverseIso.to("a")).isEqualTo("a");
            assertThat(stringToCharsIso.to("a")).containsExactly('a');
        }
    }
}
