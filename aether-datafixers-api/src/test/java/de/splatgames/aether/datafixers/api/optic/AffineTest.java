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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Affine}.
 */
@DisplayName("Affine")
class AffineTest {

    // Test data models with optional fields
    record Person(String firstName, String middleName, String lastName) {}
    record Address(String street, String city, String apartment) {}
    record PersonWithAddress(String name, Address address) {}

    // Affines for optional fields
    private final Affine<Person, Person, String, String> middleNameAffine = Affine.of(
            "person.middleName",
            person -> Optional.ofNullable(person.middleName()),
            (person, middle) -> new Person(person.firstName(), middle, person.lastName())
    );

    private final Affine<Address, Address, String, String> apartmentAffine = Affine.of(
            "address.apartment",
            address -> Optional.ofNullable(address.apartment()),
            (address, apt) -> new Address(address.street(), address.city(), apt)
    );

    private final Affine<PersonWithAddress, PersonWithAddress, Address, Address> addressAffine = Affine.of(
            "person.address",
            person -> Optional.ofNullable(person.address()),
            (person, address) -> new PersonWithAddress(person.name(), address)
    );

    @Nested
    @DisplayName("Factory Method of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("of() creates affine with correct id")
        void ofCreatesAffineWithCorrectId() {
            assertThat(middleNameAffine.id()).isEqualTo("person.middleName");
        }

        @Test
        @DisplayName("of() creates functional affine")
        void ofCreatesFunctionalAffine() {
            final Person withMiddle = new Person("Alice", "Marie", "Smith");

            assertThat(middleNameAffine.getOption(withMiddle)).contains("Marie");
            assertThat(middleNameAffine.set(withMiddle, "Ann"))
                    .isEqualTo(new Person("Alice", "Ann", "Smith"));
        }
    }

    @Nested
    @DisplayName("getOption()")
    class GetOptionOperation {

        @Test
        @DisplayName("getOption() returns value when present")
        void getOptionReturnsValueWhenPresent() {
            final Person person = new Person("Alice", "Marie", "Smith");

            assertThat(middleNameAffine.getOption(person)).contains("Marie");
        }

        @Test
        @DisplayName("getOption() returns empty when absent")
        void getOptionReturnsEmptyWhenAbsent() {
            final Person person = new Person("Bob", null, "Jones");

            assertThat(middleNameAffine.getOption(person)).isEmpty();
        }
    }

    @Nested
    @DisplayName("set()")
    class SetOperation {

        @Test
        @DisplayName("set() updates value when present")
        void setUpdatesValueWhenPresent() {
            final Person person = new Person("Alice", "Marie", "Smith");

            final Person updated = middleNameAffine.set(person, "Ann");

            assertThat(updated).isEqualTo(new Person("Alice", "Ann", "Smith"));
        }

        @Test
        @DisplayName("set() sets value even when originally absent")
        void setSetsValueWhenAbsent() {
            final Person person = new Person("Bob", null, "Jones");

            final Person updated = middleNameAffine.set(person, "Lee");

            assertThat(updated).isEqualTo(new Person("Bob", "Lee", "Jones"));
        }
    }

    @Nested
    @DisplayName("modify()")
    class ModifyOperation {

        @Test
        @DisplayName("modify() transforms value when present")
        void modifyTransformsValueWhenPresent() {
            final Person person = new Person("Alice", "marie", "Smith");

            final Person updated = middleNameAffine.modify(person, String::toUpperCase);

            assertThat(updated).isEqualTo(new Person("Alice", "MARIE", "Smith"));
        }

        @Test
        @DisplayName("modify() returns unchanged when absent")
        void modifyReturnsUnchangedWhenAbsent() {
            final Person person = new Person("Bob", null, "Jones");

            final Person updated = middleNameAffine.modify(person, String::toUpperCase);

            assertThat(updated).isSameAs(person);
        }

        @Test
        @DisplayName("modify() with identity returns equivalent when present")
        void modifyWithIdentityReturnsEquivalent() {
            final Person person = new Person("Alice", "Marie", "Smith");

            final Person updated = middleNameAffine.modify(person, s -> s);

            assertThat(updated).isEqualTo(person);
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        @Test
        @DisplayName("compose() creates affine for nested optional access")
        void composeCreatesAffineForNestedAccess() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston", "4A")
            );

            assertThat(personApartmentAffine.getOption(person)).contains("4A");
        }

        @Test
        @DisplayName("compose() returns empty when outer is absent")
        void composeReturnsEmptyWhenOuterAbsent() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress("Alice", null);

            assertThat(personApartmentAffine.getOption(person)).isEmpty();
        }

        @Test
        @DisplayName("compose() returns empty when inner is absent")
        void composeReturnsEmptyWhenInnerAbsent() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston", null)
            );

            assertThat(personApartmentAffine.getOption(person)).isEmpty();
        }

        @Test
        @DisplayName("compose() concatenates ids")
        void composeConcatenatesIds() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            assertThat(personApartmentAffine.id()).isEqualTo("person.address.address.apartment");
        }

        @Test
        @DisplayName("compose() set updates nested value")
        void composeSetUpdatesNestedValue() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston", "4A")
            );

            final PersonWithAddress updated = personApartmentAffine.set(person, "5B");

            assertThat(updated.address().apartment()).isEqualTo("5B");
            assertThat(updated.address().street()).isEqualTo("Main St");
            assertThat(updated.name()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("compose() set returns unchanged when outer absent")
        void composeSetReturnsUnchangedWhenOuterAbsent() {
            final Affine<PersonWithAddress, PersonWithAddress, String, String> personApartmentAffine =
                    addressAffine.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress("Alice", null);

            final PersonWithAddress updated = personApartmentAffine.set(person, "5B");

            assertThat(updated).isSameAs(person);
        }
    }

    @Nested
    @DisplayName("fromLens()")
    class FromLensOperation {

        private final Lens<Person, Person, String, String> firstNameLens = Lens.of(
                "person.firstName",
                Person::firstName,
                (person, name) -> new Person(name, person.middleName(), person.lastName())
        );

        @Test
        @DisplayName("fromLens() creates always-matching affine")
        void fromLensCreatesAlwaysMatchingAffine() {
            final Affine<Person, Person, String, String> firstNameAffine =
                    Affine.fromLens(firstNameLens);

            final Person person = new Person("Alice", null, "Smith");

            assertThat(firstNameAffine.getOption(person)).contains("Alice");
        }

        @Test
        @DisplayName("fromLens() preserves lens id")
        void fromLensPreservesLensId() {
            final Affine<Person, Person, String, String> firstNameAffine =
                    Affine.fromLens(firstNameLens);

            assertThat(firstNameAffine.id()).isEqualTo("person.firstName");
        }

        @Test
        @DisplayName("fromLens() set works like lens set")
        void fromLensSetWorksLikeLensSet() {
            final Affine<Person, Person, String, String> firstNameAffine =
                    Affine.fromLens(firstNameLens);

            final Person person = new Person("Alice", null, "Smith");
            final Person updated = firstNameAffine.set(person, "Bob");

            assertThat(updated).isEqualTo(new Person("Bob", null, "Smith"));
        }

        @Test
        @DisplayName("fromLens() affine can be composed with regular affine")
        void fromLensAffineCanBeComposed() {
            // Always-present address
            final Lens<PersonWithAddress, PersonWithAddress, Address, Address> addressLens = Lens.of(
                    "person.address",
                    PersonWithAddress::address,
                    (person, address) -> new PersonWithAddress(person.name(), address)
            );

            final Affine<PersonWithAddress, PersonWithAddress, Address, Address> addressFromLens =
                    Affine.fromLens(addressLens);
            final Affine<PersonWithAddress, PersonWithAddress, String, String> composed =
                    addressFromLens.compose(apartmentAffine);

            final PersonWithAddress person = new PersonWithAddress(
                    "Alice",
                    new Address("Main St", "Boston", "4A")
            );

            assertThat(composed.getOption(person)).contains("4A");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with empty string values")
        void worksWithEmptyStringValues() {
            final Person person = new Person("Alice", "", "Smith");

            assertThat(middleNameAffine.getOption(person)).contains("");
        }

        @Test
        @DisplayName("set empty string value")
        void setEmptyStringValue() {
            final Person person = new Person("Alice", "Marie", "Smith");

            final Person updated = middleNameAffine.set(person, "");

            assertThat(updated.middleName()).isEmpty();
        }

        @Test
        @DisplayName("chained modifies work correctly")
        void chainedModifiesWorkCorrectly() {
            final Person person = new Person("Alice", "marie", "Smith");

            final Person updated = middleNameAffine.modify(
                    middleNameAffine.modify(person, String::toUpperCase),
                    s -> s + "!"
            );

            assertThat(updated.middleName()).isEqualTo("MARIE!");
        }
    }
}
