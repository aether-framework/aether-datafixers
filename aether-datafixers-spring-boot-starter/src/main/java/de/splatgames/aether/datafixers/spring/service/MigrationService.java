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

package de.splatgames.aether.datafixers.spring.service;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for performing data migrations with a fluent builder API.
 *
 * <p>This service provides a high-level, user-friendly abstraction over the
 * {@link de.splatgames.aether.datafixers.core.AetherDataFixer} for performing
 * data migrations. It supports multi-domain setups, automatic metrics collection,
 * and both synchronous and asynchronous execution modes.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Fluent API:</strong> Intuitive builder pattern for configuring migrations</li>
 *   <li><strong>Multi-Domain Support:</strong> Switch between different DataFixer domains</li>
 *   <li><strong>Async Execution:</strong> Non-blocking migration with {@link CompletableFuture}</li>
 *   <li><strong>Automatic Metrics:</strong> Integration with Micrometer for observability</li>
 *   <li><strong>Version Management:</strong> Easy access to current versions per domain</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @Service
 * public class GameDataService {
 *
 *     private final MigrationService migrationService;
 *
 *     public GameDataService(MigrationService migrationService) {
 *         this.migrationService = migrationService;
 *     }
 *
 *     public GameSave loadAndMigrate(TaggedDynamic<?> savedData, int savedVersion) {
 *         MigrationResult result = migrationService
 *             .migrate(savedData)
 *             .from(savedVersion)
 *             .toLatest()
 *             .execute();
 *
 *         if (result.isSuccess()) {
 *             return deserialize(result.getData());
 *         } else {
 *             throw new MigrationException(result.getError().orElse(null));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Multi-Domain Usage</h2>
 * <pre>{@code
 * // Migrate game data using the "game" domain
 * MigrationResult gameResult = migrationService
 *     .migrate(gameSaveData)
 *     .usingDomain("game")
 *     .from(100)
 *     .to(200)
 *     .execute();
 *
 * // Migrate user data using the "user" domain
 * MigrationResult userResult = migrationService
 *     .migrate(userProfileData)
 *     .usingDomain("user")
 *     .from(50)
 *     .toLatest()
 *     .execute();
 * }</pre>
 *
 * <h2>Asynchronous Execution</h2>
 * <pre>{@code
 * CompletableFuture<MigrationResult> future = migrationService
 *     .migrate(largeDataSet)
 *     .from(100)
 *     .to(200)
 *     .executeAsync();
 *
 * // Process result when available
 * future
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             processData(result.getData());
 *         }
 *     })
 *     .exceptionally(throwable -> {
 *         log.error("Migration failed", throwable);
 *         return null;
 *     });
 *
 * // Or wait for completion with timeout
 * MigrationResult result = future.get(30, TimeUnit.SECONDS);
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <pre>{@code
 * MigrationResult result = migrationService
 *     .migrate(data)
 *     .from(100)
 *     .to(200)
 *     .execute();
 *
 * if (result.isFailure()) {
 *     Throwable error = result.getError()
 *         .orElse(new RuntimeException("Unknown error"));
 *     log.error("Migration failed after {}ms: {}",
 *         result.getDuration().toMillis(),
 *         error.getMessage());
 *     // Handle error appropriately
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations of this interface must be thread-safe. The
 * {@link DefaultMigrationService} implementation is fully thread-safe and can
 * be safely shared across multiple threads and used concurrently.</p>
 *
 * @author Erik Pförtner
 * @see MigrationResult
 * @see MigrationRequestBuilder
 * @see DefaultMigrationService
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry
 * @since 0.4.0
 */
public interface MigrationService {

    /**
     * Starts a migration request builder for the given tagged data.
     *
     * <p>This method begins the fluent API chain for configuring a migration.
     * The returned builder must be configured with at least:</p>
     * <ul>
     *   <li>Source version via {@link MigrationRequestBuilder#from(DataVersion)}</li>
     *   <li>Target version via {@link MigrationRequestBuilder#to(DataVersion)} or
     *       {@link MigrationRequestBuilder#toLatest()}</li>
     * </ul>
     *
     * <p>Optionally, you can also specify:</p>
     * <ul>
     *   <li>Domain via {@link MigrationRequestBuilder#usingDomain(String)}</li>
     *   <li>Custom DynamicOps via {@link MigrationRequestBuilder#withOps(DynamicOps)}</li>
     * </ul>
     *
     * @param data the tagged dynamic data to migrate, must not be {@code null}
     * @return a builder for configuring the migration request
     * @throws NullPointerException if data is {@code null}
     */
    @NotNull
    MigrationRequestBuilder migrate(@NotNull TaggedDynamic data);

    /**
     * Returns the current (latest) version for the default domain.
     *
     * <p>The default domain is used in single-bootstrap setups where no explicit
     * domain qualification is needed. This method is equivalent to calling
     * {@code getCurrentVersion(DataFixerRegistry.DEFAULT_DOMAIN)}.</p>
     *
     * @return the current data version for the default domain, never {@code null}
     * @throws IllegalArgumentException if no default domain is configured
     */
    @NotNull
    DataVersion getCurrentVersion();

    /**
     * Returns the current (latest) version for a specific domain.
     *
     * <p>This method allows querying the target version for any registered domain,
     * useful for version comparisons and migration planning.</p>
     *
     * @param domain the domain name to query, must not be {@code null}
     * @return the current data version for the specified domain, never {@code null}
     * @throws IllegalArgumentException if the domain does not exist
     * @throws NullPointerException     if domain is {@code null}
     */
    @NotNull
    DataVersion getCurrentVersion(@NotNull String domain);

    /**
     * Checks whether a specific domain is available for migrations.
     *
     * <p>Use this method to verify domain availability before attempting
     * migrations, particularly in dynamic multi-domain scenarios.</p>
     *
     * @param domain the domain name to check, must not be {@code null}
     * @return {@code true} if the domain exists and can be used for migrations,
     *         {@code false} otherwise
     * @throws NullPointerException if domain is {@code null}
     */
    boolean hasDomain(@NotNull String domain);

    /**
     * Returns all available domain names registered with this service.
     *
     * <p>The returned set includes all domains that can be used with
     * {@link MigrationRequestBuilder#usingDomain(String)}. The set is
     * immutable and represents a snapshot of the registered domains.</p>
     *
     * @return an unmodifiable set of domain names, never {@code null}
     */
    @NotNull
    Set<String> getAvailableDomains();

    /**
     * Builder interface for configuring and executing migration requests.
     *
     * <p>This builder follows the fluent API pattern, allowing method chaining
     * to configure all aspects of a migration before execution. The builder
     * enforces required configuration at execution time, throwing exceptions
     * for incomplete configurations.</p>
     *
     * <h2>Required Configuration</h2>
     * <p>Before calling {@link #execute()} or {@link #executeAsync()}, you must:</p>
     * <ol>
     *   <li>Specify the source version with {@link #from(DataVersion)} or {@link #from(int)}</li>
     *   <li>Specify the target version with {@link #to(DataVersion)}, {@link #to(int)},
     *       or {@link #toLatest()}</li>
     * </ol>
     *
     * <h2>Optional Configuration</h2>
     * <ul>
     *   <li>{@link #usingDomain(String)} - Select a specific domain (default: "default")</li>
     *   <li>{@link #withOps(DynamicOps)} - Provide custom DynamicOps</li>
     * </ul>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * MigrationResult result = service.migrate(data)
     *     .from(100)              // Required: source version
     *     .to(200)                // Required: target version
     *     .usingDomain("game")    // Optional: domain selection
     *     .execute();             // Execute migration
     * }</pre>
     *
     * <h2>Thread Safety</h2>
     * <p>Builder instances are NOT thread-safe. Each thread should obtain its
     * own builder instance from {@link MigrationService#migrate(TaggedDynamic)}.</p>
     *
     * @author Erik Pförtner
     * @see MigrationService
     * @see MigrationResult
     * @since 0.4.0
     */
    interface MigrationRequestBuilder {

        /**
         * Specifies the source data version for the migration.
         *
         * <p>This is the version of the input data before migration. The
         * DataFixer will apply all fixes from this version up to (but not
         * including) the target version.</p>
         *
         * @param version the source data version, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if version is {@code null}
         */
        @NotNull
        MigrationRequestBuilder from(@NotNull DataVersion version);

        /**
         * Specifies the source data version by integer value.
         *
         * <p>This is a convenience method equivalent to
         * {@code from(new DataVersion(version))}.</p>
         *
         * @param version the source version number
         * @return this builder for method chaining
         */
        @NotNull
        default MigrationRequestBuilder from(final int version) {
            return from(new DataVersion(version));
        }

        /**
         * Specifies the target data version for the migration.
         *
         * <p>This is the version the data should be migrated to. All fixes
         * between the source version (exclusive) and target version (inclusive)
         * will be applied.</p>
         *
         * @param version the target data version, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if version is {@code null}
         */
        @NotNull
        MigrationRequestBuilder to(@NotNull DataVersion version);

        /**
         * Specifies the target data version by integer value.
         *
         * <p>This is a convenience method equivalent to
         * {@code to(new DataVersion(version))}.</p>
         *
         * @param version the target version number
         * @return this builder for method chaining
         */
        @NotNull
        default MigrationRequestBuilder to(final int version) {
            return to(new DataVersion(version));
        }

        /**
         * Configures the migration to target the latest version of the selected domain.
         *
         * <p>This is useful when you always want to migrate to the current version
         * without hardcoding a specific version number. The actual target version
         * is resolved at execution time.</p>
         *
         * <p><strong>Note:</strong> Calling this method clears any previously set
         * explicit target version from {@link #to(DataVersion)}.</p>
         *
         * @return this builder for method chaining
         */
        @NotNull
        MigrationRequestBuilder toLatest();

        /**
         * Specifies which domain's DataFixer to use for the migration.
         *
         * <p>In multi-domain setups, this allows selecting the appropriate
         * DataFixer for different data types. If not called, the default
         * domain ("default") is used.</p>
         *
         * @param domain the domain name, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if domain is {@code null}
         * @see de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry
         */
        @NotNull
        MigrationRequestBuilder usingDomain(@NotNull String domain);

        /**
         * Specifies custom DynamicOps to use for the migration.
         *
         * <p>This allows overriding the default DynamicOps implementation for
         * special serialization requirements. If not called, the auto-configured
         * DynamicOps (GsonOps or JacksonJsonOps) will be used.</p>
         *
         * @param ops the dynamic ops implementation, must not be {@code null}
         * @param <T> the underlying data type of the DynamicOps
         * @return this builder for method chaining
         * @throws NullPointerException if ops is {@code null}
         */
        @NotNull
        <T> MigrationRequestBuilder withOps(@NotNull DynamicOps<T> ops);

        /**
         * Executes the configured migration synchronously.
         *
         * <p>This method blocks until the migration completes. The result contains
         * either the migrated data (on success) or error information (on failure).
         * This method never throws exceptions for migration errors; instead, errors
         * are captured in the {@link MigrationResult}.</p>
         *
         * <p><b>Validation</b></p>
         * <p>Before execution, this method validates that all required configuration
         * has been provided. It will throw {@link IllegalStateException} if:</p>
         * <ul>
         *   <li>Source version was not specified</li>
         *   <li>Target version was not specified (neither explicit nor "latest")</li>
         *   <li>The specified domain does not exist</li>
         * </ul>
         *
         * @return the migration result, never {@code null}
         * @throws IllegalStateException    if required configuration is missing
         * @throws IllegalArgumentException if the specified domain does not exist
         */
        @NotNull
        MigrationResult execute();

        /**
         * Executes the configured migration asynchronously.
         *
         * <p>This method returns immediately with a {@link CompletableFuture} that
         * will complete when the migration finishes. The migration runs on a
         * background thread (typically from the common ForkJoinPool).</p>
         *
         * <p><b>Error Handling</b></p>
         * <p>Migration errors are captured in the {@link MigrationResult}, not as
         * exceptions in the future. However, validation errors (missing configuration)
         * will cause the future to complete exceptionally.</p>
         *
         * <p><b>Example</b></p>
         * <pre>{@code
         * migrationService.migrate(data)
         *     .from(100)
         *     .to(200)
         *     .executeAsync()
         *     .thenAccept(result -> {
         *         if (result.isSuccess()) {
         *             processData(result.getData());
         *         } else {
         *             handleError(result.getError().orElse(null));
         *         }
         *     });
         * }</pre>
         *
         * @return a future containing the migration result, never {@code null}
         * @throws IllegalStateException    if required configuration is missing
         * @throws IllegalArgumentException if the specified domain does not exist
         */
        @NotNull
        CompletableFuture<MigrationResult> executeAsync();
    }
}
