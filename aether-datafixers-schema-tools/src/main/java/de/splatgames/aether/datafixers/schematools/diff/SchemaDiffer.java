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

package de.splatgames.aether.datafixers.schematools.diff;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import de.splatgames.aether.datafixers.schematools.introspection.TypeIntrospector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fluent API for comparing two schemas.
 *
 * <p>This class provides the entry point for creating schema diffs, allowing
 * configuration of comparison options before executing the diff.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Basic type-level diff
 * SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2).diff();
 *
 * // Full diff including field-level changes
 * SchemaDiff fullDiff = SchemaDiffer.compare(schemaV1, schemaV2)
 *     .includeFieldLevel(true)
 *     .diff();
 *
 * // Ignore certain types
 * SchemaDiff filtered = SchemaDiffer.compare(schemaV1, schemaV2)
 *     .includeFieldLevel(true)
 *     .ignoreTypes(TypeReferences.INTERNAL, TypeReferences.DEBUG)
 *     .diff();
 * }</pre>
 *
 * <h2>Diff Levels</h2>
 * <ul>
 *   <li><b>Type-level:</b> Always included. Shows which types were added,
 *       removed, or exist in both schemas.</li>
 *   <li><b>Field-level:</b> Optional (off by default). Shows field changes
 *       within types that exist in both schemas.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe. Create a new instance for each diff operation.</p>
 *
 * @author Erik Pförtner
 * @see SchemaDiff
 * @since 0.3.0
 */
public final class SchemaDiffer {

    /**
     * The source (older) schema to compare from.
     */
    private final Schema source;

    /**
     * The target (newer) schema to compare to.
     */
    private final Schema target;

    /**
     * Flag indicating whether to include field-level diffs for common types.
     * Defaults to {@code false} for performance reasons.
     */
    private boolean includeFieldLevel = false;

    /**
     * Set of type references to exclude from the comparison.
     * Defaults to empty set (include all types).
     */
    private Set<TypeReference> ignoredTypes = Set.of();

    /**
     * Creates a new SchemaDiffer for the specified schemas.
     *
     * <p>This constructor is private; use {@link #compare(Schema, Schema)} to create instances.</p>
     *
     * @param source the source schema, must not be {@code null}
     * @param target the target schema, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    private SchemaDiffer(@NotNull final Schema source, @NotNull final Schema target) {
        this.source = Preconditions.checkNotNull(source, "source must not be null");
        this.target = Preconditions.checkNotNull(target, "target must not be null");
    }

    /**
     * Creates a new differ for comparing two schemas.
     *
     * @param source the source (older) schema, must not be {@code null}
     * @param target the target (newer) schema, must not be {@code null}
     * @return a new SchemaDiffer instance, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static SchemaDiffer compare(@NotNull final Schema source, @NotNull final Schema target) {
        Preconditions.checkNotNull(source, "source must not be null");
        Preconditions.checkNotNull(target, "target must not be null");
        return new SchemaDiffer(source, target);
    }

    /**
     * Enables or disables field-level diffing.
     *
     * <p>When enabled, the diff will include detailed field-level changes
     * for types that exist in both schemas. This requires type introspection
     * and may be slower for large schemas.</p>
     *
     * @param include {@code true} to include field-level diffs, {@code false} to skip
     * @return this differ for chaining
     */
    @NotNull
    public SchemaDiffer includeFieldLevel(final boolean include) {
        this.includeFieldLevel = include;
        return this;
    }

    /**
     * Specifies types to ignore during comparison.
     *
     * <p>Ignored types will not appear in added, removed, or common type sets,
     * and will not have field-level diffs generated.</p>
     *
     * @param types the type references to ignore, must not be {@code null}
     * @return this differ for chaining
     * @throws NullPointerException if {@code types} is {@code null}
     */
    @NotNull
    public SchemaDiffer ignoreTypes(@NotNull final TypeReference... types) {
        Preconditions.checkNotNull(types, "types must not be null");
        this.ignoredTypes = Set.of(types);
        return this;
    }

    /**
     * Specifies types to ignore during comparison.
     *
     * @param types the set of type references to ignore, must not be {@code null}
     * @return this differ for chaining
     * @throws NullPointerException if {@code types} is {@code null}
     */
    @NotNull
    public SchemaDiffer ignoreTypes(@NotNull final Set<TypeReference> types) {
        Preconditions.checkNotNull(types, "types must not be null");
        this.ignoredTypes = Set.copyOf(types);
        return this;
    }

    /**
     * Executes the diff operation and returns the result.
     *
     * @return the schema diff result, never {@code null}
     */
    @NotNull
    public SchemaDiff diff() {
        // Get all type references from both schemas
        final Set<TypeReference> sourceRefs = getTypeReferences(this.source);
        final Set<TypeReference> targetRefs = getTypeReferences(this.target);

        // Filter out ignored types
        sourceRefs.removeAll(this.ignoredTypes);
        targetRefs.removeAll(this.ignoredTypes);

        // Calculate added, removed, and common types
        final Set<TypeReference> addedTypes = new HashSet<>(targetRefs);
        addedTypes.removeAll(sourceRefs);

        final Set<TypeReference> removedTypes = new HashSet<>(sourceRefs);
        removedTypes.removeAll(targetRefs);

        final Set<TypeReference> commonTypes = new HashSet<>(sourceRefs);
        commonTypes.retainAll(targetRefs);

        // Generate field-level diffs for common types if requested
        final Map<TypeReference, TypeDiff> typeDiffs = new HashMap<>();
        if (this.includeFieldLevel) {
            for (final TypeReference ref : commonTypes) {
                final Type<?> sourceType = this.source.types().get(ref);
                final Type<?> targetType = this.target.types().get(ref);

                if (sourceType != null && targetType != null) {
                    final TypeDiff typeDiff = diffType(ref, sourceType, targetType);
                    typeDiffs.put(ref, typeDiff);
                }
            }
        }

        return SchemaDiff.builder(this.source, this.target)
                .addedTypes(addedTypes)
                .removedTypes(removedTypes)
                .commonTypes(commonTypes)
                .typeDiffs(typeDiffs)
                .build();
    }

    /**
     * Extracts all type references from a schema's type registry.
     *
     * <p>Returns a mutable copy of the references to allow subsequent
     * filtering operations without modifying the original registry.</p>
     *
     * @param schema the schema to extract references from, must not be {@code null}
     * @return a mutable set of type references, never {@code null}
     */
    @NotNull
    private Set<TypeReference> getTypeReferences(@NotNull final Schema schema) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        return new HashSet<>(schema.types().references());
    }

    /**
     * Creates a detailed type diff by comparing fields between two versions of a type.
     *
     * <p>This method performs field-level comparison by:</p>
     * <ol>
     *   <li>Extracting all fields from both type versions using {@link TypeIntrospector}</li>
     *   <li>Building name-to-field maps for efficient lookup</li>
     *   <li>Identifying added fields (in target but not source)</li>
     *   <li>Identifying removed fields (in source but not target)</li>
     *   <li>Comparing common fields for modifications</li>
     * </ol>
     *
     * @param ref        the type reference, must not be {@code null}
     * @param sourceType the type from the source schema, must not be {@code null}
     * @param targetType the type from the target schema, must not be {@code null}
     * @return a TypeDiff containing all field-level differences, never {@code null}
     */
    @NotNull
    private TypeDiff diffType(
            @NotNull final TypeReference ref,
            @NotNull final Type<?> sourceType,
            @NotNull final Type<?> targetType
    ) {
        Preconditions.checkNotNull(ref, "ref must not be null");
        Preconditions.checkNotNull(sourceType, "sourceType must not be null");
        Preconditions.checkNotNull(targetType, "targetType must not be null");

        final List<FieldInfo> sourceFields = TypeIntrospector.extractFields(sourceType);
        final List<FieldInfo> targetFields = TypeIntrospector.extractFields(targetType);

        // Build maps by field name for easier comparison
        final Map<String, FieldInfo> sourceFieldMap = new HashMap<>();
        for (final FieldInfo field : sourceFields) {
            sourceFieldMap.put(field.name(), field);
        }

        final Map<String, FieldInfo> targetFieldMap = new HashMap<>();
        for (final FieldInfo field : targetFields) {
            targetFieldMap.put(field.name(), field);
        }

        final List<FieldDiff> fieldDiffs = new ArrayList<>();

        // Find added and common fields
        for (final Map.Entry<String, FieldInfo> entry : targetFieldMap.entrySet()) {
            final String fieldName = entry.getKey();
            final FieldInfo targetField = entry.getValue();
            final FieldInfo sourceField = sourceFieldMap.get(fieldName);

            if (sourceField == null) {
                // Added field
                fieldDiffs.add(FieldDiff.added(targetField));
            } else {
                // Common field - compare
                fieldDiffs.add(FieldDiff.compare(sourceField, targetField));
            }
        }

        // Find removed fields
        for (final Map.Entry<String, FieldInfo> entry : sourceFieldMap.entrySet()) {
            final String fieldName = entry.getKey();
            if (!targetFieldMap.containsKey(fieldName)) {
                fieldDiffs.add(FieldDiff.removed(entry.getValue()));
            }
        }

        return TypeDiff.of(ref, sourceType, targetType, fieldDiffs);
    }
}
