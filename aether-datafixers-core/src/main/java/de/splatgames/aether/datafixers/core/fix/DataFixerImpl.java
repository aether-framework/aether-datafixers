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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.exception.FixException;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;

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
        Preconditions.checkNotNull(currentVersion, "DataVersion currentVersion must not be null");
        Preconditions.checkNotNull(registry, "DataFixRegistry registry must not be null");
        Preconditions.checkNotNull(defaultContext, "DataFixerContext defaultContext must not be null");

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
        Preconditions.checkNotNull(type, "TypeReference type must not be null");
        Preconditions.checkNotNull(input, "Dynamic<T> input must not be null");
        Preconditions.checkNotNull(fromVersion, "DataVersion fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "DataVersion toVersion must not be null");
        Preconditions.checkNotNull(ctx, "DataFixerContext ctx must not be null");

        Preconditions.checkArgument(fromVersion.compareTo(toVersion) <= 0, "fromVersion must be <= toVersion");
        Preconditions.checkArgument(toVersion.compareTo(this.currentVersion) <= 0, "toVersion must be <= currentVersion");

        if (fromVersion.compareTo(toVersion) == 0) {
            return input;
        }

        final List<DataFix<?>> fixes = this.registry.getFixes(type, fromVersion, toVersion);

        @SuppressWarnings("unchecked")
        Dynamic<Object> current = (Dynamic<Object>) input;

        for (final DataFix<?> fix : fixes) {
            Preconditions.checkNotNull(fix, "DataFix<?> fix must not be null");

            Preconditions.checkArgument(
                    fix.fromVersion().compareTo(fix.toVersion()) <= 0,
                    "fix.fromVersion must be <= fix.toVersion"
            );

            if (fix.fromVersion().compareTo(fromVersion) < 0) {
                continue;
            }
            if (fix.toVersion().compareTo(toVersion) > 0) {
                continue;
            }

            @SuppressWarnings("unchecked")
            final DataFix<Object> untypedFix = (DataFix<Object>) fix;

            try {
                current = untypedFix.apply(type, current, ctx);
                Preconditions.checkNotNull(current, "Fix '%s' returned null".formatted(fix.name()));
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

        return result;
    }

}
