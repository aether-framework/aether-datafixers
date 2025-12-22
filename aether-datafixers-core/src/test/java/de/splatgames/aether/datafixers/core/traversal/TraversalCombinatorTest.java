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

package de.splatgames.aether.datafixers.core.traversal;

import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for traversal combinators in {@link Rules}.
 */
@DisplayName("Traversal Combinators")
class TraversalCombinatorTest {

    @Nested
    @DisplayName("Type.children()")
    class TypeChildrenTests {

        @Test
        @DisplayName("primitive types have no children")
        void primitiveTypesHaveNoChildren() {
            assertThat(Type.STRING.children()).isEmpty();
            assertThat(Type.INT.children()).isEmpty();
            assertThat(Type.BOOL.children()).isEmpty();
            assertThat(Type.DOUBLE.children()).isEmpty();
        }

        @Test
        @DisplayName("list type has element type as child")
        void listTypeHasElementTypeAsChild() {
            Type<List<Integer>> listType = Type.list(Type.INT);

            List<Type<?>> children = listType.children();

            assertThat(children).hasSize(1);
            assertThat(children.get(0)).isEqualTo(Type.INT);
        }

        @Test
        @DisplayName("optional type has element type as child")
        void optionalTypeHasElementTypeAsChild() {
            Type<Optional<String>> optType = Type.optional(Type.STRING);

            List<Type<?>> children = optType.children();

            assertThat(children).hasSize(1);
            assertThat(children.get(0)).isEqualTo(Type.STRING);
        }

        @Test
        @DisplayName("product type has both component types as children")
        void productTypeHasBothComponentTypesAsChildren() {
            Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);

            List<Type<?>> children = pairType.children();

            assertThat(children).hasSize(2);
            assertThat(children.get(0)).isEqualTo(Type.STRING);
            assertThat(children.get(1)).isEqualTo(Type.INT);
        }

        @Test
        @DisplayName("sum type has both alternative types as children")
        void sumTypeHasBothAlternativeTypesAsChildren() {
            var sumType = Type.sum(Type.STRING, Type.INT);

            List<Type<?>> children = sumType.children();

            assertThat(children).hasSize(2);
            assertThat(children.get(0)).isEqualTo(Type.STRING);
            assertThat(children.get(1)).isEqualTo(Type.INT);
        }

        @Test
        @DisplayName("field type has inner type as child")
        void fieldTypeHasInnerTypeAsChild() {
            Type<String> fieldType = Type.field("name", Type.STRING);

            List<Type<?>> children = fieldType.children();

            assertThat(children).hasSize(1);
            assertThat(children.get(0)).isEqualTo(Type.STRING);
        }

        @Test
        @DisplayName("named type has target type as child")
        void namedTypeHasTargetTypeAsChild() {
            Type<Integer> namedType = Type.named("Age", Type.INT);

            List<Type<?>> children = namedType.children();

            assertThat(children).hasSize(1);
            assertThat(children.get(0)).isEqualTo(Type.INT);
        }

        @Test
        @DisplayName("nested types have correct children hierarchy")
        void nestedTypesHaveCorrectChildrenHierarchy() {
            // List<Pair<String, Integer>>
            Type<List<Pair<String, Integer>>> nestedType =
                    Type.list(Type.product(Type.STRING, Type.INT));

            // Top level: list has 1 child (the pair type)
            List<Type<?>> topChildren = nestedType.children();
            assertThat(topChildren).hasSize(1);

            // The pair type has 2 children
            Type<?> pairType = topChildren.get(0);
            assertThat(pairType.children()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Typed.children()")
    class TypedChildrenTests {

        @Test
        @DisplayName("primitive typed has no children")
        void primitiveTypedHasNoChildren() {
            Typed<String> typed = new Typed<>(Type.STRING, "hello");

            var result = typed.children(GsonOps.INSTANCE);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isEmpty();
        }

        @Test
        @DisplayName("list typed has elements as children")
        void listTypedHasElementsAsChildren() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            var result = typed.children(GsonOps.INSTANCE);

            assertThat(result.isSuccess()).isTrue();
            List<Typed<?>> children = result.result().orElseThrow();
            assertThat(children).hasSize(3);
            assertThat(children.get(0).value()).isEqualTo(1);
            assertThat(children.get(1).value()).isEqualTo(2);
            assertThat(children.get(2).value()).isEqualTo(3);
        }

        @Test
        @DisplayName("pair typed has first and second as children")
        void pairTypedHasFirstAndSecondAsChildren() {
            Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);
            Typed<Pair<String, Integer>> typed = new Typed<>(pairType, Pair.of("Alice", 30));

            var result = typed.children(GsonOps.INSTANCE);

            assertThat(result.isSuccess()).isTrue();
            List<Typed<?>> children = result.result().orElseThrow();
            assertThat(children).hasSize(2);
            assertThat(children.get(0).value()).isEqualTo("Alice");
            assertThat(children.get(1).value()).isEqualTo(30);
        }

        @Test
        @DisplayName("optional typed with value has one child")
        void optionalTypedWithValueHasOneChild() {
            Type<Optional<String>> optType = Type.optional(Type.STRING);
            Typed<Optional<String>> typed = new Typed<>(optType, Optional.of("present"));

            var result = typed.children(GsonOps.INSTANCE);

            assertThat(result.isSuccess()).isTrue();
            List<Typed<?>> children = result.result().orElseThrow();
            assertThat(children).hasSize(1);
            assertThat(children.get(0).value()).isEqualTo("present");
        }

        @Test
        @DisplayName("optional typed without value has no children")
        void optionalTypedWithoutValueHasNoChildren() {
            Type<Optional<String>> optType = Type.optional(Type.STRING);
            Typed<Optional<String>> typed = new Typed<>(optType, Optional.empty());

            var result = typed.children(GsonOps.INSTANCE);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Typed.withChildren()")
    class TypedWithChildrenTests {

        @Test
        @DisplayName("reconstructs list from new children")
        void reconstructsListFromNewChildren() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            // Transform children to doubled values
            List<Typed<?>> doubledChildren = List.of(
                    new Typed<>(Type.INT, 2),
                    new Typed<>(Type.INT, 4),
                    new Typed<>(Type.INT, 6)
            );

            var result = typed.withChildren(GsonOps.INSTANCE, doubledChildren);

            assertThat(result.isSuccess()).isTrue();
            Typed<List<Integer>> newTyped = result.result().orElseThrow();
            assertThat(newTyped.value()).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("reconstructs pair from new children")
        void reconstructsPairFromNewChildren() {
            Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);
            Typed<Pair<String, Integer>> typed = new Typed<>(pairType, Pair.of("Alice", 30));

            List<Typed<?>> newChildren = List.of(
                    new Typed<>(Type.STRING, "Bob"),
                    new Typed<>(Type.INT, 25)
            );

            var result = typed.withChildren(GsonOps.INSTANCE, newChildren);

            assertThat(result.isSuccess()).isTrue();
            Typed<Pair<String, Integer>> newTyped = result.result().orElseThrow();
            assertThat(newTyped.value().first()).isEqualTo("Bob");
            assertThat(newTyped.value().second()).isEqualTo(25);
        }

        @Test
        @DisplayName("reconstructs optional from new children")
        void reconstructsOptionalFromNewChildren() {
            Type<Optional<String>> optType = Type.optional(Type.STRING);
            Typed<Optional<String>> typed = new Typed<>(optType, Optional.of("old"));

            List<Typed<?>> newChildren = List.of(
                    new Typed<>(Type.STRING, "new")
            );

            var result = typed.withChildren(GsonOps.INSTANCE, newChildren);

            assertThat(result.isSuccess()).isTrue();
            Typed<Optional<String>> newTyped = result.result().orElseThrow();
            assertThat(newTyped.value()).contains("new");
        }
    }

    @Nested
    @DisplayName("Rules.all()")
    class AllCombinatorTests {

        @Test
        @DisplayName("applies rule to all list elements")
        void appliesRuleToAllListElements() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            // Rule that doubles integers
            TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            TypeRewriteRule allDouble = Rules.all(GsonOps.INSTANCE, doubleRule);

            Optional<Typed<?>> result = allDouble.rewrite(listType, typed);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            List<Integer> values = (List<Integer>) result.get().value();
            assertThat(values).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("applies rule to both pair elements")
        void appliesRuleToBothPairElements() {
            Type<Pair<Integer, Integer>> pairType = Type.product(Type.INT, Type.INT);
            Typed<Pair<Integer, Integer>> typed = new Typed<>(pairType, Pair.of(5, 10));

            TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            TypeRewriteRule allDouble = Rules.all(GsonOps.INSTANCE, doubleRule);

            Optional<Typed<?>> result = allDouble.rewrite(pairType, typed);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            Pair<Integer, Integer> pair = (Pair<Integer, Integer>) result.get().value();
            assertThat(pair.first()).isEqualTo(10);
            assertThat(pair.second()).isEqualTo(20);
        }

        @Test
        @DisplayName("returns input unchanged for primitives")
        void returnsInputUnchangedForPrimitives() {
            Typed<String> typed = new Typed<>(Type.STRING, "hello");

            TypeRewriteRule anyRule = TypeRewriteRule.forType("any", Type.INT, n -> n * 2);
            TypeRewriteRule allRule = Rules.all(GsonOps.INSTANCE, anyRule);

            Optional<Typed<?>> result = allRule.rewrite(Type.STRING, typed);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("Rules.one()")
    class OneCombinatorTests {

        @Test
        @DisplayName("applies rule to first matching child only")
        void appliesRuleToFirstMatchingChildOnly() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            // Track which elements are transformed
            List<Integer> transformed = new ArrayList<>();
            TypeRewriteRule trackingRule = new TypeRewriteRule() {
                @Override
                public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
                    if (type.reference().equals(Type.INT.reference())) {
                        Integer value = (Integer) input.value();
                        transformed.add(value);
                        return Optional.of(new Typed<>(Type.INT, value * 2));
                    }
                    return Optional.empty();
                }
            };

            TypeRewriteRule oneRule = Rules.one(GsonOps.INSTANCE, trackingRule);

            Optional<Typed<?>> result = oneRule.rewrite(listType, typed);

            assertThat(result).isPresent();
            // Only the first element should be transformed
            assertThat(transformed).containsExactly(1);
            @SuppressWarnings("unchecked")
            List<Integer> values = (List<Integer>) result.get().value();
            assertThat(values).containsExactly(2, 2, 3);
        }
    }

    @Nested
    @DisplayName("Rules.everywhere()")
    class EverywhereCombinatorTests {

        @Test
        @DisplayName("applies rule at all levels of nested structure")
        void appliesRuleAtAllLevelsOfNestedStructure() {
            // List of pairs of integers
            Type<List<Pair<Integer, Integer>>> nestedType =
                    Type.list(Type.product(Type.INT, Type.INT));
            Typed<List<Pair<Integer, Integer>>> typed = new Typed<>(nestedType, List.of(
                    Pair.of(1, 2),
                    Pair.of(3, 4)
            ));

            TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            TypeRewriteRule everywhereDouble = Rules.everywhere(GsonOps.INSTANCE, doubleRule);

            Optional<Typed<?>> result = everywhereDouble.rewrite(nestedType, typed);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            List<Pair<Integer, Integer>> values = (List<Pair<Integer, Integer>>) result.get().value();
            assertThat(values).hasSize(2);
            assertThat(values.get(0).first()).isEqualTo(2);
            assertThat(values.get(0).second()).isEqualTo(4);
            assertThat(values.get(1).first()).isEqualTo(6);
            assertThat(values.get(1).second()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Rules.bottomUp()")
    class BottomUpCombinatorTests {

        @Test
        @DisplayName("processes children before parent")
        void processesChildrenBeforeParent() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            List<String> processingOrder = new ArrayList<>();

            TypeRewriteRule trackingRule = new TypeRewriteRule() {
                @Override
                public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
                    processingOrder.add(type.describe());
                    return Optional.of(input);
                }
            };

            TypeRewriteRule bottomUpRule = Rules.bottomUp(GsonOps.INSTANCE, trackingRule);
            bottomUpRule.rewrite(listType, typed);

            // Children (int values) should be processed before parent (list)
            assertThat(processingOrder).hasSize(4);
            assertThat(processingOrder.get(0)).isEqualTo("int"); // first child
            assertThat(processingOrder.get(1)).isEqualTo("int"); // second child
            assertThat(processingOrder.get(2)).isEqualTo("int"); // third child
            assertThat(processingOrder.get(3)).contains("List"); // parent
        }
    }

    @Nested
    @DisplayName("Rules.topDown()")
    class TopDownCombinatorTests {

        @Test
        @DisplayName("processes parent before children")
        void processesParentBeforeChildren() {
            Type<List<Integer>> listType = Type.list(Type.INT);
            Typed<List<Integer>> typed = new Typed<>(listType, List.of(1, 2, 3));

            List<String> processingOrder = new ArrayList<>();

            TypeRewriteRule trackingRule = new TypeRewriteRule() {
                @Override
                public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
                    processingOrder.add(type.describe());
                    return Optional.of(input);
                }
            };

            TypeRewriteRule topDownRule = Rules.topDown(GsonOps.INSTANCE, trackingRule);
            topDownRule.rewrite(listType, typed);

            // Parent (list) should be processed before children (int values)
            assertThat(processingOrder).hasSize(4);
            assertThat(processingOrder.get(0)).contains("List"); // parent first
            assertThat(processingOrder.get(1)).isEqualTo("int"); // first child
            assertThat(processingOrder.get(2)).isEqualTo("int"); // second child
            assertThat(processingOrder.get(3)).isEqualTo("int"); // third child
        }
    }

    @Nested
    @DisplayName("Deprecated no-DynamicOps overloads")
    @SuppressWarnings("deprecation")
    class DeprecatedOverloadsTests {

        @Test
        @DisplayName("all() without ops returns input unchanged")
        void allWithoutOpsReturnsInputUnchanged() {
            Typed<String> typed = new Typed<>(Type.STRING, "hello");
            TypeRewriteRule anyRule = TypeRewriteRule.forType("any", Type.INT, n -> n * 2);

            Optional<Typed<?>> result = Rules.all(anyRule).rewrite(Type.STRING, typed);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("one() without ops returns empty")
        void oneWithoutOpsReturnsEmpty() {
            Typed<String> typed = new Typed<>(Type.STRING, "hello");
            TypeRewriteRule anyRule = TypeRewriteRule.forType("any", Type.INT, n -> n * 2);

            Optional<Typed<?>> result = Rules.one(anyRule).rewrite(Type.STRING, typed);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("everywhere() without ops applies only to self")
        void everywhereWithoutOpsAppliesToSelfOnly() {
            Typed<String> typed = new Typed<>(Type.STRING, "hello");
            TypeRewriteRule upperRule = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);

            Optional<Typed<?>> result = Rules.everywhere(upperRule).rewrite(Type.STRING, typed);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }
    }
}
