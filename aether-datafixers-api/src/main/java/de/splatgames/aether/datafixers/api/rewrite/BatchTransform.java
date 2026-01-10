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

package de.splatgames.aether.datafixers.api.rewrite;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A builder for batching multiple field operations into a single transformation pass.
 *
 * <p>When performing multiple field operations (rename, remove, set, transform),
 * using separate rules causes each operation to perform its own encode/decode cycle. {@code BatchTransform} collects
 * all operations and applies them in a single pass, significantly improving performance for complex migrations.</p>
 *
 * <h2>Performance Comparison</h2>
 * <pre>{@code
 * // Slow: 4 encode/decode cycles
 * Rules.seq(
 *     Rules.renameField(ops, "playerName", "name"),
 *     Rules.renameField(ops, "xp", "experience"),
 *     Rules.removeField(ops, "deprecated"),
 *     Rules.addField(ops, "version", defaultVersion)
 * )
 *
 * // Fast: 1 encode/decode cycle
 * Rules.batch(ops, b -> b
 *     .rename("playerName", "name")
 *     .rename("xp", "experience")
 *     .remove("deprecated")
 *     .set("version", d -> d.createInt(2))
 * )
 * }</pre>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li>{@link #rename(String, String)} - Rename a field</li>
 *   <li>{@link #remove(String)} - Remove a field</li>
 *   <li>{@link #set(String, Function)} - Set a field value (computed)</li>
 *   <li>{@link #setStatic(String, Dynamic)} - Set a field to a static value</li>
 *   <li>{@link #transform(String, Function)} - Transform an existing field value</li>
 *   <li>{@link #addIfMissing(String, Function)} - Add field only if missing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe during construction. Once built into a
 * {@link TypeRewriteRule} via {@link Rules#batch}, the resulting rule is thread-safe.</p>
 *
 * @param <T> the underlying data format type (e.g., JsonElement)
 * @author Erik Pförtner
 * @see Rules#batch(DynamicOps, java.util.function.Consumer)
 * @since 0.2.0
 */
public final class BatchTransform<T> {

    private final List<FieldOperation<T>> operations = new ArrayList<>();
    private final DynamicOps<T> ops;

    /**
     * Creates a new batch transform builder.
     *
     * @param ops the dynamic operations for the data format, must not be {@code null}
     */
    public BatchTransform(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        this.ops = ops;
    }

    /**
     * Adds a rename operation to the batch.
     *
     * <p>If the source field doesn't exist, this operation is a no-op for that field.</p>
     *
     * @param from the current field name, must not be {@code null}
     * @param to   the new field name, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> rename(@NotNull final String from, @NotNull final String to) {
        Preconditions.checkNotNull(from, "from must not be null");
        Preconditions.checkNotNull(to, "to must not be null");
        operations.add(new RenameOp<>(from, to));
        return this;
    }

    /**
     * Adds a remove operation to the batch.
     *
     * <p>If the field doesn't exist, this operation is a no-op.</p>
     *
     * @param field the field name to remove, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if field is {@code null}
     */
    @NotNull
    public BatchTransform<T> remove(@NotNull final String field) {
        Preconditions.checkNotNull(field, "field must not be null");
        operations.add(new RemoveOp<>(field));
        return this;
    }

    /**
     * Adds a set operation to the batch with a computed value.
     *
     * <p>The value function receives the current Dynamic and should return
     * the value to set. This always overwrites any existing value.</p>
     *
     * @param field         the field name to set, must not be {@code null}
     * @param valueSupplier function that computes the value from the current dynamic, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> set(@NotNull final String field,
                                 @NotNull final Function<Dynamic<T>, Dynamic<T>> valueSupplier) {
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(valueSupplier, "valueSupplier must not be null");
        operations.add(new SetOp<>(field, valueSupplier));
        return this;
    }

    /**
     * Adds a set operation to the batch with a static value.
     *
     * <p>This is a convenience method when the value doesn't depend on the current dynamic.</p>
     *
     * @param field the field name to set, must not be {@code null}
     * @param value the value to set, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> setStatic(@NotNull final String field,
                                       @NotNull final Dynamic<T> value) {
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        operations.add(new SetOp<>(field, d -> value));
        return this;
    }

    /**
     * Adds a transform operation to the batch.
     *
     * <p>The transform function receives the current field value and should return
     * the new value. If the field doesn't exist, this operation is a no-op.</p>
     *
     * @param field     the field name to transform, must not be {@code null}
     * @param transform the transformation function, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> transform(@NotNull final String field,
                                       @NotNull final Function<Dynamic<T>, Dynamic<T>> transform) {
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(transform, "transform must not be null");
        operations.add(new TransformOp<>(field, transform));
        return this;
    }

    /**
     * Adds an operation that sets a field only if it doesn't exist.
     *
     * <p>If the field already exists, this operation is a no-op.
     * Equivalent to {@code Rules.addField}.</p>
     *
     * @param field         the field name, must not be {@code null}
     * @param valueSupplier function that computes the default value, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> addIfMissing(@NotNull final String field,
                                          @NotNull final Function<Dynamic<T>, Dynamic<T>> valueSupplier) {
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(valueSupplier, "valueSupplier must not be null");
        operations.add(new AddIfMissingOp<>(field, valueSupplier));
        return this;
    }

    /**
     * Adds an operation that sets a field only if it doesn't exist (static value).
     *
     * @param field the field name, must not be {@code null}
     * @param value the default value, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public BatchTransform<T> addIfMissingStatic(@NotNull final String field,
                                                @NotNull final Dynamic<T> value) {
        Preconditions.checkNotNull(field, "field must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        operations.add(new AddIfMissingOp<>(field, d -> value));
        return this;
    }

    /**
     * Applies all batched operations to the input dynamic in a single pass.
     *
     * <p>Operations are applied in the order they were added.</p>
     *
     * @param input the input dynamic, must not be {@code null}
     * @return the transformed dynamic, never {@code null}
     * @throws NullPointerException if input is {@code null}
     */
    @NotNull
    public Dynamic<T> apply(@NotNull final Dynamic<T> input) {
        Preconditions.checkNotNull(input, "input must not be null");

        Dynamic<T> result = input;
        for (final FieldOperation<T> op : operations) {
            result = op.apply(result);
        }
        return result;
    }

    /**
     * Returns the number of operations in this batch.
     *
     * @return the operation count
     */
    public int size() {
        return operations.size();
    }

    /**
     * Returns whether this batch has any operations.
     *
     * @return {@code true} if empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    // ==================== Internal Operation Classes ====================

    /**
     * Base interface for field operations.
     */
    private interface FieldOperation<T> {
        @NotNull
        Dynamic<T> apply(@NotNull Dynamic<T> dynamic);
    }

    /**
     * Rename operation: moves value from one field to another.
     */
    private record RenameOp<T>(String from, String to) implements FieldOperation<T> {
        @Override
        @NotNull
        public Dynamic<T> apply(@NotNull final Dynamic<T> dynamic) {
            Preconditions.checkNotNull(dynamic, "dynamic must not be null");
            final Dynamic<T> value = dynamic.get(from);
            if (value == null) {
                return dynamic;
            }
            return dynamic.remove(from).set(to, value);
        }
    }

    /**
     * Remove operation: removes a field.
     */
    private record RemoveOp<T>(String field) implements FieldOperation<T> {
        @Override
        @NotNull
        public Dynamic<T> apply(@NotNull final Dynamic<T> dynamic) {
            Preconditions.checkNotNull(dynamic, "dynamic must not be null");
            return dynamic.remove(field);
        }
    }

    /**
     * Set operation: sets a field to a computed value (overwrites existing).
     */
    private record SetOp<T>(String field, Function<Dynamic<T>, Dynamic<T>> valueSupplier) implements FieldOperation<T> {
        @Override
        @NotNull
        public Dynamic<T> apply(@NotNull final Dynamic<T> dynamic) {
            Preconditions.checkNotNull(dynamic, "dynamic must not be null");
            return dynamic.set(field, valueSupplier.apply(dynamic));
        }
    }

    /**
     * Transform operation: transforms an existing field value.
     */
    private record TransformOp<T>(String field,
                                  Function<Dynamic<T>, Dynamic<T>> transform) implements FieldOperation<T> {
        @Override
        @NotNull
        public Dynamic<T> apply(@NotNull final Dynamic<T> dynamic) {
            Preconditions.checkNotNull(dynamic, "dynamic must not be null");
            final Dynamic<T> value = dynamic.get(field);
            if (value == null) {
                return dynamic;
            }
            return dynamic.set(field, transform.apply(value));
        }
    }

    /**
     * Add if missing operation: sets a field only if it doesn't exist.
     */
    private record AddIfMissingOp<T>(String field,
                                     Function<Dynamic<T>, Dynamic<T>> valueSupplier) implements FieldOperation<T> {
        @Override
        @NotNull
        public Dynamic<T> apply(@NotNull final Dynamic<T> dynamic) {
            Preconditions.checkNotNull(dynamic, "dynamic must not be null");
            if (dynamic.get(field) != null) {
                return dynamic;
            }
            return dynamic.set(field, valueSupplier.apply(dynamic));
        }
    }
}
