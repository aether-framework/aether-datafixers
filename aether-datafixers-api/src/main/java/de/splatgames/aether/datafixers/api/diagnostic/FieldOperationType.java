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

package de.splatgames.aether.datafixers.api.diagnostic;

import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import org.jetbrains.annotations.NotNull;

/**
 * Classifies the kind of field-level operation performed by a
 * {@link TypeRewriteRule} during a migration.
 *
 * <p>Each constant corresponds to one or more factory methods in
 * {@link Rules}:</p>
 *
 * <table>
 *   <caption>Mapping of field operation types to Rules methods</caption>
 *   <tr><th>Type</th><th>Rules method(s)</th></tr>
 *   <tr><td>{@link #RENAME}</td><td>{@code renameField}, {@code renameFields}, {@code renameFieldAt}</td></tr>
 *   <tr><td>{@link #REMOVE}</td><td>{@code removeField}, {@code removeFields}, {@code removeFieldAt}</td></tr>
 *   <tr><td>{@link #ADD}</td><td>{@code addField}, {@code addFieldAt}</td></tr>
 *   <tr><td>{@link #TRANSFORM}</td><td>{@code transformField}, {@code transformFieldAt}</td></tr>
 *   <tr><td>{@link #SET}</td><td>{@code setField}</td></tr>
 *   <tr><td>{@link #MOVE}</td><td>{@code moveField}</td></tr>
 *   <tr><td>{@link #COPY}</td><td>{@code copyField}</td></tr>
 *   <tr><td>{@link #GROUP}</td><td>{@code groupFields}</td></tr>
 *   <tr><td>{@link #FLATTEN}</td><td>{@code flattenField}</td></tr>
 *   <tr><td>{@link #CONDITIONAL}</td><td>{@code ifFieldExists}, {@code ifFieldMissing}, {@code ifFieldEquals}</td></tr>
 * </table>
 *
 * @author Erik Pförtner
 * @see FieldOperation
 * @see Rules
 * @since 1.0.0
 */
public enum FieldOperationType {

    /**
     * A field is renamed to a new name.
     *
     * @see Rules#renameField
     */
    RENAME("rename", true),

    /**
     * A field is removed from the data structure.
     *
     * @see Rules#removeField
     */
    REMOVE("remove", false),

    /**
     * A new field is added with a default value (only if not already present).
     *
     * @see Rules#addField
     */
    ADD("add", false),

    /**
     * A field's value is transformed by a function.
     *
     * @see Rules#transformField
     */
    TRANSFORM("transform", false),

    /**
     * A field is set to a value, overwriting any existing value.
     *
     * @see Rules#setField
     */
    SET("set", false),

    /**
     * A field is moved from one location to another.
     *
     * @see Rules#moveField
     */
    MOVE("move", true),

    /**
     * A field's value is copied to another location (original preserved).
     *
     * @see Rules#copyField
     */
    COPY("copy", true),

    /**
     * Multiple fields are grouped into a nested object.
     *
     * @see Rules#groupFields
     */
    GROUP("group", true),

    /**
     * A nested object's fields are flattened into the parent.
     *
     * @see Rules#flattenField
     */
    FLATTEN("flatten", false),

    /**
     * A conditional operation that executes a rule based on field presence, absence, or value.
     *
     * @see Rules#ifFieldExists
     * @see Rules#ifFieldMissing
     * @see Rules#ifFieldEquals
     */
    CONDITIONAL("conditional", false);

    /**
     * Human-readable lowercase display name for this operation type.
     */
    private final String displayName;

    /**
     * Whether this operation type requires a target field name (e.g., rename target, move/copy destination).
     */
    private final boolean requiresTarget;

    /**
     * Creates a new field operation type constant.
     *
     * @param displayName    the human-readable lowercase display name
     * @param requiresTarget whether this operation requires a target field name
     */
    FieldOperationType(@NotNull final String displayName, final boolean requiresTarget) {
        this.displayName = displayName;
        this.requiresTarget = requiresTarget;
    }

    /**
     * Returns the human-readable lowercase display name for this operation type.
     *
     * <p>For example, {@link #RENAME} returns {@code "rename"} and
     * {@link #CONDITIONAL} returns {@code "conditional"}.</p>
     *
     * @return the display name, never {@code null}
     */
    @NotNull
    public String displayName() {
        return this.displayName;
    }

    /**
     * Returns whether this operation type requires a target field name.
     *
     * <p>Operations that produce a target include:</p>
     * <ul>
     *   <li>{@link #RENAME} — the new field name</li>
     *   <li>{@link #MOVE} — the destination path</li>
     *   <li>{@link #COPY} — the destination path</li>
     *   <li>{@link #GROUP} — the name of the new nested object</li>
     * </ul>
     *
     * @return {@code true} if this operation type has a target field
     */
    public boolean requiresTarget() {
        return this.requiresTarget;
    }

    /**
     * Returns whether this operation type modifies the data structure layout rather than
     * just individual field values.
     *
     * <p>Structural operations change the shape of the data (nesting, flattening, moving fields
     * between levels), while non-structural operations modify field values in place or add/remove
     * individual fields at the same level.</p>
     *
     * <p>Structural types: {@link #MOVE}, {@link #COPY}, {@link #GROUP}, {@link #FLATTEN}.</p>
     *
     * @return {@code true} if this is a structural operation
     */
    public boolean isStructural() {
        return this == MOVE || this == COPY || this == GROUP || this == FLATTEN;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name, never {@code null}
     */
    @Override
    @NotNull
    public String toString() {
        return this.displayName;
    }
}
