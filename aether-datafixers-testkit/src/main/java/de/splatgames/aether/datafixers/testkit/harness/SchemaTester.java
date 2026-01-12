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

package de.splatgames.aether.datafixers.testkit.harness;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A fluent test harness for validating {@link Schema} implementations.
 *
 * <p>{@code SchemaTester} provides a clean API for testing schema configurations,
 * including version validation, type registration verification, and inheritance
 * chain validation.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * SchemaTester.forSchema(schema110)
 *     .hasVersion(110)
 *     .containsType(PLAYER)
 *     .containsType(WORLD)
 *     .verify();
 * }</pre>
 *
 * <h2>Validating Multiple Types</h2>
 * <pre>{@code
 * SchemaTester.forSchema(schema)
 *     .hasVersion(200)
 *     .containsTypes(PLAYER, WORLD, ENTITY, INVENTORY)
 *     .doesNotContainType(LEGACY_DATA)
 *     .verify();
 * }</pre>
 *
 * <h2>Validating Inheritance</h2>
 * <pre>{@code
 * SchemaTester.forSchema(schema110)
 *     .hasVersion(110)
 *     .hasParent()
 *     .inheritsFrom(schema100)
 *     .verify();
 *
 * SchemaTester.forSchema(schema100)
 *     .hasVersion(100)
 *     .hasNoParent()
 *     .verify();
 * }</pre>
 *
 * <h2>Type Validation</h2>
 * <pre>{@code
 * // Validate a specific type is correctly configured
 * SchemaTester.forSchema(schema)
 *     .hasVersion(100)
 *     .typeForReference(PLAYER, type -> {
 *         assertThat(type.reference()).isEqualTo(PLAYER);
 *         // Additional type-specific assertions...
 *     })
 *     .verify();
 * }</pre>
 *
 * @author Erik Pförtner
 * @see Schema
 * @since 0.2.0
 */
public final class SchemaTester {

    private final Schema schema;
    private Integer expectedVersion;
    private Schema expectedParent;
    private Boolean expectHasParent;

    private SchemaTester(@NotNull final Schema schema) {
        this.schema = Preconditions.checkNotNull(schema, "schema must not be null");
    }

    /**
     * Creates a tester for the given {@link Schema}.
     *
     * @param schema the Schema to test
     * @return a new SchemaTester
     * @throws NullPointerException if {@code schema} is null
     */
    @NotNull
    public static SchemaTester forSchema(@NotNull final Schema schema) {
        return new SchemaTester(schema);
    }

    // ==================== Version Validation ====================

    /**
     * Asserts that the schema has the specified version.
     *
     * @param version the expected version number
     * @return this tester for chaining
     */
    @NotNull
    public SchemaTester hasVersion(final int version) {
        this.expectedVersion = version;
        return this;
    }

    /**
     * Asserts that the schema has the specified version.
     *
     * @param version the expected DataVersion
     * @return this tester for chaining
     * @throws NullPointerException if {@code version} is null
     */
    @NotNull
    public SchemaTester hasVersion(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");
        this.expectedVersion = version.getVersion();
        return this;
    }

    // ==================== Type Validation ====================

    /**
     * Asserts that the schema contains a type for the given reference.
     *
     * @param type the TypeReference to check
     * @return this tester for chaining
     * @throws AssertionError       if the type is not registered
     * @throws NullPointerException if {@code type} is null
     */
    @NotNull
    public SchemaTester containsType(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");

        if (!this.schema.types().has(type)) {
            throw new AssertionError(String.format(
                    "Schema v%d does not contain type '%s'",
                    this.schema.version().getVersion(),
                    type.getId()
            ));
        }
        return this;
    }

    /**
     * Asserts that the schema contains a type for the given type id.
     *
     * @param typeId the type reference id
     * @return this tester for chaining
     * @throws AssertionError       if the type is not registered
     * @throws NullPointerException if {@code typeId} is null
     */
    @NotNull
    public SchemaTester containsType(@NotNull final String typeId) {
        Preconditions.checkNotNull(typeId, "typeId must not be null");
        return this.containsType(new TypeReference(typeId));
    }

    /**
     * Asserts that the schema contains all specified types.
     *
     * @param types the TypeReferences to check
     * @return this tester for chaining
     * @throws AssertionError       if any type is not registered
     * @throws NullPointerException if {@code types} is null
     */
    @NotNull
    public SchemaTester containsTypes(@NotNull final TypeReference... types) {
        Preconditions.checkNotNull(types, "types must not be null");

        final String missing = Arrays.stream(types)
                .filter(type -> !this.schema.types().has(type))
                .map(TypeReference::getId)
                .collect(Collectors.joining(", "));

        if (!missing.isEmpty()) {
            throw new AssertionError(String.format(
                    "Schema v%d is missing types: [%s]",
                    this.schema.version().getVersion(),
                    missing
            ));
        }
        return this;
    }

    /**
     * Asserts that the schema does NOT contain a type for the given reference.
     *
     * @param type the TypeReference to check
     * @return this tester for chaining
     * @throws AssertionError       if the type IS registered
     * @throws NullPointerException if {@code type} is null
     */
    @NotNull
    public SchemaTester doesNotContainType(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");

        if (this.schema.types().has(type)) {
            throw new AssertionError(String.format(
                    "Schema v%d unexpectedly contains type '%s'",
                    this.schema.version().getVersion(),
                    type.getId()
            ));
        }
        return this;
    }

    /**
     * Asserts that the schema does NOT contain a type for the given type id.
     *
     * @param typeId the type reference id
     * @return this tester for chaining
     * @throws AssertionError       if the type IS registered
     * @throws NullPointerException if {@code typeId} is null
     */
    @NotNull
    public SchemaTester doesNotContainType(@NotNull final String typeId) {
        Preconditions.checkNotNull(typeId, "typeId must not be null");
        return this.doesNotContainType(new TypeReference(typeId));
    }

    /**
     * Validates a specific type using a custom validator.
     *
     * @param type      the TypeReference to retrieve
     * @param validator the validator to run on the type
     * @return this tester for chaining
     * @throws AssertionError       if the type is not registered or validation fails
     * @throws NullPointerException if {@code type} or {@code validator} is null
     */
    @NotNull
    public SchemaTester typeForReference(
            @NotNull final TypeReference type,
            @NotNull final TypeValidator validator
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(validator, "validator must not be null");

        if (!this.schema.types().has(type)) {
            throw new AssertionError(String.format(
                    "Schema v%d does not contain type '%s'",
                    this.schema.version().getVersion(),
                    type.getId()
            ));
        }

        final Type<?> schemaType = this.schema.types().get(type);
        validator.validate(schemaType);
        return this;
    }

    // ==================== Parent/Inheritance Validation ====================

    /**
     * Asserts that the schema has a parent.
     *
     * @return this tester for chaining
     */
    @NotNull
    public SchemaTester hasParent() {
        this.expectHasParent = true;
        return this;
    }

    /**
     * Asserts that the schema has no parent (is the root schema).
     *
     * @return this tester for chaining
     */
    @NotNull
    public SchemaTester hasNoParent() {
        this.expectHasParent = false;
        return this;
    }

    /**
     * Asserts that the schema inherits from the specified parent schema.
     *
     * @param parent the expected parent schema
     * @return this tester for chaining
     * @throws NullPointerException if {@code parent} is null
     */
    @NotNull
    public SchemaTester inheritsFrom(@NotNull final Schema parent) {
        Preconditions.checkNotNull(parent, "parent must not be null");
        this.expectedParent = parent;
        this.expectHasParent = true;
        return this;
    }

    /**
     * Asserts that the schema's parent has the specified version.
     *
     * @param parentVersion the expected parent version
     * @return this tester for chaining
     */
    @NotNull
    public SchemaTester parentHasVersion(final int parentVersion) {
        this.expectHasParent = true;

        final Schema parent = this.schema.parent();
        if (parent == null) {
            throw new AssertionError(String.format(
                    "Schema v%d has no parent, but expected parent with version %d",
                    this.schema.version().getVersion(),
                    parentVersion
            ));
        }

        if (parent.version().getVersion() != parentVersion) {
            throw new AssertionError(String.format(
                    "Schema v%d has parent with version %d, but expected version %d",
                    this.schema.version().getVersion(),
                    parent.version().getVersion(),
                    parentVersion
            ));
        }
        return this;
    }

    // ==================== Execution ====================

    /**
     * Runs all configured validations.
     *
     * @return this tester for chaining (allows further assertions)
     * @throws AssertionError if any validation fails
     */
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "Parent null check is performed via hasParent boolean before access."
    )
    @NotNull
    public SchemaTester verify() {
        // Validate version
        if (this.expectedVersion != null) {
            final int actualVersion = this.schema.version().getVersion();
            if (actualVersion != this.expectedVersion) {
                throw new AssertionError(String.format(
                        "Schema has version %d, but expected %d",
                        actualVersion,
                        this.expectedVersion
                ));
            }
        }

        // Validate parent existence
        if (this.expectHasParent != null) {
            final boolean hasParent = this.schema.parent() != null;
            if (this.expectHasParent && !hasParent) {
                throw new AssertionError(String.format(
                        "Schema v%d has no parent, but one was expected",
                        this.schema.version().getVersion()
                ));
            }
            if (!this.expectHasParent && hasParent) {
                throw new AssertionError(String.format(
                        "Schema v%d has a parent (v%d), but none was expected",
                        this.schema.version().getVersion(),
                        this.schema.parent().version().getVersion()
                ));
            }
        }

        // Validate specific parent
        if (this.expectedParent != null) {
            final Schema actualParent = this.schema.parent();
            if (actualParent == null) {
                throw new AssertionError(String.format(
                        "Schema v%d has no parent, but expected parent v%d",
                        this.schema.version().getVersion(),
                        this.expectedParent.version().getVersion()
                ));
            }
            if (!Objects.equals(actualParent, this.expectedParent)) {
                throw new AssertionError(String.format(
                        "Schema v%d has different parent than expected.%nExpected: v%d%nActual: v%d",
                        this.schema.version().getVersion(),
                        this.expectedParent.version().getVersion(),
                        actualParent.version().getVersion()
                ));
            }
        }

        return this;
    }

    /**
     * Returns the schema being tested.
     *
     * @return the schema
     */
    @NotNull
    public Schema schema() {
        return this.schema;
    }

    // ==================== Type Validator ====================

    /**
     * Functional interface for custom type validation.
     */
    @FunctionalInterface
    public interface TypeValidator {

        /**
         * Validates the given type.
         *
         * @param type the type to validate
         * @throws AssertionError if validation fails
         */
        void validate(@Nullable Type<?> type);
    }
}
