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
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * Immutable result object representing the outcome of a data migration operation.
 *
 * <p>This class encapsulates all information about a completed migration, whether
 * successful or failed. It follows the Result pattern, providing a type-safe way
 * to handle migration outcomes without relying on exceptions for control flow.</p>
 *
 * <h2>Key Information</h2>
 * <p>A MigrationResult contains:</p>
 * <ul>
 *   <li><strong>Success/Failure Status:</strong> Whether the migration completed successfully</li>
 *   <li><strong>Migrated Data:</strong> The transformed data (on success only)</li>
 *   <li><strong>Version Information:</strong> Source and target versions</li>
 *   <li><strong>Domain:</strong> Which DataFixer domain was used</li>
 *   <li><strong>Duration:</strong> How long the migration took</li>
 *   <li><strong>Error Details:</strong> The exception that caused failure (on failure only)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Success Handling</h3>
 * <pre>{@code
 * MigrationResult result = migrationService
 *     .migrate(data)
 *     .from(100)
 *     .to(200)
 *     .execute();
 *
 * if (result.isSuccess()) {
 *     TaggedDynamic<?> migratedData = result.getData();
 *     // Process the migrated data
 * }
 * }</pre>
 *
 * <h3>Comprehensive Error Handling</h3>
 * <pre>{@code
 * MigrationResult result = migrationService
 *     .migrate(data)
 *     .from(100)
 *     .to(200)
 *     .execute();
 *
 * if (result.isFailure()) {
 *     log.error("Migration from v{} to v{} failed in domain '{}' after {}ms",
 *         result.getFromVersion().getVersion(),
 *         result.getToVersion().getVersion(),
 *         result.getDomain(),
 *         result.getDuration().toMillis());
 *
 *     result.getError().ifPresent(error -> {
 *         log.error("Cause: {}", error.getMessage(), error);
 *     });
 *
 *     throw new MigrationException("Data migration failed", result.getError().orElse(null));
 * }
 * }</pre>
 *
 * <h3>Using Optional API</h3>
 * <pre>{@code
 * MigrationResult result = migrationService.migrate(data).from(100).to(200).execute();
 *
 * TaggedDynamic<?> migratedData = result.getDataOptional()
 *     .orElseThrow(() -> new IllegalStateException(
 *         "Migration failed: " + result.getError().map(Throwable::getMessage).orElse("Unknown")
 *     ));
 * }</pre>
 *
 * <h3>Metrics and Monitoring</h3>
 * <pre>{@code
 * MigrationResult result = migrationService.migrate(data).from(100).to(200).execute();
 *
 * // Log performance metrics
 * log.info("Migration completed: success={}, duration={}ms, versionSpan={}",
 *     result.isSuccess(),
 *     result.getDuration().toMillis(),
 *     result.getVersionSpan());
 *
 * // Track in custom metrics
 * metrics.recordMigrationDuration(result.getDomain(), result.getDuration());
 * if (result.isFailure()) {
 *     metrics.incrementFailureCount(result.getDomain());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and therefore thread-safe. Instances can be safely
 * shared between threads without synchronization.</p>
 *
 * <h2>Equality and Hashing</h2>
 * <p>This class does not override {@code equals()} and {@code hashCode()}.
 * Each instance is unique and identity-based comparison is used.</p>
 *
 * @author Erik Pförtner
 * @see MigrationService
 * @see MigrationService.MigrationRequestBuilder
 * @since 0.4.0
 */
public final class MigrationResult {

    /**
     * Indicates whether the migration completed successfully.
     */
    private final boolean success;

    /**
     * The migrated data. Only present when {@link #success} is {@code true}.
     */
    @Nullable
    private final TaggedDynamic data;

    /**
     * The source version from which data was migrated.
     */
    @NotNull
    private final DataVersion fromVersion;

    /**
     * The target version to which data was migrated.
     */
    @NotNull
    private final DataVersion toVersion;

    /**
     * The domain name that was used for the migration.
     */
    @NotNull
    private final String domain;

    /**
     * The wall-clock duration of the migration operation.
     */
    @NotNull
    private final Duration duration;

    /**
     * The error that caused the migration to fail. Only present when
     * {@link #success} is {@code false}.
     */
    @Nullable
    private final Throwable error;

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param success     whether the migration succeeded
     * @param data        the migrated data (null on failure)
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param domain      the domain name
     * @param duration    the migration duration
     * @param error       the error (null on success)
     */
    private MigrationResult(
            final boolean success,
            @Nullable final TaggedDynamic data,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion,
            @NotNull final String domain,
            @NotNull final Duration duration,
            @Nullable final Throwable error
    ) {
        this.success = success;
        this.data = data;
        this.fromVersion = Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        this.toVersion = Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        this.domain = Preconditions.checkNotNull(domain, "domain must not be null");
        this.duration = Preconditions.checkNotNull(duration, "duration must not be null");
        this.error = error;
    }

    /**
     * Creates a successful migration result.
     *
     * <p>Use this factory method when a migration completes without errors.
     * The migrated data must be provided and will be accessible via {@link #getData()}.</p>
     *
     * @param data        the migrated data, must not be {@code null}
     * @param fromVersion the source version, must not be {@code null}
     * @param toVersion   the target version, must not be {@code null}
     * @param domain      the domain name used, must not be {@code null}
     * @param duration    the migration duration, must not be {@code null}
     * @return a success result containing the migrated data
     * @throws NullPointerException if any parameter is {@code null}
     */
    @NotNull
    public static MigrationResult success(
            @NotNull final TaggedDynamic data,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion,
            @NotNull final String domain,
            @NotNull final Duration duration
    ) {
        Preconditions.checkNotNull(data, "data must not be null");
        return new MigrationResult(true, data, fromVersion, toVersion, domain, duration, null);
    }

    /**
     * Creates a failed migration result.
     *
     * <p>Use this factory method when a migration fails due to an error.
     * The error that caused the failure must be provided and will be accessible
     * via {@link #getError()}.</p>
     *
     * @param fromVersion the source version, must not be {@code null}
     * @param toVersion   the target version, must not be {@code null}
     * @param domain      the domain name used, must not be {@code null}
     * @param duration    the migration duration, must not be {@code null}
     * @param error       the error that caused the failure, must not be {@code null}
     * @return a failure result containing the error information
     * @throws NullPointerException if any parameter is {@code null}
     */
    @NotNull
    public static MigrationResult failure(
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion,
            @NotNull final String domain,
            @NotNull final Duration duration,
            @NotNull final Throwable error
    ) {
        Preconditions.checkNotNull(error, "error must not be null");
        return new MigrationResult(false, null, fromVersion, toVersion, domain, duration, error);
    }

    /**
     * Returns whether the migration completed successfully.
     *
     * <p>A successful migration means:</p>
     * <ul>
     *   <li>All data fixes were applied without errors</li>
     *   <li>The migrated data is available via {@link #getData()}</li>
     *   <li>No error is present in {@link #getError()}</li>
     * </ul>
     *
     * @return {@code true} if the migration was successful, {@code false} otherwise
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Returns whether the migration failed.
     *
     * <p>This is the logical inverse of {@link #isSuccess()}. A failed migration means:</p>
     * <ul>
     *   <li>An error occurred during migration</li>
     *   <li>The error is available via {@link #getError()}</li>
     *   <li>No migrated data is available</li>
     * </ul>
     *
     * @return {@code true} if the migration failed, {@code false} otherwise
     */
    public boolean isFailure() {
        return !this.success;
    }

    /**
     * Returns the migrated data.
     *
     * <p>This method should only be called after verifying the migration was successful
     * using {@link #isSuccess()}. Calling this method on a failed result will throw
     * an {@link IllegalStateException} with details about the failure.</p>
     *
     * <p>For safer access, consider using {@link #getDataOptional()} instead.</p>
     *
     * @return the migrated TaggedDynamic data, never {@code null} on success
     * @throws IllegalStateException if the migration failed (no data available)
     * @see #getDataOptional()
     * @see #isSuccess()
     */
    @NotNull
    public TaggedDynamic getData() {
        if (!this.success || this.data == null) {
            throw new IllegalStateException(
                    "Cannot get data from failed migration. Error: " +
                    (this.error != null ? this.error.getMessage() : "unknown")
            );
        }
        return this.data;
    }

    /**
     * Returns the migrated data wrapped in an Optional.
     *
     * <p>This provides a null-safe way to access the migrated data without
     * risking an exception. The Optional will be empty for failed migrations.</p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * result.getDataOptional()
     *     .ifPresent(data -> processData(data));
     *
     * // Or with default handling
     * TaggedDynamic<?> data = result.getDataOptional()
     *     .orElseGet(() -> createDefaultData());
     * }</pre>
     *
     * @return an Optional containing the migrated data if successful, empty otherwise
     */
    @NotNull
    public Optional<TaggedDynamic> getDataOptional() {
        return Optional.ofNullable(this.data);
    }

    /**
     * Returns the source version from which the data was migrated.
     *
     * <p>This is the version that was specified when configuring the migration
     * via {@link MigrationService.MigrationRequestBuilder#from(DataVersion)}.</p>
     *
     * @return the source data version, never {@code null}
     */
    @NotNull
    public DataVersion getFromVersion() {
        return this.fromVersion;
    }

    /**
     * Returns the target version to which the data was migrated.
     *
     * <p>This is the version that was specified when configuring the migration
     * via {@link MigrationService.MigrationRequestBuilder#to(DataVersion)} or
     * resolved automatically when using
     * {@link MigrationService.MigrationRequestBuilder#toLatest()}.</p>
     *
     * @return the target data version, never {@code null}
     */
    @NotNull
    public DataVersion getToVersion() {
        return this.toVersion;
    }

    /**
     * Returns the domain name that was used for the migration.
     *
     * <p>In multi-domain setups, this identifies which DataFixer was used.
     * For single-domain setups, this will typically be "default".</p>
     *
     * @return the domain name, never {@code null}
     */
    @NotNull
    public String getDomain() {
        return this.domain;
    }

    /**
     * Returns the wall-clock duration of the migration operation.
     *
     * <p>This duration includes the time spent applying all data fixes from
     * the source version to the target version. It can be used for performance
     * monitoring and identifying slow migrations.</p>
     *
     * @return the migration duration, never {@code null}
     */
    @NotNull
    public Duration getDuration() {
        return this.duration;
    }

    /**
     * Returns the error that caused the migration to fail, if any.
     *
     * <p>The Optional will contain the error for failed migrations and will
     * be empty for successful migrations. Use this for error logging, metrics,
     * or exception chaining.</p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * result.getError().ifPresent(error -> {
     *     log.error("Migration failed: {}", error.getMessage(), error);
     *     alertService.notifyMigrationFailure(error);
     * });
     * }</pre>
     *
     * @return an Optional containing the error if the migration failed, empty otherwise
     */
    @NotNull
    public Optional<Throwable> getError() {
        return Optional.ofNullable(this.error);
    }

    /**
     * Returns the version span (absolute difference between target and source versions).
     *
     * <p>The version span indicates how many version increments the migration covered.
     * Larger spans typically indicate more fixes to apply and potentially longer
     * migration times. This metric is useful for:</p>
     * <ul>
     *   <li>Performance analysis (correlating span with duration)</li>
     *   <li>Alerting on large migrations</li>
     *   <li>Metrics collection for migration complexity</li>
     * </ul>
     *
     * @return the absolute version span, always non-negative
     */
    public int getVersionSpan() {
        return Math.abs(this.toVersion.getVersion() - this.fromVersion.getVersion());
    }

    /**
     * Returns a human-readable string representation of this result.
     *
     * <p>The string includes the success status, version information, domain,
     * duration, and error message (if applicable). This is useful for logging
     * and debugging.</p>
     *
     * @return a string representation of this migration result
     */
    @Override
    public String toString() {
        return "MigrationResult{" +
                "success=" + this.success +
                ", fromVersion=" + this.fromVersion.getVersion() +
                ", toVersion=" + this.toVersion.getVersion() +
                ", domain='" + this.domain + '\'' +
                ", duration=" + this.duration.toMillis() + "ms" +
                (this.error != null ? ", error=" + this.error.getMessage() : "") +
                '}';
    }
}
