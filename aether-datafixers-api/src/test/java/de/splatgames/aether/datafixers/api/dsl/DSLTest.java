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

package de.splatgames.aether.datafixers.api.dsl;

import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.template.TypeFamily;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DSL}.
 */
@DisplayName("DSL")
class DSLTest {

    private final TypeFamily emptyFamily = TypeFamily.empty();

    @Nested
    @DisplayName("Primitive Types")
    class PrimitiveTypes {

        @Test
        @DisplayName("bool() creates boolean type template")
        void boolCreatesBooleanTypeTemplate() {
            final TypeTemplate template = DSL.bool();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("bool");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.BOOL);
        }

        @Test
        @DisplayName("intType() creates integer type template")
        void intTypeCreatesIntegerTypeTemplate() {
            final TypeTemplate template = DSL.intType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("int");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.INT);
        }

        @Test
        @DisplayName("longType() creates long type template")
        void longTypeCreatesLongTypeTemplate() {
            final TypeTemplate template = DSL.longType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("long");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.LONG);
        }

        @Test
        @DisplayName("floatType() creates float type template")
        void floatTypeCreatesFloatTypeTemplate() {
            final TypeTemplate template = DSL.floatType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("float");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.FLOAT);
        }

        @Test
        @DisplayName("doubleType() creates double type template")
        void doubleTypeCreatesDoubleTypeTemplate() {
            final TypeTemplate template = DSL.doubleType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("double");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.DOUBLE);
        }

        @Test
        @DisplayName("string() creates string type template")
        void stringCreatesStringTypeTemplate() {
            final TypeTemplate template = DSL.string();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("string");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.STRING);
        }

        @Test
        @DisplayName("byteType() creates byte type template")
        void byteTypeCreatesByteTypeTemplate() {
            final TypeTemplate template = DSL.byteType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("byte");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.BYTE);
        }

        @Test
        @DisplayName("shortType() creates short type template")
        void shortTypeCreatesShortTypeTemplate() {
            final TypeTemplate template = DSL.shortType();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("short");

            final Type<?> type = template.apply(emptyFamily);
            assertThat(type).isEqualTo(Type.SHORT);
        }

        @Test
        @DisplayName("primitives are constant templates")
        void primitivesAreConstantTemplates() {
            final TypeFamily anotherFamily = TypeFamily.of(Type.STRING);

            assertThat(DSL.intType().apply(emptyFamily))
                    .isEqualTo(DSL.intType().apply(anotherFamily));
        }
    }

    @Nested
    @DisplayName("List Type")
    class ListType {

        @Test
        @DisplayName("list() creates list type template")
        void listCreatesListTypeTemplate() {
            final TypeTemplate template = DSL.list(DSL.intType());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("List<int>");
        }

        @Test
        @DisplayName("list() applies to produce list type")
        void listAppliesToProduceListType() {
            final TypeTemplate template = DSL.list(DSL.string());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
            assertThat(type.describe()).contains("List");
        }

        @Test
        @DisplayName("nested lists work correctly")
        void nestedListsWorkCorrectly() {
            final TypeTemplate template = DSL.list(DSL.list(DSL.intType()));

            assertThat(template.describe()).isEqualTo("List<List<int>>");
        }
    }

    @Nested
    @DisplayName("Optional Type")
    class OptionalType {

        @Test
        @DisplayName("optional() creates optional type template")
        void optionalCreatesOptionalTypeTemplate() {
            final TypeTemplate template = DSL.optional(DSL.string());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("Optional<string>");
        }

        @Test
        @DisplayName("optional() applies to produce optional type")
        void optionalAppliesToProduceOptionalType() {
            final TypeTemplate template = DSL.optional(DSL.intType());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
            assertThat(type.describe()).contains("Optional");
        }

        @Test
        @DisplayName("optional list works correctly")
        void optionalListWorksCorrectly() {
            final TypeTemplate template = DSL.optional(DSL.list(DSL.string()));

            assertThat(template.describe()).isEqualTo("Optional<List<string>>");
        }
    }

    @Nested
    @DisplayName("Product Type (AND)")
    class ProductType {

        @Test
        @DisplayName("and() with two elements creates product type")
        void andWithTwoElementsCreatesProductType() {
            final TypeTemplate template = DSL.and(DSL.string(), DSL.intType());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("(string × int)");
        }

        @Test
        @DisplayName("and() with varargs creates nested product")
        void andWithVarargsCreatesNestedProduct() {
            final TypeTemplate template = DSL.and(
                    DSL.string(),
                    DSL.intType(),
                    DSL.bool()
            );

            assertThat(template).isNotNull();
            // Right-associative: (string × (int × bool))
            assertThat(template.describe()).isEqualTo("(string × (int × bool))");
        }

        @Test
        @DisplayName("and() applies to produce product type")
        void andAppliesToProduceProductType() {
            final TypeTemplate template = DSL.and(DSL.string(), DSL.intType());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("and() with fewer than 2 elements throws exception")
        void andWithFewerThan2ElementsThrowsException() {
            assertThatThrownBy(() -> DSL.and(DSL.string()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }

        @Test
        @DisplayName("and() with 4 elements creates correctly nested structure")
        void andWith4ElementsCreatesCorrectlyNestedStructure() {
            final TypeTemplate template = DSL.and(
                    DSL.string(),
                    DSL.intType(),
                    DSL.bool(),
                    DSL.doubleType()
            );

            // Right-associative: (a × (b × (c × d)))
            assertThat(template.describe())
                    .isEqualTo("(string × (int × (bool × double)))");
        }
    }

    @Nested
    @DisplayName("Sum Type (OR)")
    class SumType {

        @Test
        @DisplayName("or() with two elements creates sum type")
        void orWithTwoElementsCreatesSumType() {
            final TypeTemplate template = DSL.or(DSL.string(), DSL.intType());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("(string + int)");
        }

        @Test
        @DisplayName("or() with varargs creates nested sum")
        void orWithVarargsCreatesNestedSum() {
            final TypeTemplate template = DSL.or(
                    DSL.string(),
                    DSL.intType(),
                    DSL.bool()
            );

            assertThat(template).isNotNull();
            // Right-associative: (string + (int + bool))
            assertThat(template.describe()).isEqualTo("(string + (int + bool))");
        }

        @Test
        @DisplayName("or() applies to produce sum type")
        void orAppliesToProduceSumType() {
            final TypeTemplate template = DSL.or(DSL.string(), DSL.intType());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("or() with fewer than 2 elements throws exception")
        void orWithFewerThan2ElementsThrowsException() {
            assertThatThrownBy(() -> DSL.or(DSL.string()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }
    }

    @Nested
    @DisplayName("Field Type")
    class FieldType {

        @Test
        @DisplayName("field() creates required field template")
        void fieldCreatesRequiredFieldTemplate() {
            final TypeTemplate template = DSL.field("name", DSL.string());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("name: string");
        }

        @Test
        @DisplayName("field() applies to produce field type")
        void fieldAppliesToProduceFieldType() {
            final TypeTemplate template = DSL.field("age", DSL.intType());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("nested field structures work correctly")
        void nestedFieldStructuresWorkCorrectly() {
            final TypeTemplate template = DSL.field("address", DSL.and(
                    DSL.field("street", DSL.string()),
                    DSL.field("city", DSL.string())
            ));

            assertThat(template.describe()).contains("address:");
        }
    }

    @Nested
    @DisplayName("Optional Field Type")
    class OptionalFieldType {

        @Test
        @DisplayName("optionalField() creates optional field template")
        void optionalFieldCreatesOptionalFieldTemplate() {
            final TypeTemplate template = DSL.optionalField("nickname", DSL.string());

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("?nickname: string");
        }

        @Test
        @DisplayName("optionalField() applies to produce optional field type")
        void optionalFieldAppliesToProduceOptionalFieldType() {
            final TypeTemplate template = DSL.optionalField("bio", DSL.string());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("mixed required and optional fields work correctly")
        void mixedRequiredAndOptionalFieldsWorkCorrectly() {
            final TypeTemplate template = DSL.and(
                    DSL.field("id", DSL.intType()),
                    DSL.optionalField("name", DSL.string())
            );

            assertThat(template.describe()).contains("id: int");
            assertThat(template.describe()).contains("?name: string");
        }
    }

    @Nested
    @DisplayName("Named Type")
    class NamedType {

        @Test
        @DisplayName("named() creates named type template")
        void namedCreatesNamedTypeTemplate() {
            final TypeTemplate template = DSL.named("Position", DSL.and(
                    DSL.field("x", DSL.doubleType()),
                    DSL.field("y", DSL.doubleType())
            ));

            assertThat(template).isNotNull();
            assertThat(template.describe()).startsWith("Position=");
        }

        @Test
        @DisplayName("named() applies to produce named type")
        void namedAppliesToProduceNamedType() {
            final TypeTemplate template = DSL.named("Point", DSL.intType());
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }
    }

    @Nested
    @DisplayName("Remainder Type")
    class RemainderType {

        @Test
        @DisplayName("remainder() creates remainder type template")
        void remainderCreatesRemainderTypeTemplate() {
            final TypeTemplate template = DSL.remainder();

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("...");
        }

        @Test
        @DisplayName("remainder() applies to produce passthrough type")
        void remainderAppliesToProducePassthroughType() {
            final TypeTemplate template = DSL.remainder();
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isEqualTo(Type.PASSTHROUGH);
        }

        @Test
        @DisplayName("remainder in schema structure works correctly")
        void remainderInSchemaStructureWorksCorrectly() {
            final TypeTemplate template = DSL.and(
                    DSL.field("id", DSL.intType()),
                    DSL.field("name", DSL.string()),
                    DSL.remainder()
            );

            assertThat(template.describe()).contains("...");
        }
    }

    @Nested
    @DisplayName("Tagged Choice Type")
    class TaggedChoiceType {

        @Test
        @DisplayName("taggedChoice() creates tagged choice template")
        void taggedChoiceCreatesTaggedChoiceTemplate() {
            final TypeTemplate template = DSL.taggedChoice("type", Map.of(
                    "player", DSL.field("name", DSL.string()),
                    "npc", DSL.field("id", DSL.intType())
            ));

            assertThat(template).isNotNull();
            assertThat(template.describe()).contains("TaggedChoice<type>");
        }

        @Test
        @DisplayName("taggedChoice() applies to produce tagged choice type")
        void taggedChoiceAppliesToProduceTaggedChoiceType() {
            final TypeTemplate template = DSL.taggedChoice("kind", Map.of(
                    "text", DSL.string(),
                    "number", DSL.intType()
            ));
            final Type<?> type = template.apply(emptyFamily);

            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("taggedChoice() description shows all choices")
        void taggedChoiceDescriptionShowsAllChoices() {
            final TypeTemplate template = DSL.taggedChoice("variant", Map.of(
                    "a", DSL.intType(),
                    "b", DSL.string()
            ));

            final String desc = template.describe();
            assertThat(desc).contains("a ->");
            assertThat(desc).contains("b ->");
        }

        @Test
        @DisplayName("taggedChoiceTyped() works like taggedChoice()")
        void taggedChoiceTypedWorksLikeTaggedChoice() {
            final TypeTemplate template = DSL.taggedChoiceTyped(
                    "type",
                    DSL.string(),
                    Map.of("option", DSL.intType())
            );

            assertThat(template).isNotNull();
            assertThat(template.describe()).contains("TaggedChoice<type>");
        }
    }

    @Nested
    @DisplayName("Type Parameter Reference (id)")
    class TypeParameterReference {

        @Test
        @DisplayName("id() creates type parameter reference")
        void idCreatesTypeParameterReference() {
            final TypeTemplate template = DSL.id(0);

            assertThat(template).isNotNull();
            assertThat(template.describe()).isEqualTo("µ0");
        }

        @Test
        @DisplayName("id() applies with family to get referenced type")
        void idAppliesWithFamilyToGetReferencedType() {
            final TypeTemplate template = DSL.id(0);
            final TypeFamily family = TypeFamily.of(Type.STRING);

            final Type<?> type = template.apply(family);
            assertThat(type).isEqualTo(Type.STRING);
        }

        @Test
        @DisplayName("id() with different indices")
        void idWithDifferentIndices() {
            assertThat(DSL.id(0).describe()).isEqualTo("µ0");
            assertThat(DSL.id(1).describe()).isEqualTo("µ1");
            assertThat(DSL.id(2).describe()).isEqualTo("µ2");
        }

        @Test
        @DisplayName("id() with out of bounds index throws on apply")
        void idWithOutOfBoundsIndexThrowsOnApply() {
            final TypeTemplate template = DSL.id(5);
            final TypeFamily family = TypeFamily.of(Type.STRING);

            assertThatThrownBy(() -> template.apply(family))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("Recursive Type")
    class RecursiveType {

        @Test
        @DisplayName("recursive() creates recursive type template")
        void recursiveCreatesRecursiveTypeTemplate() {
            final TypeTemplate template = DSL.recursive("LinkedList", self ->
                    DSL.optional(DSL.and(
                            DSL.field("value", DSL.intType()),
                            DSL.field("next", self)
                    ))
            );

            assertThat(template).isNotNull();
            assertThat(template.describe()).contains("LinkedList");
        }

        @Test
        @DisplayName("recursive() template can be created")
        void recursiveTemplateCanBeCreated() {
            final TypeTemplate template = DSL.recursive("Tree", self ->
                    DSL.and(
                            DSL.field("value", DSL.intType()),
                            DSL.optionalField("left", self),
                            DSL.optionalField("right", self)
                    )
            );

            assertThat(template).isNotNull();
            assertThat(template.describe()).contains("Tree");
        }

        @Test
        @DisplayName("recursive() with simple self-reference works")
        void recursiveWithSimpleSelfReferenceWorks() {
            final TypeTemplate template = DSL.recursive("SelfRef", self -> self);

            assertThat(template).isNotNull();
        }
    }

    @Nested
    @DisplayName("Finders")
    class Finders {

        @Test
        @DisplayName("fieldFinder() creates field finder")
        void fieldFinderCreatesFieldFinder() {
            final Finder<Object> finder = DSL.fieldFinder("name");

            assertThat(finder).isNotNull();
            assertThat(finder.id()).isEqualTo("field[name]");
        }

        @Test
        @DisplayName("indexFinder() creates index finder")
        void indexFinderCreatesIndexFinder() {
            final Finder<Object> finder = DSL.indexFinder(0);

            assertThat(finder).isNotNull();
            assertThat(finder.id()).isEqualTo("index[0]");
        }

        @Test
        @DisplayName("indexFinder() with different indices")
        void indexFinderWithDifferentIndices() {
            assertThat(DSL.indexFinder(0).id()).isEqualTo("index[0]");
            assertThat(DSL.indexFinder(5).id()).isEqualTo("index[5]");
            assertThat(DSL.indexFinder(100).id()).isEqualTo("index[100]");
        }

        @Test
        @DisplayName("remainderFinder() creates remainder finder")
        void remainderFinderCreatesRemainderFinder() {
            final Finder<Object> finder = DSL.remainderFinder("id", "name");

            assertThat(finder).isNotNull();
            assertThat(finder.id()).contains("remainder");
        }

        @Test
        @DisplayName("remainderFinder() with no exclusions")
        void remainderFinderWithNoExclusions() {
            final Finder<Object> finder = DSL.remainderFinder();

            assertThat(finder).isNotNull();
        }
    }

    @Nested
    @DisplayName("Complex Schema Examples")
    class ComplexSchemaExamples {

        @Test
        @DisplayName("player schema example")
        void playerSchemaExample() {
            final TypeTemplate playerSchema = DSL.and(
                    DSL.field("id", DSL.intType()),
                    DSL.field("name", DSL.string()),
                    DSL.field("health", DSL.intType()),
                    DSL.field("position", DSL.and(
                            DSL.field("x", DSL.doubleType()),
                            DSL.field("y", DSL.doubleType()),
                            DSL.field("z", DSL.doubleType())
                    )),
                    DSL.optionalField("inventory", DSL.list(DSL.string())),
                    DSL.remainder()
            );

            assertThat(playerSchema).isNotNull();

            final Type<?> type = playerSchema.apply(emptyFamily);
            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("entity system with tagged choice")
        void entitySystemWithTaggedChoice() {
            final TypeTemplate entitySchema = DSL.taggedChoice("type", Map.of(
                    "player", DSL.and(
                            DSL.field("name", DSL.string()),
                            DSL.field("level", DSL.intType())
                    ),
                    "monster", DSL.and(
                            DSL.field("species", DSL.string()),
                            DSL.field("hostile", DSL.bool())
                    ),
                    "item", DSL.and(
                            DSL.field("itemId", DSL.string()),
                            DSL.field("count", DSL.intType())
                    )
            ));

            final Type<?> type = entitySchema.apply(emptyFamily);
            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("nested configuration schema")
        void nestedConfigurationSchema() {
            final TypeTemplate configSchema = DSL.and(
                    DSL.field("version", DSL.intType()),
                    DSL.field("settings", DSL.and(
                            DSL.field("graphics", DSL.and(
                                    DSL.field("resolution", DSL.string()),
                                    DSL.field("fullscreen", DSL.bool())
                            )),
                            DSL.field("audio", DSL.and(
                                    DSL.field("volume", DSL.floatType()),
                                    DSL.field("muted", DSL.bool())
                            ))
                    )),
                    DSL.optionalField("customData", DSL.remainder())
            );

            assertThat(configSchema).isNotNull();
            final Type<?> type = configSchema.apply(emptyFamily);
            assertThat(type).isNotNull();
        }

        @Test
        @DisplayName("tree structure with recursive types")
        void treeStructureWithRecursiveTypes() {
            final TypeTemplate commentThread = DSL.recursive("Comment", self ->
                    DSL.and(
                            DSL.field("author", DSL.string()),
                            DSL.field("text", DSL.string()),
                            DSL.field("timestamp", DSL.longType()),
                            DSL.field("replies", DSL.list(self))
                    )
            );

            assertThat(commentThread).isNotNull();
            assertThat(commentThread.describe()).contains("Comment");
        }
    }

    @Nested
    @DisplayName("Template Immutability")
    class TemplateImmutability {

        @Test
        @DisplayName("templates are reusable")
        void templatesAreReusable() {
            final TypeTemplate nameField = DSL.field("name", DSL.string());

            final TypeTemplate schema1 = DSL.and(nameField, DSL.field("age", DSL.intType()));
            final TypeTemplate schema2 = DSL.and(nameField, DSL.field("level", DSL.intType()));

            assertThat(schema1.apply(emptyFamily)).isNotNull();
            assertThat(schema2.apply(emptyFamily)).isNotNull();
        }

        @Test
        @DisplayName("same template produces same type")
        void sameTemplateProducesSameType() {
            final TypeTemplate template = DSL.intType();

            final Type<?> type1 = template.apply(emptyFamily);
            final Type<?> type2 = template.apply(emptyFamily);

            assertThat(type1).isEqualTo(type2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("empty tagged choice map")
        void emptyTaggedChoiceMap() {
            final TypeTemplate template = DSL.taggedChoice("type", Map.of());

            assertThat(template).isNotNull();
        }

        @Test
        @DisplayName("deeply nested structures")
        void deeplyNestedStructures() {
            TypeTemplate nested = DSL.intType();
            for (int i = 0; i < 10; i++) {
                nested = DSL.list(nested);
            }

            final TypeTemplate finalNested = nested;
            assertThat(finalNested.describe()).contains("List<List<List<");
        }

        @Test
        @DisplayName("field with empty name")
        void fieldWithEmptyName() {
            final TypeTemplate template = DSL.field("", DSL.string());

            assertThat(template.describe()).isEqualTo(": string");
        }

        @Test
        @DisplayName("named with special characters in name")
        void namedWithSpecialCharactersInName() {
            final TypeTemplate template = DSL.named("Player.Position", DSL.intType());

            assertThat(template.describe()).startsWith("Player.Position=");
        }
    }
}
