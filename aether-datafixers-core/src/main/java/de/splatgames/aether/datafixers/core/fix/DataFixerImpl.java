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

package de.splatgames.aether.datafixers.core.fix;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.exception.FixException;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Default implementation of {@link DataFixer}.
 *
 * <p>{@code DataFixerImpl} applies registered {@link DataFix} instances to migrate
 * data between versions. It retrieves applicable fixes from a {@link DataFixRegistry}
 * and applies them in sequence.</p>
 *
 * <h2>Fix Application</h2>
 * <p>When updating data, this implementation:</p>
 * <ol>
 *   <li>Validates that the version range is valid</li>
 *   <li>Retrieves all fixes applicable to the type and version range</li>
 *   <li>Filters fixes to only those within the specified range</li>
 *   <li>Applies each fix in sequence, passing results through</li>
 * </ol>
 *
 * <h2>Creation</h2>
 * <p>Instances are typically created via {@link DataFixerBuilder#build()}.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe if the underlying registry and fixes are thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixer
 * @see DataFixerBuilder
 * @see DataFixRegistry
 * @since 0.1.0
 */
public final class DataFixerImpl implements DataFixer {

    private final DataVersion currentVersion;
    private final DataFixRegistry registry;
    private final DataFixerContext defaultContext;

    /**
     * Creates a new data fixer implementation.
     *
     * @param currentVersion the current (latest) data version, must not be {@code null}
     * @param registry       the fix registry to retrieve fixes from, must not be {@code null}
     * @param defaultContext the default context for logging, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public DataFixerImpl(
            @NotNull final DataVersion currentVersion,
            @NotNull final DataFixRegistry registry,
            @NotNull final DataFixerContext defaultContext
    ) {
        Preconditions.checkNotNull(currentVersion, "currentVersion must not be null");
        Preconditions.checkNotNull(registry, "registry must not be null");
        Preconditions.checkNotNull(defaultContext, "defaultContext must not be null");

        this.currentVersion = currentVersion;
        this.registry = registry;
        this.defaultContext = defaultContext;
    }

    @Override
    @NotNull
    public DataVersion currentVersion() {
        return this.currentVersion;
    }

    @Override
    @NotNull
    public <T> Dynamic<T> update(
            @NotNull final TypeReference type,
            @NotNull final Dynamic<T> input,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        return this.update(type, input, fromVersion, toVersion, this.defaultContext);
    }

    @Override
    @NotNull
    public <T> Dynamic<T> update(
            @NotNull final TypeReference type,
            @NotNull final Dynamic<T> input,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion,
            @NotNull final DataFixerContext ctx
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkNotNull(ctx, "ctx must not be null");

        Preconditions.checkArgument(fromVersion.compareTo(toVersion) <= 0, "fromVersion must be <= toVersion");
        Preconditions.checkArgument(toVersion.compareTo(this.currentVersion) <= 0, "toVersion must be <= currentVersion");

        if (fromVersion.compareTo(toVersion) == 0) {
            return input;
        }

        // Check if diagnostics are enabled
        final DiagnosticContext diagCtx = (ctx instanceof DiagnosticContext dc && dc.isDiagnosticEnabled())
                ? dc : null;

        if (diagCtx != null) {
            diagCtx.reportBuilder().startMigration(type, fromVersion, toVersion);
            if (diagCtx.options().captureSnapshots()) {
                diagCtx.reportBuilder().setInputSnapshot(serializeSnapshot(input, diagCtx));
            }
        }

        final List<DataFix<?>> fixes = this.registry.getFixes(type, fromVersion, toVersion);

        @SuppressWarnings("unchecked")
        Dynamic<Object> current = (Dynamic<Object>) input;

        for (final DataFix<?> fix : fixes) {
            // Note: Null check and version ordering are validated at registration time.
            // The fromVersion range check is handled by DataFixRegistry.getFixes().
            // Only toVersion check is needed here to avoid applying fixes past target.
            if (fix.toVersion().compareTo(toVersion) > 0) {
                continue;
            }

            @SuppressWarnings("unchecked")
            final DataFix<Object> untypedFix = (DataFix<Object>) fix;

            // Capture diagnostic events if enabled
            final Instant fixStart = diagCtx != null ? Instant.now() : null;
            String beforeSnapshot = null;

            if (diagCtx != null) {
                diagCtx.reportBuilder().startFix(fix);
                if (diagCtx.options().captureSnapshots()) {
                    beforeSnapshot = serializeSnapshot(current, diagCtx);
                    diagCtx.reportBuilder().setFixBeforeSnapshot(beforeSnapshot);
                }
            }

            try {
                current = untypedFix.apply(type, current, ctx);
                Preconditions.checkNotNull(current, "Fix '%s' returned null".formatted(fix.name()));

                if (diagCtx != null) {
                    final Duration duration = Duration.between(fixStart, Instant.now());
                    final String afterSnapshot = diagCtx.options().captureSnapshots()
                            ? serializeSnapshot(current, diagCtx) : null;
                    diagCtx.reportBuilder().endFix(fix, duration, afterSnapshot);
                }
            } catch (final FixException e) {
                throw e; // Re-throw FixException as-is
            } catch (final Exception e) {
                throw new FixException(
                        "Fix '" + fix.name() + "' failed: " + e.getMessage(),
                        fix.name(),
                        fix.fromVersion(),
                        fix.toVersion(),
                        type,
                        e
                );
            }
        }

        @SuppressWarnings("unchecked")
        final Dynamic<T> result = (Dynamic<T>) current;

        if (diagCtx != null && diagCtx.options().captureSnapshots()) {
            diagCtx.reportBuilder().setOutputSnapshot(serializeSnapshot(result, diagCtx));
        }

        return result;
    }

    /**
     * Serializes a Dynamic value to a snapshot string for diagnostics.
     *
     * @param dynamic the dynamic value to serialize
     * @param ctx     the diagnostic context for options
     * @param <T>     the dynamic type
     * @return the serialized snapshot string
     */
    @Nullable
    private <T> String serializeSnapshot(
            @NotNull final Dynamic<T> dynamic,
            @NotNull final DiagnosticContext ctx
    ) {
        Preconditions.checkNotNull(dynamic, "dynamic must not be null");
        Preconditions.checkNotNull(ctx, "ctx must not be null");
        String snapshot = dynamic.value().toString();

        final int maxLength = ctx.options().maxSnapshotLength();
        if (maxLength > 0 && snapshot.length() > maxLength) {
            snapshot = snapshot.substring(0, maxLength) + "... (truncated)";
        }

        return snapshot;
    }

}
