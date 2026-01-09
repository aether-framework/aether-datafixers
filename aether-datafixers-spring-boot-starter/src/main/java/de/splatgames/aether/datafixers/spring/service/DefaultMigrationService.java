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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Default implementation of {@link MigrationService}.
 *
 * <p>This service provides a fluent API for executing data migrations with automatic
 * domain resolution, comprehensive metrics recording, and support for both synchronous
 * and asynchronous execution. It integrates seamlessly with Spring's dependency injection
 * and the Aether Datafixers auto-configuration.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Multi-Domain Support:</strong> Automatically resolves DataFixers from the registry</li>
 *   <li><strong>Metrics Integration:</strong> Records success/failure counts, timing, and version spans</li>
 *   <li><strong>Async Support:</strong> Non-blocking migrations using configurable executor</li>
 *   <li><strong>Comprehensive Logging:</strong> Debug and error logging for troubleshooting</li>
 *   <li><strong>Validation:</strong> Validates configuration before execution</li>
 * </ul>
 *
 * <h2>Spring Configuration</h2>
 * <p>This service is typically auto-configured by
 * {@link de.splatgames.aether.datafixers.spring.autoconfigure.MigrationServiceAutoConfiguration}.
 * For manual configuration:</p>
 * <pre>{@code
 * @Configuration
 * public class MigrationConfig {
 *
 *     @Bean
 *     public MigrationService migrationService(
 *             DataFixerRegistry registry,
 *             @Nullable MigrationMetrics metrics) {
 *         return new DefaultMigrationService(registry, metrics);
 *     }
 * }
 * }</pre>
 *
 * <h2>Custom Executor</h2>
 * <p>For applications with specific threading requirements, a custom executor can be provided:</p>
 * <pre>{@code
 * @Bean
 * public MigrationService migrationService(
 *         DataFixerRegistry registry,
 *         @Nullable MigrationMetrics metrics,
 *         @Qualifier("migrationExecutor") Executor executor) {
 *     return new DefaultMigrationService(registry, metrics, executor);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Multiple threads can safely call {@link #migrate(TaggedDynamic)}
 * concurrently. Each call returns a new builder instance that is thread-local.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Migration errors are captured in the {@link MigrationResult} rather than thrown as exceptions.
 * This allows for consistent handling of both successful and failed migrations. Validation
 * errors (missing configuration) are thrown as exceptions.</p>
 *
 * @author Erik Pförtner
 * @see MigrationService
 * @see MigrationResult
 * @see DataFixerRegistry
 * @see MigrationMetrics
 * @since 0.4.0
 */
public class DefaultMigrationService implements MigrationService {

    /**
     * Logger for this service. Logs at DEBUG level for successful operations
     * and ERROR level for failures.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMigrationService.class);

    /**
     * Registry containing all available DataFixer instances by domain.
     */
    private final DataFixerRegistry registry;

    /**
     * Optional metrics recorder. May be null if metrics are disabled.
     */
    @Nullable
    private final MigrationMetrics metrics;

    /**
     * Executor used for asynchronous migration operations.
     */
    private final Executor asyncExecutor;

    /**
     * Creates a new DefaultMigrationService with the common ForkJoinPool for async operations.
     *
     * <p>This constructor is suitable for most use cases. The common ForkJoinPool is shared
     * across the application and provides good default behavior for CPU-bound tasks.</p>
     *
     * @param registry the DataFixer registry for looking up domain fixers, must not be {@code null}
     * @param metrics  the metrics recorder for tracking migration statistics, may be {@code null}
     * @throws NullPointerException if registry is {@code null}
     */
    public DefaultMigrationService(
            @NotNull final DataFixerRegistry registry,
            @Nullable final MigrationMetrics metrics
    ) {
        this(registry, metrics, ForkJoinPool.commonPool());
    }

    /**
     * Creates a new DefaultMigrationService with a custom executor for async operations.
     *
     * <p>Use this constructor when you need control over the threading behavior of
     * asynchronous migrations. Common scenarios include:</p>
     * <ul>
     *   <li>Using a dedicated thread pool for migrations</li>
     *   <li>Integrating with Spring's task executors</li>
     *   <li>Applying backpressure or rate limiting</li>
     *   <li>Using virtual threads (Java 21+)</li>
     * </ul>
     *
     * @param registry      the DataFixer registry for looking up domain fixers, must not be {@code null}
     * @param metrics       the metrics recorder for tracking migration statistics, may be {@code null}
     * @param asyncExecutor the executor for async operations, must not be {@code null}
     * @throws NullPointerException if registry or asyncExecutor is {@code null}
     */
    public DefaultMigrationService(
            @NotNull final DataFixerRegistry registry,
            @Nullable final MigrationMetrics metrics,
            @NotNull final Executor asyncExecutor
    ) {
        this.registry = Preconditions.checkNotNull(registry, "registry must not be null");
        this.metrics = metrics;
        this.asyncExecutor = Preconditions.checkNotNull(asyncExecutor, "asyncExecutor must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new {@link DefaultMigrationRequestBuilder} for configuring
     * and executing the migration.</p>
     *
     * @param data the tagged dynamic data to migrate, must not be {@code null}
     * @return a builder for configuring the migration request
     * @throws NullPointerException if data is {@code null}
     */
    @Override
    @NotNull
    public MigrationRequestBuilder migrate(@NotNull final TaggedDynamic data) {
        Preconditions.checkNotNull(data, "data must not be null");
        return new DefaultMigrationRequestBuilder(data);
    }

    /**
     * {@inheritDoc}
     *
     * @return the current data version for the default domain, never {@code null}
     * @throws IllegalArgumentException if no default domain is configured
     */
    @Override
    @NotNull
    public DataVersion getCurrentVersion() {
        return getCurrentVersion(DataFixerRegistry.DEFAULT_DOMAIN);
    }

    /**
     * {@inheritDoc}
     *
     * @param domain the domain name to query, must not be {@code null}
     * @return the current data version for the specified domain, never {@code null}
     * @throws IllegalArgumentException if the domain does not exist
     * @throws NullPointerException     if domain is {@code null}
     */
    @Override
    @NotNull
    public DataVersion getCurrentVersion(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        return this.registry.require(domain).currentVersion();
    }

    /**
     * {@inheritDoc}
     *
     * @param domain the domain name to check, must not be {@code null}
     * @return {@code true} if the domain exists and can be used for migrations,
     *         {@code false} otherwise
     * @throws NullPointerException if domain is {@code null}
     */
    @Override
    public boolean hasDomain(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        return this.registry.contains(domain);
    }

    /**
     * {@inheritDoc}
     *
     * @return an unmodifiable set of domain names, never {@code null}
     */
    @Override
    @NotNull
    public Set<String> getAvailableDomains() {
        return this.registry.getDomains();
    }

    /**
     * Internal implementation of the migration request builder.
     *
     * <p>This builder collects all configuration and executes the migration when
     * {@link #execute()} or {@link #executeAsync()} is called. The builder validates
     * that all required configuration is present before execution.</p>
     *
     * <h2>Builder State</h2>
     * <p>The builder maintains the following state:</p>
     * <ul>
     *   <li>{@code data} - The input data to migrate (set at construction)</li>
     *   <li>{@code fromVersion} - Source version (required)</li>
     *   <li>{@code toVersion} - Target version (set explicitly or via toLatest)</li>
     *   <li>{@code toLatest} - Flag to use the domain's current version</li>
     *   <li>{@code domain} - Domain name (defaults to "default")</li>
     *   <li>{@code ops} - Custom DynamicOps (optional)</li>
     * </ul>
     *
     * <h2>Thread Safety</h2>
     * <p>This builder is NOT thread-safe. Each thread should obtain its own
     * builder instance from {@link #migrate(TaggedDynamic)}.</p>
     *
     * @author Erik Pförtner
     * @since 0.4.0
     */
    private class DefaultMigrationRequestBuilder implements MigrationRequestBuilder {

        /**
         * The input data to be migrated.
         */
        private final TaggedDynamic data;

        /**
         * The source version for the migration. Must be set before execution.
         */
        @Nullable
        private DataVersion fromVersion;

        /**
         * The explicit target version. Either this or {@link #toLatest} must be set.
         */
        @Nullable
        private DataVersion toVersion;

        /**
         * Flag indicating whether to migrate to the domain's current version.
         */
        private boolean toLatest = false;

        /**
         * The domain to use for the migration. Defaults to the default domain.
         */
        private String domain = DataFixerRegistry.DEFAULT_DOMAIN;

        /**
         * Optional custom DynamicOps implementation.
         */
        @Nullable
        private DynamicOps<?> ops;

        /**
         * Creates a new builder for the given input data.
         *
         * @param data the data to migrate, must not be {@code null}
         */
        DefaultMigrationRequestBuilder(@NotNull final TaggedDynamic data) {
            this.data = data;
        }

        /**
         * {@inheritDoc}
         *
         * @param version the source data version, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if version is {@code null}
         */
        @Override
        @NotNull
        public MigrationRequestBuilder from(@NotNull final DataVersion version) {
            this.fromVersion = Preconditions.checkNotNull(version, "version must not be null");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param version the target data version, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if version is {@code null}
         */
        @Override
        @NotNull
        public MigrationRequestBuilder to(@NotNull final DataVersion version) {
            this.toVersion = Preconditions.checkNotNull(version, "version must not be null");
            this.toLatest = false;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @return this builder for method chaining
         */
        @Override
        @NotNull
        public MigrationRequestBuilder toLatest() {
            this.toLatest = true;
            this.toVersion = null;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param domain the domain name, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if domain is {@code null}
         */
        @Override
        @NotNull
        public MigrationRequestBuilder usingDomain(@NotNull final String domain) {
            this.domain = Preconditions.checkNotNull(domain, "domain must not be null");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param ops the dynamic ops implementation, must not be {@code null}
         * @param <T> the underlying data type of the DynamicOps
         * @return this builder for method chaining
         * @throws NullPointerException if ops is {@code null}
         */
        @Override
        @NotNull
        public <T> MigrationRequestBuilder withOps(@NotNull final DynamicOps<T> ops) {
            this.ops = Preconditions.checkNotNull(ops, "ops must not be null");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>This implementation:</p>
         * <ol>
         *   <li>Validates the builder configuration</li>
         *   <li>Resolves the DataFixer from the registry</li>
         *   <li>Resolves the target version if "toLatest" was specified</li>
         *   <li>Executes the migration with timing</li>
         *   <li>Records metrics if available</li>
         *   <li>Returns a success or failure result</li>
         * </ol>
         *
         * @return the migration result, never {@code null}
         * @throws IllegalStateException    if required configuration is missing
         * @throws IllegalArgumentException if the specified domain does not exist
         */
        @Override
        @NotNull
        public MigrationResult execute() {
            validate();

            final AetherDataFixer fixer = DefaultMigrationService.this.registry.require(this.domain);
            final DataVersion from = this.fromVersion;
            final DataVersion to = this.toLatest ? fixer.currentVersion() : this.toVersion;

            assert from != null : "fromVersion must be set";
            assert to != null : "toVersion must be set";

            LOG.debug("Starting migration from v{} to v{} in domain '{}'",
                    from.getVersion(), to.getVersion(), this.domain);

            final Instant start = Instant.now();

            try {
                final TaggedDynamic result = fixer.update(this.data, from, to);
                final Duration duration = Duration.between(start, Instant.now());

                LOG.debug("Migration completed successfully in {}ms", duration.toMillis());

                // Record metrics
                if (DefaultMigrationService.this.metrics != null) {
                    DefaultMigrationService.this.metrics.recordSuccess(
                            this.domain, from.getVersion(), to.getVersion(), duration);
                }

                return MigrationResult.success(result, from, to, this.domain, duration);

            } catch (final Exception e) {
                final Duration duration = Duration.between(start, Instant.now());

                LOG.error("Migration failed from v{} to v{} in domain '{}': {}",
                        from.getVersion(), to.getVersion(), this.domain, e.getMessage(), e);

                // Record metrics
                if (DefaultMigrationService.this.metrics != null) {
                    DefaultMigrationService.this.metrics.recordFailure(
                            this.domain, from.getVersion(), to.getVersion(), duration, e);
                }

                return MigrationResult.failure(from, to, this.domain, duration, e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * <p>Executes the migration asynchronously using the service's configured executor.
         * The returned future will complete with the migration result.</p>
         *
         * @return a future containing the migration result, never {@code null}
         * @throws IllegalStateException    if required configuration is missing
         * @throws IllegalArgumentException if the specified domain does not exist
         */
        @Override
        @NotNull
        public CompletableFuture<MigrationResult> executeAsync() {
            return CompletableFuture.supplyAsync(this::execute, DefaultMigrationService.this.asyncExecutor);
        }

        /**
         * Validates that all required builder configuration has been provided.
         *
         * @throws IllegalStateException    if source or target version is not specified
         * @throws IllegalArgumentException if the specified domain does not exist
         */
        private void validate() {
            if (this.fromVersion == null) {
                throw new IllegalStateException(
                        "Source version not specified. Call .from(version) before execute()."
                );
            }
            if (this.toVersion == null && !this.toLatest) {
                throw new IllegalStateException(
                        "Target version not specified. Call .to(version) or .toLatest() before execute()."
                );
            }
            if (!DefaultMigrationService.this.registry.contains(this.domain)) {
                throw new IllegalArgumentException(
                        "Unknown domain: '" + this.domain + "'. Available domains: "
                                + DefaultMigrationService.this.registry.getDomains()
                );
            }
        }
    }
}
