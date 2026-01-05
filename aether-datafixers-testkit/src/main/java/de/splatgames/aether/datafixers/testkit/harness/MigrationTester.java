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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A fluent test harness for testing complete data migration chains.
 *
 * <p>{@code MigrationTester} provides a clean API for testing full data migrations
 * from one version to another through a {@link DataFixer}.</p>
 *
 * <h2>Testing with an Existing Fixer</h2>
 * <pre>{@code
 * MigrationTester.forFixer(myFixer)
 *     .forType(PLAYER)
 *     .withInput(v1PlayerData)
 *     .from(1).to(5)
 *     .expectOutput(v5PlayerData)
 *     .verify();
 * }</pre>
 *
 * <h2>Testing with Quick Setup</h2>
 * <pre>{@code
 * MigrationTester.withFixes(builder -> {
 *     builder.addFix(PLAYER, fix1);
 *     builder.addFix(PLAYER, fix2);
 *     builder.addFix(PLAYER, fix3);
 * })
 *     .forType(PLAYER)
 *     .withInput(inputData)
 *     .from(1).to(4)
 *     .expectOutput(expectedData)
 *     .verify();
 * }</pre>
 *
 * <h2>Getting the Result</h2>
 * <pre>{@code
 * Dynamic<?> result = MigrationTester.forFixer(myFixer)
 *     .forType(PLAYER)
 *     .withInput(inputData)
 *     .from(1).to(5)
 *     .migrate();
 *
 * assertThat(result).hasField("newField");
 * }</pre>
 *
 * @param <T> the underlying value type (e.g., JsonElement)
 * @author Erik Pförtner
 * @since 0.2.0
 */
public final class MigrationTester<T> {

    private final DataFixer fixer;
    private TypeReference typeReference;
    private Dynamic<T> input;
    private DataVersion fromVersion;
    private DataVersion toVersion;
    private Dynamic<T> expectedOutput;

    private MigrationTester(@NotNull final DataFixer fixer) {
        this.fixer = Preconditions.checkNotNull(fixer, "fixer must not be null");
    }

    /**
     * Creates a tester for an existing {@link DataFixer}.
     *
     * @param fixer the DataFixer to test
     * @param <T>   the underlying value type
     * @return a new MigrationTester
     * @throws NullPointerException if {@code fixer} is null
     */
    @NotNull
    public static <T> MigrationTester<T> forFixer(@NotNull final DataFixer fixer) {
        return new MigrationTester<>(fixer);
    }

    /**
     * Creates a tester using a builder for quick setup.
     *
     * <p>The target version will be set to the highest toVersion of the registered fixes.</p>
     *
     * @param setup the consumer to configure the DataFixerBuilder
     * @param <T>   the underlying value type
     * @return a new MigrationTester
     * @throws NullPointerException if {@code setup} is null
     */
    @NotNull
    public static <T> MigrationTester<T> withFixes(@NotNull final Consumer<FixerSetup> setup) {
        Preconditions.checkNotNull(setup, "setup must not be null");

        final FixerSetup fixerSetup = new FixerSetup();
        setup.accept(fixerSetup);
        final DataFixer fixer = fixerSetup.build();
        return new MigrationTester<>(fixer);
    }

    // ==================== Configuration ====================

    /**
     * Sets the type reference for the migration.
     *
     * @param type the TypeReference
     * @return this tester for chaining
     * @throws NullPointerException if {@code type} is null
     */
    @NotNull
    public MigrationTester<T> forType(@NotNull final TypeReference type) {
        this.typeReference = Preconditions.checkNotNull(type, "type must not be null");
        return this;
    }

    /**
     * Sets the type reference using a string id.
     *
     * @param typeId the type reference id
     * @return this tester for chaining
     * @throws NullPointerException if {@code typeId} is null
     */
    @NotNull
    public MigrationTester<T> forType(@NotNull final String typeId) {
        return this.forType(new TypeReference(typeId));
    }

    /**
     * Sets the input data for the migration.
     *
     * @param input the input Dynamic
     * @return this tester for chaining
     * @throws NullPointerException if {@code input} is null
     */
    @NotNull
    public MigrationTester<T> withInput(@NotNull final Dynamic<T> input) {
        this.input = Preconditions.checkNotNull(input, "input must not be null");
        return this;
    }

    /**
     * Sets the source version.
     *
     * @param version the source version number
     * @return this tester for chaining
     */
    @NotNull
    public MigrationTester<T> from(final int version) {
        this.fromVersion = new DataVersion(version);
        return this;
    }

    /**
     * Sets the source version.
     *
     * @param version the source DataVersion
     * @return this tester for chaining
     * @throws NullPointerException if {@code version} is null
     */
    @NotNull
    public MigrationTester<T> from(@NotNull final DataVersion version) {
        this.fromVersion = Preconditions.checkNotNull(version, "version must not be null");
        return this;
    }

    /**
     * Sets the target version.
     *
     * @param version the target version number
     * @return this tester for chaining
     */
    @NotNull
    public MigrationTester<T> to(final int version) {
        this.toVersion = new DataVersion(version);
        return this;
    }

    /**
     * Sets the target version.
     *
     * @param version the target DataVersion
     * @return this tester for chaining
     * @throws NullPointerException if {@code version} is null
     */
    @NotNull
    public MigrationTester<T> to(@NotNull final DataVersion version) {
        this.toVersion = Preconditions.checkNotNull(version, "version must not be null");
        return this;
    }

    /**
     * Sets the expected output for verification.
     *
     * @param expected the expected output Dynamic
     * @return this tester for chaining
     * @throws NullPointerException if {@code expected} is null
     */
    @NotNull
    public MigrationTester<T> expectOutput(@NotNull final Dynamic<T> expected) {
        this.expectedOutput = Preconditions.checkNotNull(expected, "expected must not be null");
        return this;
    }

    // ==================== Execution ====================

    /**
     * Runs the migration and returns the result.
     *
     * @return the migrated Dynamic
     * @throws IllegalStateException if required configuration is missing
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public Dynamic<T> migrate() {
        this.validateConfiguration();
        return (Dynamic<T>) this.fixer.update(
                this.typeReference,
                this.input,
                this.fromVersion,
                this.toVersion
        );
    }

    /**
     * Runs the migration and verifies the expected output.
     *
     * @return this tester for chaining (allows further assertions)
     * @throws IllegalStateException if required configuration is missing
     * @throws AssertionError        if the output does not match the expected value
     */
    @NotNull
    public MigrationTester<T> verify() {
        final Dynamic<T> result = this.migrate();

        if (this.expectedOutput != null) {
            if (!Objects.equals(result.value(), this.expectedOutput.value())) {
                throw new AssertionError(String.format(
                        "Migration from v%d to v%d did not produce expected output.%nExpected:%n  %s%nActual:%n  %s",
                        this.fromVersion.getVersion(),
                        this.toVersion.getVersion(),
                        this.expectedOutput.value(),
                        result.value()
                ));
            }
        }

        return this;
    }

    // ==================== Validation ====================

    private void validateConfiguration() {
        if (this.typeReference == null) {
            throw new IllegalStateException("Type reference not set. Call forType() before migrate() or verify().");
        }
        if (this.input == null) {
            throw new IllegalStateException("Input not set. Call withInput() before migrate() or verify().");
        }
        if (this.fromVersion == null) {
            throw new IllegalStateException("Source version not set. Call from() before migrate() or verify().");
        }
        if (this.toVersion == null) {
            throw new IllegalStateException("Target version not set. Call to() before migrate() or verify().");
        }
    }

    // ==================== Fixer Setup Helper ====================

    /**
     * A helper class for setting up a DataFixer for testing.
     */
    public static final class FixerSetup {

        private final DataFixerBuilder builder;
        private int maxVersion = 1;

        FixerSetup() {
            // Start with a placeholder version, will be updated
            this.builder = new DataFixerBuilder(new DataVersion(Integer.MAX_VALUE));
        }

        /**
         * Adds a fix for the given type.
         *
         * @param type the TypeReference
         * @param fix  the DataFix
         * @return this setup for chaining
         */
        @NotNull
        public FixerSetup addFix(@NotNull final TypeReference type, @NotNull final DataFix<?> fix) {
            Preconditions.checkNotNull(type, "type must not be null");
            Preconditions.checkNotNull(fix, "fix must not be null");
            this.builder.addFix(type, fix);
            this.maxVersion = Math.max(this.maxVersion, fix.toVersion().getVersion());
            return this;
        }

        /**
         * Adds a fix for the given type id.
         *
         * @param typeId the type reference id
         * @param fix    the DataFix
         * @return this setup for chaining
         */
        @NotNull
        public FixerSetup addFix(@NotNull final String typeId, @NotNull final DataFix<?> fix) {
            return this.addFix(new TypeReference(typeId), fix);
        }

        DataFixer build() {
            return this.builder.build();
        }
    }
}
