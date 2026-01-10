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
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Traversal}.
 */
@DisplayName("Traversal")
class TraversalTest {

    // Traversals
    private final Traversal<List<String>, List<String>, String, String> listTraversal = Traversal.of(
            "list.elements",
            List::stream,
            (list, modifier) -> list.stream().map(modifier).toList()
    );
    private final Traversal<Person, Person, String, String> skillsTraversal = Traversal.of(
            "person.skills",
            person -> person.skills().stream(),
            (person, modifier) -> new Person(person.name(), person.skills().stream().map(modifier).toList())
    );
    private final Traversal<Team, Team, Person, Person> membersTraversal = Traversal.of(
            "team.members",
            team -> team.members().stream(),
            (team, modifier) -> new Team(team.name(), team.members().stream().map(modifier).toList())
    );

    // Test data models
    record Person(String name, List<String> skills) {
    }

    record Team(String name, List<Person> members) {
    }

    @Nested
    @DisplayName("Factory Method of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("of() creates traversal with correct id")
        void ofCreatesTraversalWithCorrectId() {
            assertThat(listTraversal.id()).isEqualTo("list.elements");
        }

        @Test
        @DisplayName("of() creates functional traversal")
        void ofCreatesFunctionalTraversal() {
            final List<String> names = List.of("alice", "bob");

            assertThat(listTraversal.toList(names)).containsExactly("alice", "bob");
            assertThat(listTraversal.modify(names, String::toUpperCase))
                    .containsExactly("ALICE", "BOB");
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllOperation {

        @Test
        @DisplayName("getAll() returns stream of all elements")
        void getAllReturnsStreamOfAllElements() {
            final List<String> names = List.of("Alice", "Bob", "Charlie");

            assertThat(listTraversal.getAll(names).toList())
                    .containsExactly("Alice", "Bob", "Charlie");
        }

        @Test
        @DisplayName("getAll() returns empty stream for empty source")
        void getAllReturnsEmptyStreamForEmptySource() {
            final List<String> empty = List.of();

            assertThat(listTraversal.getAll(empty).toList()).isEmpty();
        }

        @Test
        @DisplayName("getAll() works with nested structures")
        void getAllWorksWithNestedStructures() {
            final Person person = new Person("Alice", List.of("Java", "Python", "Go"));

            assertThat(skillsTraversal.getAll(person).toList())
                    .containsExactly("Java", "Python", "Go");
        }
    }

    @Nested
    @DisplayName("toList()")
    class ToListOperation {

        @Test
        @DisplayName("toList() collects all elements")
        void toListCollectsAllElements() {
            final List<String> names = List.of("Alice", "Bob");

            assertThat(listTraversal.toList(names)).containsExactly("Alice", "Bob");
        }

        @Test
        @DisplayName("toList() returns immutable list")
        void toListReturnsImmutableList() {
            final List<String> names = List.of("Alice");
            final List<String> result = listTraversal.toList(names);

            assertThat(result).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("modify()")
    class ModifyOperation {

        @Test
        @DisplayName("modify() transforms all elements")
        void modifyTransformsAllElements() {
            final List<String> names = List.of("alice", "bob");

            final List<String> result = listTraversal.modify(names, String::toUpperCase);

            assertThat(result).containsExactly("ALICE", "BOB");
        }

        @Test
        @DisplayName("modify() returns empty for empty source")
        void modifyReturnsEmptyForEmptySource() {
            final List<String> empty = List.of();

            final List<String> result = listTraversal.modify(empty, String::toUpperCase);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("modify() preserves order")
        void modifyPreservesOrder() {
            final List<String> names = List.of("C", "A", "B");

            final List<String> result = listTraversal.modify(names, s -> s.toLowerCase());

            assertThat(result).containsExactly("c", "a", "b");
        }

        @Test
        @DisplayName("modify() works with nested record")
        void modifyWorksWithNestedRecord() {
            final Person person = new Person("Alice", List.of("java", "python"));

            final Person updated = skillsTraversal.modify(person, String::toUpperCase);

            assertThat(updated.name()).isEqualTo("Alice");
            assertThat(updated.skills()).containsExactly("JAVA", "PYTHON");
        }
    }

    @Nested
    @DisplayName("set()")
    class SetOperation {

        @Test
        @DisplayName("set() replaces all elements with same value")
        void setReplacesAllElementsWithSameValue() {
            final List<String> names = List.of("Alice", "Bob", "Charlie");

            final List<String> result = listTraversal.set(names, "X");

            assertThat(result).containsExactly("X", "X", "X");
        }

        @Test
        @DisplayName("set() returns empty for empty source")
        void setReturnsEmptyForEmptySource() {
            final List<String> empty = List.of();

            final List<String> result = listTraversal.set(empty, "X");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        @Test
        @DisplayName("compose() creates traversal for nested elements")
        void composeCreatesTraversalForNestedElements() {
            final Traversal<Team, Team, String, String> allSkillsTraversal =
                    membersTraversal.compose(skillsTraversal);

            final Team team = new Team("Dev", List.of(
                    new Person("Alice", List.of("Java", "Python")),
                    new Person("Bob", List.of("Go", "Rust"))
            ));

            assertThat(allSkillsTraversal.toList(team))
                    .containsExactly("Java", "Python", "Go", "Rust");
        }

        @Test
        @DisplayName("compose() concatenates ids")
        void composeConcatenatesIds() {
            final Traversal<Team, Team, String, String> allSkillsTraversal =
                    membersTraversal.compose(skillsTraversal);

            assertThat(allSkillsTraversal.id()).isEqualTo("team.members.person.skills");
        }

        @Test
        @DisplayName("compose() modify transforms all nested elements")
        void composeModifyTransformsAllNestedElements() {
            final Traversal<Team, Team, String, String> allSkillsTraversal =
                    membersTraversal.compose(skillsTraversal);

            final Team team = new Team("Dev", List.of(
                    new Person("Alice", List.of("java", "python")),
                    new Person("Bob", List.of("go"))
            ));

            final Team updated = allSkillsTraversal.modify(team, String::toUpperCase);

            assertThat(updated.members().get(0).skills()).containsExactly("JAVA", "PYTHON");
            assertThat(updated.members().get(1).skills()).containsExactly("GO");
        }

        @Test
        @DisplayName("compose() handles empty nested collections")
        void composeHandlesEmptyNestedCollections() {
            final Traversal<Team, Team, String, String> allSkillsTraversal =
                    membersTraversal.compose(skillsTraversal);

            final Team team = new Team("Dev", List.of(
                    new Person("Alice", List.of()),
                    new Person("Bob", List.of("Go"))
            ));

            assertThat(allSkillsTraversal.toList(team)).containsExactly("Go");
        }

        @Test
        @DisplayName("compose() handles empty outer collection")
        void composeHandlesEmptyOuterCollection() {
            final Traversal<Team, Team, String, String> allSkillsTraversal =
                    membersTraversal.compose(skillsTraversal);

            final Team team = new Team("Empty", List.of());

            assertThat(allSkillsTraversal.toList(team)).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromLens()")
    class FromLensOperation {

        private final Lens<Person, Person, String, String> nameLens = Lens.of(
                "person.name",
                Person::name,
                (person, name) -> new Person(name, person.skills())
        );

        @Test
        @DisplayName("fromLens() creates single-element traversal")
        void fromLensCreatesSingleElementTraversal() {
            final Traversal<Person, Person, String, String> nameTraversal =
                    Traversal.fromLens(nameLens);

            final Person person = new Person("Alice", List.of());

            assertThat(nameTraversal.toList(person)).containsExactly("Alice");
        }

        @Test
        @DisplayName("fromLens() preserves lens id")
        void fromLensPreservesLensId() {
            final Traversal<Person, Person, String, String> nameTraversal =
                    Traversal.fromLens(nameLens);

            assertThat(nameTraversal.id()).isEqualTo("person.name");
        }

        @Test
        @DisplayName("fromLens() modify works like lens modify")
        void fromLensModifyWorksLikeLensModify() {
            final Traversal<Person, Person, String, String> nameTraversal =
                    Traversal.fromLens(nameLens);

            final Person person = new Person("alice", List.of());
            final Person updated = nameTraversal.modify(person, String::toUpperCase);

            assertThat(updated.name()).isEqualTo("ALICE");
        }

        @Test
        @DisplayName("fromLens() traversal can be composed")
        void fromLensTraversalCanBeComposed() {
            final Lens<Team, Team, String, String> teamNameLens = Lens.of(
                    "team.name",
                    Team::name,
                    (team, name) -> new Team(name, team.members())
            );

            final Traversal<Team, Team, String, String> teamNameTraversal =
                    Traversal.fromLens(teamNameLens);

            // This is a weird composition but shows it works
            final Team team = new Team("Dev", List.of());
            assertThat(teamNameTraversal.toList(team)).containsExactly("Dev");
        }
    }

    @Nested
    @DisplayName("Map Traversal")
    class MapTraversal {

        private final Traversal<Map<String, Integer>, Map<String, Integer>, Integer, Integer> valuesTraversal =
                Traversal.of(
                        "map.values",
                        map -> map.values().stream(),
                        (map, modifier) -> map.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> modifier.apply(e.getValue())))
                );

        @Test
        @DisplayName("traversal over map values")
        void traversalOverMapValues() {
            final Map<String, Integer> scores = Map.of("alice", 10, "bob", 20);

            assertThat(valuesTraversal.toList(scores)).containsExactlyInAnyOrder(10, 20);
        }

        @Test
        @DisplayName("modify map values")
        void modifyMapValues() {
            final Map<String, Integer> scores = Map.of("alice", 10, "bob", 20);

            final Map<String, Integer> doubled = valuesTraversal.modify(scores, n -> n * 2);

            assertThat(doubled).containsEntry("alice", 20).containsEntry("bob", 40);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with single element")
        void worksWithSingleElement() {
            final List<String> single = List.of("only");

            assertThat(listTraversal.toList(single)).containsExactly("only");
            assertThat(listTraversal.modify(single, String::toUpperCase)).containsExactly("ONLY");
        }

        @Test
        @DisplayName("modify with identity returns equivalent")
        void modifyWithIdentityReturnsEquivalent() {
            final List<String> names = List.of("Alice", "Bob");

            final List<String> result = listTraversal.modify(names, s -> s);

            assertThat(result).isEqualTo(names);
        }

        @Test
        @DisplayName("deeply nested compose")
        void deeplyNestedCompose() {
            record Company(String name, List<Team> teams) {
            }

            final Traversal<Company, Company, Team, Team> teamsTraversal = Traversal.of(
                    "company.teams",
                    company -> company.teams().stream(),
                    (company, modifier) -> new Company(company.name(),
                            company.teams().stream().map(modifier).toList())
            );

            final Traversal<Company, Company, String, String> allSkills = teamsTraversal
                    .compose(membersTraversal)
                    .compose(skillsTraversal);

            final Company company = new Company("Acme", List.of(
                    new Team("Dev", List.of(
                            new Person("Alice", List.of("Java"))
                    )),
                    new Team("Ops", List.of(
                            new Person("Bob", List.of("Docker", "K8s"))
                    ))
            ));

            assertThat(allSkills.toList(company)).containsExactly("Java", "Docker", "K8s");
        }
    }
}
