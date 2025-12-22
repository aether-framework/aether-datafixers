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
 * Unit tests for {@link Lens}.
 */
@DisplayName("Lens")
class LensTest {

    // Test data models
    record Person(String name, int age) {}
    record Address(String street, String city) {}
    record PersonWithAddress(String name, Address address) {}

    // Basic lenses
    private final Lens<Person, Person, String, String> nameLens = Lens.of(
            "person.name",
            Person::name,
            (person, newName) -> new Person(newName, person.age())
    );

    private final Lens<Person, Person, Integer, Integer> ageLens = Lens.of(
            "person.age",
            Person::age,
            (person, newAge) -> new Person(person.name(), newAge)
    );

    private final Lens<Address, Address, String, String> cityLens = Lens.of(
            "address.city",
            Address::city,
            (address, newCity) -> new Address(address.street(), newCity)
    );

    private final Lens<PersonWithAddress, PersonWithAddress, Address, Address> addressLens = Lens.of(
            "person.address",
            PersonWithAddress::address,
            (person, newAddress) -> new PersonWithAddress(person.name(), newAddress)
    );

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("of() creates lens with correct id")
        void ofCreatesLensWithCorrectId() {
            assertThat(nameLens.id()).isEqualTo("person.name");
        }

        @Test
        @DisplayName("of() creates functional lens")
        void ofCreatesFunctionalLens() {
            final Person person = new Person("Alice", 30);

            assertThat(nameLens.get(person)).isEqualTo("Alice");
            assertThat(nameLens.set(person, "Bob")).isEqualTo(new Person("Bob", 30));
        }
    }

    @Nested
    @DisplayName("get()")
    class GetOperation {

        @Test
        @DisplayName("get() extracts focused value")
        void getExtractsFocusedValue() {
            final Person person = new Person("Alice", 30);

            assertThat(nameLens.get(person)).isEqualTo("Alice");
            assertThat(ageLens.get(person)).isEqualTo(30);
        }

        @Test
        @DisplayName("get() works with nested structures")
        void getWorksWithNestedStructures() {
            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston")
            );

            assertThat(addressLens.get(person)).isEqualTo(new Address("Main St", "Boston"));
        }
    }

    @Nested
    @DisplayName("set()")
    class SetOperation {

        @Test
        @DisplayName("set() returns new instance with updated value")
        void setReturnsNewInstanceWithUpdatedValue() {
            final Person original = new Person("Alice", 30);
            final Person updated = nameLens.set(original, "Bob");

            assertThat(updated).isEqualTo(new Person("Bob", 30));
            assertThat(original).isEqualTo(new Person("Alice", 30)); // original unchanged
        }

        @Test
        @DisplayName("set() preserves other fields")
        void setPreservesOtherFields() {
            final Person person = new Person("Alice", 30);
            final Person updated = ageLens.set(person, 31);

            assertThat(updated.name()).isEqualTo("Alice");
            assertThat(updated.age()).isEqualTo(31);
        }
    }

    @Nested
    @DisplayName("modify()")
    class ModifyOperation {

        @Test
        @DisplayName("modify() transforms focused value")
        void modifyTransformsFocusedValue() {
            final Person person = new Person("alice", 30);
            final Person updated = nameLens.modify(person, String::toUpperCase);

            assertThat(updated).isEqualTo(new Person("ALICE", 30));
        }

        @Test
        @DisplayName("modify() with identity function returns equivalent structure")
        void modifyWithIdentityReturnsEquivalent() {
            final Person person = new Person("Alice", 30);
            final Person updated = nameLens.modify(person, name -> name);

            assertThat(updated).isEqualTo(person);
        }

        @Test
        @DisplayName("modify() works with numeric transformations")
        void modifyWorksWithNumericTransformations() {
            final Person person = new Person("Alice", 30);
            final Person updated = ageLens.modify(person, age -> age + 1);

            assertThat(updated).isEqualTo(new Person("Alice", 31));
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        @Test
        @DisplayName("compose() creates lens for nested access")
        void composeCreatesLensForNestedAccess() {
            final Lens<PersonWithAddress, PersonWithAddress, String, String> personCityLens =
                    addressLens.compose(cityLens);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston")
            );

            assertThat(personCityLens.get(person)).isEqualTo("Boston");
        }

        @Test
        @DisplayName("compose() set updates nested value")
        void composeSetUpdatesNestedValue() {
            final Lens<PersonWithAddress, PersonWithAddress, String, String> personCityLens =
                    addressLens.compose(cityLens);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston")
            );
            final PersonWithAddress updated = personCityLens.set(person, "Seattle");

            assertThat(updated.address().city()).isEqualTo("Seattle");
            assertThat(updated.address().street()).isEqualTo("Main St");
            assertThat(updated.name()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("compose() concatenates ids with dot")
        void composeConcatenatesIds() {
            final Lens<PersonWithAddress, PersonWithAddress, String, String> personCityLens =
                    addressLens.compose(cityLens);

            assertThat(personCityLens.id()).isEqualTo("person.address.address.city");
        }

        @Test
        @DisplayName("compose() modify works on nested value")
        void composeModifyWorksOnNestedValue() {
            final Lens<PersonWithAddress, PersonWithAddress, String, String> personCityLens =
                    addressLens.compose(cityLens);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "boston")
            );
            final PersonWithAddress updated = personCityLens.modify(person, String::toUpperCase);

            assertThat(updated.address().city()).isEqualTo("BOSTON");
        }
    }

    @Nested
    @DisplayName("Lens Laws")
    class LensLaws {

        @Test
        @DisplayName("GetPut: set(s, get(s)) == s")
        void getPutLaw() {
            final Person person = new Person("Alice", 30);

            // Setting a value to what it already is returns the original
            final Person result = nameLens.set(person, nameLens.get(person));

            assertThat(result).isEqualTo(person);
        }

        @Test
        @DisplayName("PutGet: get(set(s, a)) == a")
        void putGetLaw() {
            final Person person = new Person("Alice", 30);
            final String newName = "Bob";

            // Getting after setting returns exactly what was set
            final String result = nameLens.get(nameLens.set(person, newName));

            assertThat(result).isEqualTo(newName);
        }

        @Test
        @DisplayName("PutPut: set(set(s, a1), a2) == set(s, a2)")
        void putPutLaw() {
            final Person person = new Person("Alice", 30);
            final String name1 = "Bob";
            final String name2 = "Charlie";

            // Setting twice is equivalent to setting once with the final value
            final Person doubleSet = nameLens.set(nameLens.set(person, name1), name2);
            final Person singleSet = nameLens.set(person, name2);

            assertThat(doubleSet).isEqualTo(singleSet);
        }

        @Test
        @DisplayName("Laws hold for composed lenses")
        void lawsHoldForComposedLenses() {
            final Lens<PersonWithAddress, PersonWithAddress, String, String> personCityLens =
                    addressLens.compose(cityLens);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston")
            );

            // GetPut
            assertThat(personCityLens.set(person, personCityLens.get(person)))
                    .isEqualTo(person);

            // PutGet
            assertThat(personCityLens.get(personCityLens.set(person, "Seattle")))
                    .isEqualTo("Seattle");

            // PutPut
            assertThat(personCityLens.set(personCityLens.set(person, "Seattle"), "Portland"))
                    .isEqualTo(personCityLens.set(person, "Portland"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with empty string values")
        void worksWithEmptyStringValues() {
            final Person person = new Person("", 30);

            assertThat(nameLens.get(person)).isEmpty();
            assertThat(nameLens.set(person, "Alice")).isEqualTo(new Person("Alice", 30));
        }

        @Test
        @DisplayName("works with zero numeric values")
        void worksWithZeroNumericValues() {
            final Person person = new Person("Alice", 0);

            assertThat(ageLens.get(person)).isZero();
            assertThat(ageLens.set(person, 30)).isEqualTo(new Person("Alice", 30));
        }

        @Test
        @DisplayName("works with negative numeric values")
        void worksWithNegativeNumericValues() {
            final Person person = new Person("Alice", -5);

            assertThat(ageLens.get(person)).isEqualTo(-5);
            assertThat(ageLens.modify(person, age -> age * -1)).isEqualTo(new Person("Alice", 5));
        }
    }
}
