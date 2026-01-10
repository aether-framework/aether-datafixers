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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Getter}.
 */
@DisplayName("Getter")
class GetterTest {

    // Basic getters
    private final Getter<Point, Integer> xGetter = Getter.of("point.x", Point::x);
    private final Getter<Point, Integer> yGetter = Getter.of("point.y", Point::y);
    private final Getter<Line, Point> startGetter = Getter.of("line.start", Line::start);
    private final Getter<Person, String> nameGetter = Getter.of("person.name", Person::name);

    // Test data models
    record Point(int x, int y) {
    }

    record Line(Point start, Point end) {
    }

    record Person(String name, int age) {
    }

    @Nested
    @DisplayName("Factory Method of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("of() creates getter with correct id")
        void ofCreatesGetterWithCorrectId() {
            assertThat(xGetter.id()).isEqualTo("point.x");
            assertThat(nameGetter.id()).isEqualTo("person.name");
        }

        @Test
        @DisplayName("of() creates functional getter")
        void ofCreatesFunctionalGetter() {
            final Point point = new Point(3, 4);

            assertThat(xGetter.get(point)).isEqualTo(3);
            assertThat(yGetter.get(point)).isEqualTo(4);
        }

        @Test
        @DisplayName("of() works with computed values")
        void ofWorksWithComputedValues() {
            final Getter<Point, Double> distanceGetter = Getter.of(
                    "point.distance",
                    p -> Math.sqrt(p.x() * p.x() + p.y() * p.y())
            );

            final Point point = new Point(3, 4);
            assertThat(distanceGetter.get(point)).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("get()")
    class GetOperation {

        @Test
        @DisplayName("get() extracts value from source")
        void getExtractsValueFromSource() {
            final Person person = new Person("Alice", 30);

            assertThat(nameGetter.get(person)).isEqualTo("Alice");
        }

        @Test
        @DisplayName("get() works with different instances")
        void getWorksWithDifferentInstances() {
            final Point p1 = new Point(1, 2);
            final Point p2 = new Point(10, 20);

            assertThat(xGetter.get(p1)).isEqualTo(1);
            assertThat(xGetter.get(p2)).isEqualTo(10);
        }

        @Test
        @DisplayName("get() works with nested structures")
        void getWorksWithNestedStructures() {
            final Line line = new Line(new Point(1, 2), new Point(5, 6));

            assertThat(startGetter.get(line)).isEqualTo(new Point(1, 2));
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        @Test
        @DisplayName("compose() creates getter for nested access")
        void composeCreatesGetterForNestedAccess() {
            final Getter<Line, Integer> startXGetter = startGetter.compose(xGetter);

            final Line line = new Line(new Point(1, 2), new Point(5, 6));
            assertThat(startXGetter.get(line)).isEqualTo(1);
        }

        @Test
        @DisplayName("compose() concatenates ids with dot")
        void composeConcatenatesIds() {
            final Getter<Line, Integer> startXGetter = startGetter.compose(xGetter);

            assertThat(startXGetter.id()).isEqualTo("line.start.point.x");
        }

        @Test
        @DisplayName("compose() is associative")
        void composeIsAssociative() {
            record Container(Line line) {
            }
            final Getter<Container, Line> lineGetter = Getter.of("container.line", Container::line);

            final Getter<Container, Integer> composed1 =
                    lineGetter.compose(startGetter).compose(xGetter);
            final Getter<Container, Integer> composed2 =
                    lineGetter.compose(startGetter.compose(xGetter));

            final Container container = new Container(new Line(new Point(42, 0), new Point(0, 0)));

            assertThat(composed1.get(container)).isEqualTo(composed2.get(container));
        }

        @Test
        @DisplayName("compose() works with multiple levels")
        void composeWorksWithMultipleLevels() {
            record Container(Line line) {
            }
            final Getter<Container, Line> lineGetter = Getter.of("container.line", Container::line);

            final Getter<Container, Integer> deepGetter = lineGetter
                    .compose(startGetter)
                    .compose(xGetter);

            final Container container = new Container(
                    new Line(new Point(99, 0), new Point(0, 0))
            );

            assertThat(deepGetter.get(container)).isEqualTo(99);
            assertThat(deepGetter.id()).isEqualTo("container.line.line.start.point.x");
        }
    }

    @Nested
    @DisplayName("fromLens()")
    class FromLensOperation {

        @Test
        @DisplayName("fromLens() creates getter from lens")
        void fromLensCreatesGetterFromLens() {
            final Lens<Person, Person, String, String> nameLens = Lens.of(
                    "person.name",
                    Person::name,
                    (person, name) -> new Person(name, person.age())
            );

            final Getter<Person, String> getter = Getter.fromLens(nameLens);
            final Person person = new Person("Alice", 30);

            assertThat(getter.get(person)).isEqualTo("Alice");
        }

        @Test
        @DisplayName("fromLens() preserves lens id")
        void fromLensPreservesLensId() {
            final Lens<Person, Person, String, String> nameLens = Lens.of(
                    "person.name",
                    Person::name,
                    (person, name) -> new Person(name, person.age())
            );

            final Getter<Person, String> getter = Getter.fromLens(nameLens);

            assertThat(getter.id()).isEqualTo("person.name");
        }

        @Test
        @DisplayName("fromLens() getter can be composed")
        void fromLensGetterCanBeComposed() {
            final Lens<Point, Point, Integer, Integer> xLens = Lens.of(
                    "point.x",
                    Point::x,
                    (point, x) -> new Point(x, point.y())
            );

            final Getter<Line, Point> startGetter = Getter.of("line.start", Line::start);
            final Getter<Point, Integer> xGetterFromLens = Getter.fromLens(xLens);
            final Getter<Line, Integer> composedGetter = startGetter.compose(xGetterFromLens);

            final Line line = new Line(new Point(42, 0), new Point(0, 0));

            assertThat(composedGetter.get(line)).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with empty string values")
        void worksWithEmptyStringValues() {
            final Person person = new Person("", 30);

            assertThat(nameGetter.get(person)).isEmpty();
        }

        @Test
        @DisplayName("works with zero numeric values")
        void worksWithZeroNumericValues() {
            final Point point = new Point(0, 0);

            assertThat(xGetter.get(point)).isZero();
            assertThat(yGetter.get(point)).isZero();
        }

        @Test
        @DisplayName("works with negative numeric values")
        void worksWithNegativeNumericValues() {
            final Point point = new Point(-10, -20);

            assertThat(xGetter.get(point)).isEqualTo(-10);
            assertThat(yGetter.get(point)).isEqualTo(-20);
        }

        @Test
        @DisplayName("same getter can be reused on multiple sources")
        void sameGetterCanBeReusedOnMultipleSources() {
            final Person alice = new Person("Alice", 30);
            final Person bob = new Person("Bob", 25);

            assertThat(nameGetter.get(alice)).isEqualTo("Alice");
            assertThat(nameGetter.get(bob)).isEqualTo("Bob");
        }
    }
}
