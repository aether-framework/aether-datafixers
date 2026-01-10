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

package de.splatgames.aether.datafixers.testkit.factory;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Factory methods for creating common {@link DataFix} patterns quickly.
 *
 * <p>{@code QuickFix} reduces the boilerplate needed to create simple fixes
 * for testing. Instead of implementing the full {@code DataFix} interface,
 * you can use factory methods to create fixes for common operations.</p>
 *
 * <h2>Rename Field</h2>
 * <pre>{@code
 * DataFix<JsonElement> fix = QuickFix.renameField(
 *     GsonOps.INSTANCE,
 *     "rename_player_name", 1, 2,
 *     "playerName", "name"
 * );
 * }</pre>
 *
 * <h2>Add Field</h2>
 * <pre>{@code
 * DataFix<JsonElement> fix = QuickFix.addField(
 *     GsonOps.INSTANCE,
 *     "add_score", 2, 3,
 *     "score", 0
 * );
 * }</pre>
 *
 * <h2>Remove Field</h2>
 * <pre>{@code
 * DataFix<JsonElement> fix = QuickFix.removeField(
 *     GsonOps.INSTANCE,
 *     "remove_legacy", 3, 4,
 *     "legacyField"
 * );
 * }</pre>
 *
 * <h2>Transform Field</h2>
 * <pre>{@code
 * DataFix<JsonElement> fix = QuickFix.transformField(
 *     GsonOps.INSTANCE,
 *     "uppercase_name", 1, 2,
 *     "name", d -> d.createString(d.asString().result().orElse("").toUpperCase())
 * );
 * }</pre>
 *
 * <h2>Custom Lambda Fix</h2>
 * <pre>{@code
 * DataFix<JsonElement> fix = QuickFix.simple(
 *     "custom_fix", 1, 2,
 *     input -> input.set("version", input.createInt(2))
 * );
 * }</pre>
 *
 * @author Erik Pförtner
 * @since 0.2.0
 */
public final class QuickFix {

    private QuickFix() {
        // Factory class
    }

    // ==================== Simple Lambda Fix ====================

    /**
     * Creates a simple fix from a lambda function.
     *
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param transform   the transformation function
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> simple(
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");

        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(fromVersion);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(toVersion);
            }

            @Override
            public @NotNull Dynamic<T> apply(
                    @NotNull final TypeReference type,
                    @NotNull final Dynamic<T> input,
                    @NotNull final DataFixerContext context
            ) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                Preconditions.checkNotNull(context, "context must not be null");
                return transform.apply(input);
            }
        };
    }

    /**
     * Creates a simple fix from a lambda function with DataVersion objects.
     *
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param transform   the transformation function
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> simple(
            @NotNull final String name,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");
        return simple(name, fromVersion.getVersion(), toVersion.getVersion(), transform);
    }

    // ==================== Rename Field ====================

    /**
     * Creates a fix that renames a field.
     *
     * @param ops         the DynamicOps to use
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param oldField    the old field name
     * @param newField    the new field name
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> renameField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String oldField,
            @NotNull final String newField
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(oldField, "oldField must not be null");
        Preconditions.checkNotNull(newField, "newField must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            final Dynamic<T> fieldValue = input.get(oldField);
            if (fieldValue == null) {
                return input;
            }
            return input.remove(oldField).set(newField, fieldValue);
        });
    }

    // ==================== Add Field ====================

    /**
     * Creates a fix that adds a string field with a default value.
     *
     * @param ops          the DynamicOps to use
     * @param name         the fix name
     * @param fromVersion  the source version
     * @param toVersion    the target version
     * @param field        the field name to add
     * @param defaultValue the default string value
     * @param <T>          the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> addStringField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String field,
            @NotNull final String defaultValue
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(defaultValue, "defaultValue must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            if (input.has(field)) {
                return input;
            }
            return input.set(field, input.createString(defaultValue));
        });
    }

    /**
     * Creates a fix that adds an integer field with a default value.
     *
     * @param ops          the DynamicOps to use
     * @param name         the fix name
     * @param fromVersion  the source version
     * @param toVersion    the target version
     * @param field        the field name to add
     * @param defaultValue the default integer value
     * @param <T>          the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> addIntField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String field,
            final int defaultValue
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(field, "field must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            if (input.has(field)) {
                return input;
            }
            return input.set(field, input.createInt(defaultValue));
        });
    }

    /**
     * Creates a fix that adds a boolean field with a default value.
     *
     * @param ops          the DynamicOps to use
     * @param name         the fix name
     * @param fromVersion  the source version
     * @param toVersion    the target version
     * @param field        the field name to add
     * @param defaultValue the default boolean value
     * @param <T>          the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> addBooleanField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String field,
            final boolean defaultValue
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(field, "field must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            if (input.has(field)) {
                return input;
            }
            return input.set(field, input.createBoolean(defaultValue));
        });
    }

    // ==================== Remove Field ====================

    /**
     * Creates a fix that removes a field.
     *
     * @param ops         the DynamicOps to use
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param field       the field name to remove
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> removeField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String field
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(field, "field must not be null");

        return simple(name, fromVersion, toVersion, input -> input.remove(field));
    }

    // ==================== Transform Field ====================

    /**
     * Creates a fix that transforms a field value.
     *
     * @param ops         the DynamicOps to use
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param field       the field name to transform
     * @param transform   the transformation function
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> transformField(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final String field,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform
    ) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            final Dynamic<T> fieldValue = input.get(field);
            if (fieldValue == null) {
                return input;
            }
            return input.set(field, transform.apply(fieldValue));
        });
    }

    // ==================== Identity Fix ====================

    /**
     * Creates an identity fix that does nothing (useful for testing).
     *
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param <T>         the underlying value type
     * @return a new DataFix that returns input unchanged
     */
    @NotNull
    public static <T> DataFix<T> identity(
            @NotNull final String name,
            final int fromVersion,
            final int toVersion
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        return simple(name, fromVersion, toVersion, Function.identity());
    }

    // ==================== Conditional Fix ====================

    /**
     * Creates a fix that only applies when a condition is met.
     *
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param condition   the condition to check
     * @param transform   the transformation to apply if condition is true
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @NotNull
    public static <T> DataFix<T> conditional(
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final Predicate<Dynamic<T>> condition,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(condition, "condition must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            if (condition.test(input)) {
                return transform.apply(input);
            }
            return input;
        });
    }

    // ==================== Composed Fix ====================

    /**
     * Creates a fix that applies multiple transformations in sequence.
     *
     * @param name        the fix name
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param transforms  the transformations to apply in order
     * @param <T>         the underlying value type
     * @return a new DataFix
     */
    @SafeVarargs
    @NotNull
    public static <T> DataFix<T> compose(
            @NotNull final String name,
            final int fromVersion,
            final int toVersion,
            @NotNull final Function<Dynamic<T>, Dynamic<T>>... transforms
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(transforms, "transforms must not be null");

        return simple(name, fromVersion, toVersion, input -> {
            Dynamic<T> result = input;
            for (final Function<Dynamic<T>, Dynamic<T>> transform : transforms) {
                result = transform.apply(result);
            }
            return result;
        });
    }
}
