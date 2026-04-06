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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Metadata about a single field-level operation within a
 * {@link TypeRewriteRule} application.
 *
 * <p>{@code FieldOperation} captures structured information about which fields
 * are affected by a rule and how. This enables field-level diagnostic reporting beyond the type-level granularity of
 * {@link RuleApplication}.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MigrationReport report = context.getReport();
 * for (FixExecution fix : report.fixExecutions()) {
 *     for (RuleApplication rule : fix.ruleApplications()) {
 *         for (FieldOperation fieldOp : rule.fieldOperations()) {
 *             System.out.println(fieldOp.operationType() + " on " +
 *                 String.join(".", fieldOp.fieldPath()));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Path Representation</h2>
 * <p>Field paths are represented as a list of path segments. A top-level field
 * {@code "name"} is represented as {@code ["name"]}, while a nested field {@code "position.x"} is represented as
 * {@code ["position", "x"]}.</p>
 *
 * @param operationType   the kind of field operation, must not be {@code null}
 * @param fieldPath       the path segments to the affected field, must not be {@code null} or empty
 * @param targetFieldName the target field name for operations that have one (e.g., rename target, move/copy destination
 *                        as dot-notation path); {@code null} for operations without a target
 * @param description     optional human-readable description providing additional context; may be {@code null}
 * @author Erik Pförtner
 * @see FieldOperationType
 * @see RuleApplication#fieldOperations()
 * @see FieldAwareRule
 * @since 1.0.0
 */
public record FieldOperation(
        @NotNull FieldOperationType operationType,
        @NotNull List<String> fieldPath,
        @Nullable String targetFieldName,
        @Nullable String description
) {

    /**
     * Creates a new field operation record.
     *
     * @param operationType   the kind of field operation, must not be {@code null}
     * @param fieldPath       the path segments to the affected field, must not be {@code null} or empty
     * @param targetFieldName the target field name (may be {@code null})
     * @param description     optional description (may be {@code null})
     * @throws NullPointerException     if {@code operationType} or {@code fieldPath} is {@code null}
     * @throws IllegalArgumentException if {@code fieldPath} is empty
     */
    public FieldOperation {
        Preconditions.checkNotNull(operationType, "operationType must not be null");
        Preconditions.checkNotNull(fieldPath, "fieldPath must not be null");
        Preconditions.checkArgument(!fieldPath.isEmpty(), "fieldPath must not be empty");
        fieldPath = List.copyOf(fieldPath);
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Top-Level Fields
    // -------------------------------------------------------------------------

    /**
     * Creates a rename field operation.
     *
     * @param oldName the current field name, must not be {@code null}
     * @param newName the new field name, must not be {@code null}
     * @return a new rename field operation
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldOperation rename(@NotNull final String oldName,
                                        @NotNull final String newName) {
        Preconditions.checkNotNull(oldName, "oldName must not be null");
        Preconditions.checkNotNull(newName, "newName must not be null");
        return new FieldOperation(FieldOperationType.RENAME, List.of(oldName), newName, null);
    }

    /**
     * Creates a remove field operation.
     *
     * @param fieldName the field name to remove, must not be {@code null}
     * @return a new remove field operation
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    public static FieldOperation remove(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new FieldOperation(FieldOperationType.REMOVE, List.of(fieldName), null, null);
    }

    /**
     * Creates an add field operation.
     *
     * @param fieldName the field name to add, must not be {@code null}
     * @return a new add field operation
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    public static FieldOperation add(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new FieldOperation(FieldOperationType.ADD, List.of(fieldName), null, null);
    }

    /**
     * Creates a transform field operation.
     *
     * @param fieldName the field name to transform, must not be {@code null}
     * @return a new transform field operation
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    public static FieldOperation transform(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new FieldOperation(FieldOperationType.TRANSFORM, List.of(fieldName), null, null);
    }

    /**
     * Creates a set field operation.
     *
     * @param fieldName the field name to set, must not be {@code null}
     * @return a new set field operation
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    public static FieldOperation set(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new FieldOperation(FieldOperationType.SET, List.of(fieldName), null, null);
    }

    /**
     * Creates a move field operation.
     *
     * @param sourcePath the source field path in dot-notation, must not be {@code null}
     * @param targetPath the target field path in dot-notation, must not be {@code null}
     * @return a new move field operation
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldOperation move(@NotNull final String sourcePath,
                                      @NotNull final String targetPath) {
        Preconditions.checkNotNull(sourcePath, "sourcePath must not be null");
        Preconditions.checkNotNull(targetPath, "targetPath must not be null");
        return new FieldOperation(FieldOperationType.MOVE, parsePath(sourcePath), targetPath, null);
    }

    /**
     * Creates a copy field operation.
     *
     * @param sourcePath the source field path in dot-notation, must not be {@code null}
     * @param targetPath the target field path in dot-notation, must not be {@code null}
     * @return a new copy field operation
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldOperation copy(@NotNull final String sourcePath,
                                      @NotNull final String targetPath) {
        Preconditions.checkNotNull(sourcePath, "sourcePath must not be null");
        Preconditions.checkNotNull(targetPath, "targetPath must not be null");
        return new FieldOperation(FieldOperationType.COPY, parsePath(sourcePath), targetPath, null);
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Nested / Path-Based Fields
    // -------------------------------------------------------------------------

    /**
     * Creates a rename operation for a nested field specified by dot-notation path.
     *
     * @param path    the dot-notation path to the field (e.g., {@code "position.posX"}), must not be {@code null}
     * @param newName the new name for the leaf field, must not be {@code null}
     * @return a new rename field operation with a nested path
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldOperation renamePath(@NotNull final String path,
                                            @NotNull final String newName) {
        Preconditions.checkNotNull(path, "path must not be null");
        Preconditions.checkNotNull(newName, "newName must not be null");
        return new FieldOperation(FieldOperationType.RENAME, parsePath(path), newName, null);
    }

    /**
     * Creates a remove operation for a nested field specified by dot-notation path.
     *
     * @param path the dot-notation path to the field, must not be {@code null}
     * @return a new remove field operation with a nested path
     * @throws NullPointerException if {@code path} is {@code null}
     */
    @NotNull
    public static FieldOperation removePath(@NotNull final String path) {
        Preconditions.checkNotNull(path, "path must not be null");
        return new FieldOperation(FieldOperationType.REMOVE, parsePath(path), null, null);
    }

    /**
     * Creates an add operation for a nested field specified by dot-notation path.
     *
     * @param path the dot-notation path to the field, must not be {@code null}
     * @return a new add field operation with a nested path
     * @throws NullPointerException if {@code path} is {@code null}
     */
    @NotNull
    public static FieldOperation addPath(@NotNull final String path) {
        Preconditions.checkNotNull(path, "path must not be null");
        return new FieldOperation(FieldOperationType.ADD, parsePath(path), null, null);
    }

    /**
     * Creates a transform operation for a nested field specified by dot-notation path.
     *
     * @param path the dot-notation path to the field, must not be {@code null}
     * @return a new transform field operation with a nested path
     * @throws NullPointerException if {@code path} is {@code null}
     */
    @NotNull
    public static FieldOperation transformPath(@NotNull final String path) {
        Preconditions.checkNotNull(path, "path must not be null");
        return new FieldOperation(FieldOperationType.TRANSFORM, parsePath(path), null, null);
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Structural Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a group fields operation.
     *
     * <p>This represents grouping multiple source fields into a nested object
     * at the target field name.</p>
     *
     * @param targetField  the name of the new nested object field, must not be {@code null}
     * @param sourceFields the fields to group into the target, must not be {@code null} or empty
     * @return a new group field operation
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code sourceFields} is empty
     */
    @NotNull
    public static FieldOperation group(@NotNull final String targetField,
                                       @NotNull final String... sourceFields) {
        Preconditions.checkNotNull(targetField, "targetField must not be null");
        Preconditions.checkNotNull(sourceFields, "sourceFields must not be null");
        Preconditions.checkArgument(sourceFields.length > 0, "sourceFields must not be empty");
        return new FieldOperation(
                FieldOperationType.GROUP,
                List.copyOf(Arrays.asList(sourceFields)),
                targetField,
                null
        );
    }

    /**
     * Creates a flatten field operation.
     *
     * @param fieldName the name of the nested object to flatten, must not be {@code null}
     * @return a new flatten field operation
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    public static FieldOperation flatten(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new FieldOperation(FieldOperationType.FLATTEN, List.of(fieldName), null, null);
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Conditional Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a conditional field operation.
     *
     * @param fieldName     the field name that the condition checks, must not be {@code null}
     * @param conditionType a description of the condition (e.g., {@code "exists"}, {@code "missing"},
     *                      {@code "equals"}), must not be {@code null}
     * @return a new conditional field operation
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldOperation conditional(@NotNull final String fieldName,
                                             @NotNull final String conditionType) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        Preconditions.checkNotNull(conditionType, "conditionType must not be null");
        return new FieldOperation(
                FieldOperationType.CONDITIONAL,
                List.of(fieldName),
                null,
                conditionType
        );
    }

    // -------------------------------------------------------------------------
    // Convenience Methods
    // -------------------------------------------------------------------------

    /**
     * Parses a dot-notation path string into a list of path segments.
     *
     * @param path the dot-notation path (e.g., {@code "position.x"})
     * @return list of path segments (e.g., {@code ["position", "x"]})
     */
    @NotNull
    private static List<String> parsePath(@NotNull final String path) {
        return List.of(path.split("\\."));
    }

    /**
     * Returns the target field name as an {@link Optional}.
     *
     * @return optional containing the target field name, or empty if none
     */
    @NotNull
    public Optional<String> targetFieldNameOpt() {
        return Optional.ofNullable(this.targetFieldName);
    }

    /**
     * Returns the description as an {@link Optional}.
     *
     * @return optional containing the description, or empty if none
     */
    @NotNull
    public Optional<String> descriptionOpt() {
        return Optional.ofNullable(this.description);
    }

    /**
     * Returns the field path as a dot-notation string.
     *
     * <p>For example, a path of {@code ["position", "x"]} returns {@code "position.x"}.</p>
     *
     * @return the dot-notation field path, never {@code null}
     */
    @NotNull
    public String fieldPathString() {
        return String.join(".", this.fieldPath);
    }

    /**
     * Returns whether this operation targets a nested field (path depth &gt; 1).
     *
     * @return {@code true} if the field path has more than one segment
     */
    public boolean isNested() {
        return this.fieldPath.size() > 1;
    }

    // -------------------------------------------------------------------------
    // Internal Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable summary of this field operation.
     *
     * @return formatted summary string, never {@code null}
     */
    @NotNull
    public String toSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.operationType.name()).append('(').append(fieldPathString());
        if (this.targetFieldName != null) {
            sb.append(" -> ").append(this.targetFieldName);
        }
        sb.append(')');
        if (this.description != null) {
            sb.append(" [").append(this.description).append(']');
        }
        return sb.toString();
    }
}
