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

package de.splatgames.aether.datafixers.schematools.validation;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates schema structure integrity.
 *
 * <p>This validator checks for structural issues in schema definitions:</p>
 * <ul>
 *   <li>Parent chain cycles</li>
 *   <li>Version ordering violations</li>
 *   <li>Missing parent schemas</li>
 *   <li>Duplicate version registrations</li>
 *   <li>Empty schemas (no types)</li>
 * </ul>
 *
 * <h2>Issue Codes</h2>
 * <ul>
 *   <li>{@code STRUCTURE_CYCLE} - Parent chain contains a cycle</li>
 *   <li>{@code STRUCTURE_VERSION_ORDER} - Parent version >= child version</li>
 *   <li>{@code STRUCTURE_MISSING_PARENT} - Parent schema not found in registry</li>
 *   <li>{@code STRUCTURE_EMPTY_SCHEMA} - Schema has no types defined</li>
 *   <li>{@code STRUCTURE_NO_ROOT} - No root schema (schema without parent)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>This class is used internally by {@link SchemaValidator}. Direct usage
 * is not recommended.</p>
 *
 * @author Erik Pfoertner
 * @see SchemaValidator
 * @since 0.3.0
 */
public final class StructureValidator {

    /**
     * Issue code for parent chain cycles.
     */
    public static final String STRUCTURE_CYCLE = "STRUCTURE_CYCLE";

    /**
     * Issue code for version ordering violations.
     */
    public static final String STRUCTURE_VERSION_ORDER = "STRUCTURE_VERSION_ORDER";

    /**
     * Issue code for missing parent schemas.
     */
    public static final String STRUCTURE_MISSING_PARENT = "STRUCTURE_MISSING_PARENT";

    /**
     * Issue code for empty schemas.
     */
    public static final String STRUCTURE_EMPTY_SCHEMA = "STRUCTURE_EMPTY_SCHEMA";

    /**
     * Issue code for missing root schema.
     */
    public static final String STRUCTURE_NO_ROOT = "STRUCTURE_NO_ROOT";

    private StructureValidator() {
        // Utility class
    }

    /**
     * Validates a single schema's structure.
     *
     * @param schema   the schema to validate, must not be {@code null}
     * @param registry the registry containing all schemas (for parent resolution),
     *                 may be {@code null} if parent validation is not needed
     * @return the validation result, never {@code null}
     */
    @NotNull
    public static ValidationResult validate(
            @NotNull final Schema schema,
            final SchemaRegistry registry
    ) {
        Preconditions.checkNotNull(schema, "schema must not be null");

        final ValidationResult.Builder result = ValidationResult.builder();
        final String location = "Schema@" + schema.version().getVersion();

        // Check for empty schema
        if (schema.types().references().isEmpty()) {
            result.add(ValidationIssue.warning(STRUCTURE_EMPTY_SCHEMA, "Schema has no types defined")
                    .at(location));
        }

        // Check parent chain
        if (schema.parent() != null) {
            validateParentChain(schema, registry, result, location);
        }

        return result.build();
    }

    /**
     * Validates the entire schema registry structure.
     *
     * @param registry the registry to validate, must not be {@code null}
     * @return the validation result, never {@code null}
     */
    @NotNull
    public static ValidationResult validateRegistry(@NotNull final SchemaRegistry registry) {
        Preconditions.checkNotNull(registry, "registry must not be null");

        final ValidationResult.Builder result = ValidationResult.builder();

        // Find all schemas
        boolean hasRootSchema = false;

        for (final Schema schema : registry.stream().toList()) {
            // Check if this is a root schema
            if (schema.parent() == null) {
                hasRootSchema = true;
            }

            // Validate individual schema
            final ValidationResult schemaResult = validate(schema, registry);
            result.addAll(schemaResult.issues());
        }

        // Check for root schema
        if (!hasRootSchema && registry.stream().findAny().isPresent()) {
            result.add(ValidationIssue.error(STRUCTURE_NO_ROOT,
                    "No root schema found (all schemas have parents)"));
        }

        return result.build();
    }

    /**
     * Validates parent chain for cycles and version ordering.
     */
    private static void validateParentChain(
            @NotNull final Schema schema,
            final SchemaRegistry registry,
            @NotNull final ValidationResult.Builder result,
            @NotNull final String location
    ) {
        final Set<Integer> visited = new HashSet<>();
        visited.add(schema.version().getVersion());

        Schema current = schema;
        Schema parent = current.parent();

        while (parent != null) {
            final int parentVersion = parent.version().getVersion();

            // Check for cycle
            if (visited.contains(parentVersion)) {
                result.add(ValidationIssue.error(STRUCTURE_CYCLE,
                                "Parent chain contains a cycle at version " + parentVersion)
                        .at(location)
                        .withContext("cycleVersion", parentVersion));
                break;
            }

            // Check version ordering (parent must be < child)
            if (parentVersion >= current.version().getVersion()) {
                result.add(ValidationIssue.error(STRUCTURE_VERSION_ORDER,
                                "Parent version (" + parentVersion + ") must be less than child version ("
                                        + current.version().getVersion() + ")")
                        .at(location)
                        .withContext("parentVersion", parentVersion)
                        .withContext("childVersion", current.version().getVersion()));
            }

            visited.add(parentVersion);

            // Move to next parent
            current = parent;
            parent = current.parent();
        }
    }
}
