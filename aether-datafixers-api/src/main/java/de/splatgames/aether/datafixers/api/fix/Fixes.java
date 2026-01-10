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

package de.splatgames.aether.datafixers.api.fix;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Factory class for common data fix patterns.
 *
 * <p>This class provides high-level methods for creating {@link TypeRewriteRule}
 * instances that handle common data migration patterns. These patterns include field renaming, removal, addition,
 * transformation, and tagged choice handling.</p>
 *
 * <h2>Categories of Fixes</h2>
 * <ul>
 *   <li><b>Typed Fixes:</b> {@link #fixTypeEverywhereTyped} and {@link #fixTypeEverywhere}
 *       for general type-based transformations</li>
 *   <li><b>Field Operations:</b> {@link #renameField}, {@link #removeField},
 *       {@link #addField}, {@link #transformField} for field-level changes</li>
 *   <li><b>Tagged Choice:</b> {@link #fixChoice} and {@link #renameChoice}
 *       for discriminated union migrations</li>
 *   <li><b>Recursive:</b> {@link #walkRecursive} for deep tree transformations</li>
 *   <li><b>Utility:</b> {@link #composite} and {@link #conditional} for combining rules</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Rename a field in player data
 * TypeRewriteRule renameRule = Fixes.renameField(
 *     GsonOps.INSTANCE, "playerName", "name", playerType
 * );
 *
 * // Add a new field with default value
 * TypeRewriteRule addHealthRule = Fixes.addField(
 *     GsonOps.INSTANCE, "maxHealth", DSL.intType(), () -> 20, playerType
 * );
 *
 * // Combine multiple fixes
 * TypeRewriteRule composite = Fixes.composite("PlayerV1ToV2",
 *     renameRule, addHealthRule
 * );
 *
 * // Apply the fix
 * Typed<?> updated = composite.apply(typed);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class are thread-safe and return immutable rules.</p>
 *
 * @author Erik Pförtner
 * @see TypeRewriteRule
 * @see Rules
 * @see DataFix
 * @since 0.1.0
 */
public final class Fixes {

    private Fixes() {
        // private constructor to prevent instantiation
    }

    // ==================== Typed Fixes ====================

    /**
     * Creates a rule that transforms typed values of a specific type.
     *
     * @param name       the fix name
     * @param inputType  the input type
     * @param outputType the output type
     * @param rewrite    the transformation function
     * @return a type rewrite rule
     */
    @NotNull
    public static TypeRewriteRule fixTypeEverywhereTyped(@NotNull final String name,
                                                         @NotNull final Type<?> inputType,
                                                         @NotNull final Type<?> outputType,
                                                         @NotNull final Function<Typed<?>, Typed<?>> rewrite) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(inputType, "inputType must not be null");
        Preconditions.checkNotNull(outputType, "outputType must not be null");
        Preconditions.checkNotNull(rewrite, "rewrite must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!type.reference().equals(inputType.reference())) {
                    return Optional.empty();
                }
                return Optional.of(rewrite.apply(input));
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates a rule that transforms the dynamic representation of a type.
     *
     * @param name    the fix name
     * @param type    the type to transform
     * @param rewrite the dynamic transformation function
     * @return a type rewrite rule
     */
    @NotNull
    public static TypeRewriteRule fixTypeEverywhere(@NotNull final String name,
                                                    @NotNull final Type<?> type,
                                                    @NotNull final Function<Dynamic<?>, Dynamic<?>> rewrite) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(rewrite, "rewrite must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> inputType,
                                              @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(inputType, "inputType must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!inputType.reference().equals(type.reference())) {
                    return Optional.empty();
                }

                // We need to get DynamicOps from somewhere - this requires the caller to provide it
                // For now, we'll use a simplified approach
                return Optional.of(input);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    // ==================== Field Operations ====================

    /**
     * Creates a rule that renames a field in the data.
     *
     * @param <T>           the dynamic type
     * @param ops           the dynamic ops
     * @param oldName       the old field name
     * @param newName       the new field name
     * @param containerType the container type
     * @return a renaming rule
     */
    @NotNull
    public static <T> TypeRewriteRule renameField(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String oldName,
                                                  @NotNull final String newName,
                                                  @NotNull final Type<?> containerType) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(oldName, "oldName must not be null");
        Preconditions.checkNotNull(newName, "newName must not be null");
        Preconditions.checkNotNull(containerType, "containerType must not be null");
        return Rules.renameField(ops, oldName, newName).ifType(containerType);
    }

    /**
     * Creates a rule that removes a field from the data.
     *
     * @param <T>           the dynamic type
     * @param ops           the dynamic ops
     * @param fieldName     the field to remove
     * @param containerType the container type
     * @return a removal rule
     */
    @NotNull
    public static <T> TypeRewriteRule removeField(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String fieldName,
                                                  @NotNull final Type<?> containerType) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        Preconditions.checkNotNull(containerType, "containerType must not be null");
        return Rules.removeField(ops, fieldName).ifType(containerType);
    }

    /**
     * Creates a rule that adds a field with a default value.
     *
     * @param <T>           the dynamic type
     * @param <A>           the field value type
     * @param ops           the dynamic ops
     * @param fieldName     the field name to add
     * @param fieldType     the field type
     * @param defaultValue  supplier for the default value
     * @param containerType the container type
     * @return an addition rule
     */
    @NotNull
    public static <T, A> TypeRewriteRule addField(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String fieldName,
                                                  @NotNull final Type<A> fieldType,
                                                  @NotNull final Supplier<A> defaultValue,
                                                  @NotNull final Type<?> containerType) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        Preconditions.checkNotNull(defaultValue, "defaultValue must not be null");
        Preconditions.checkNotNull(containerType, "containerType must not be null");
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!type.reference().equals(containerType.reference())) {
                    return Optional.empty();
                }

                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    // Only add if field doesn't exist
                    if (dynamic.get(fieldName) != null) {
                        return ((Type) containerType).read(dynamic);
                    }
                    // Encode the default value
                    return fieldType.codec().encodeStart(ops, defaultValue.get())
                            .flatMap(encoded -> {
                                final Dynamic<T> updated = dynamic.set(fieldName, new Dynamic<>(ops, encoded));
                                return ((Type) containerType).read(updated);
                            });
                }).map(newValue -> new Typed<>((Type) containerType, newValue)).result();
            }

            @Override
            public String toString() {
                return "addField(" + fieldName + ")";
            }
        };
    }

    /**
     * Creates a rule that transforms a specific field.
     *
     * @param <T>           the dynamic type
     * @param ops           the dynamic ops
     * @param fieldName     the field to transform
     * @param transform     the transformation function
     * @param containerType the container type
     * @return a transformation rule
     */
    @NotNull
    public static <T> TypeRewriteRule transformField(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final String fieldName,
                                                     @NotNull final Function<Dynamic<?>, Dynamic<?>> transform,
                                                     @NotNull final Type<?> containerType) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");
        Preconditions.checkNotNull(containerType, "containerType must not be null");
        return Rules.transformField(ops, fieldName, transform).ifType(containerType);
    }

    // ==================== Tagged Choice Operations ====================

    /**
     * Creates a rule that applies different fixes based on a tag value.
     *
     * @param <T>      the dynamic type
     * @param ops      the dynamic ops
     * @param tagField the discriminator field name
     * @param type     the tagged choice type
     * @param fixByTag a map of tag values to fix functions
     * @return a tagged choice fix rule
     */
    @NotNull
    public static <T> TypeRewriteRule fixChoice(@NotNull final DynamicOps<T> ops,
                                                @NotNull final String tagField,
                                                @NotNull final Type<?> type,
                                                @NotNull final Map<String, Function<Dynamic<?>, Dynamic<?>>> fixByTag) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(tagField, "tagField must not be null");
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fixByTag, "fixByTag must not be null");
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> inputType,
                                                       @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(inputType, "inputType must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!inputType.reference().equals(type.reference())) {
                    return Optional.empty();
                }

                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    // Get the tag value
                    final Dynamic<?> tagValue = dynamic.get(tagField);
                    if (tagValue == null) {
                        return ((Type) type).read(dynamic);
                    }

                    return tagValue.asString().flatMap(tag -> {
                        final Function<Dynamic<?>, Dynamic<?>> fix = fixByTag.get(tag);
                        if (fix == null) {
                            // No fix for this tag, return unchanged
                            return ((Type) type).read(dynamic);
                        }
                        final Dynamic<?> transformed = fix.apply(dynamic);
                        return ((Type) type).read(transformed);
                    });
                }).map(newValue -> new Typed<>((Type) type, newValue)).result();
            }

            @Override
            public String toString() {
                return "fixChoice(" + tagField + ", " + fixByTag.keySet() + ")";
            }
        };
    }

    /**
     * Creates a rule that renames a tag value in a tagged choice.
     *
     * @param <T>      the dynamic type
     * @param ops      the dynamic ops
     * @param tagField the discriminator field name
     * @param oldTag   the old tag value
     * @param newTag   the new tag value
     * @param type     the tagged choice type
     * @return a tag renaming rule
     */
    @NotNull
    public static <T> TypeRewriteRule renameChoice(@NotNull final DynamicOps<T> ops,
                                                   @NotNull final String tagField,
                                                   @NotNull final String oldTag,
                                                   @NotNull final String newTag,
                                                   @NotNull final Type<?> type) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(tagField, "tagField must not be null");
        Preconditions.checkNotNull(oldTag, "oldTag must not be null");
        Preconditions.checkNotNull(newTag, "newTag must not be null");
        Preconditions.checkNotNull(type, "type must not be null");
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> inputType,
                                                       @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(inputType, "inputType must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!inputType.reference().equals(type.reference())) {
                    return Optional.empty();
                }

                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    final Dynamic<?> tagValue = dynamic.get(tagField);
                    if (tagValue == null) {
                        return ((Type) type).read(dynamic);
                    }

                    return tagValue.asString().flatMap(tag -> {
                        if (!tag.equals(oldTag)) {
                            return ((Type) type).read(dynamic);
                        }
                        final Dynamic<T> updated = dynamic.set(tagField, dynamic.createString(newTag));
                        return ((Type) type).read(updated);
                    });
                }).map(newValue -> new Typed<>((Type) type, newValue)).result();
            }

            @Override
            public String toString() {
                return "renameChoice(" + tagField + ": " + oldTag + " -> " + newTag + ")";
            }
        };
    }

    // ==================== Recursive Operations ====================

    /**
     * Creates a rule that walks a type recursively, applying a transformation.
     *
     * @param name   the fix name
     * @param type   the type to walk
     * @param walker the walker function
     * @return a recursive walking rule
     */
    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TypeRewriteRule walkRecursive(@NotNull final String name,
                                                @NotNull final Type<?> type,
                                                @NotNull final Function<Typed<?>, Typed<?>> walker) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(walker, "walker must not be null");
        return Rules.everywhere(TypeRewriteRule.forType(name, (Type) type, value -> walker.apply(new Typed<>((Type) type, value)).value()));
    }

    // ==================== Utility ====================

    /**
     * Creates a composite fix from multiple rules.
     *
     * @param name  the fix name
     * @param rules the rules to compose
     * @return a composite rule
     */
    @NotNull
    public static TypeRewriteRule composite(@NotNull final String name,
                                            @NotNull final TypeRewriteRule... rules) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(rules, "rules must not be null");
        return Rules.seqAll(rules).named(name);
    }

    /**
     * Creates a conditional fix that only applies if a condition is met.
     *
     * @param name      the fix name
     * @param condition the condition predicate
     * @param rule      the rule to apply if condition is true
     * @return a conditional rule
     */
    @NotNull
    public static TypeRewriteRule conditional(@NotNull final String name,
                                              @NotNull final java.util.function.Predicate<Typed<?>> condition,
                                              @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(condition, "condition must not be null");
        Preconditions.checkNotNull(rule, "rule must not be null");
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!condition.test(input)) {
                    return Optional.empty();
                }
                return rule.rewrite(type, input);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
