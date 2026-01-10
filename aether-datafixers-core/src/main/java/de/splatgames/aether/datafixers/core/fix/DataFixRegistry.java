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
import de.splatgames.aether.datafixers.api.fix.DataFix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Internal registry for storing and retrieving {@link DataFix} instances.
 *
 * <p>{@code DataFixRegistry} organizes fixes by type reference and version,
 * allowing efficient retrieval of applicable fixes for a given version range.
 * Uses a {@link TreeMap} internally for version-ordered storage.</p>
 *
 * <h2>Storage Structure</h2>
 * <p>Fixes are stored in a nested map structure:</p>
 * <pre>
 * TypeReference -> DataVersion -> List&lt;DataFix&gt;
 * </pre>
 * <p>This allows multiple fixes per version and efficient range queries.</p>
 *
 * <h2>Usage</h2>
 * <p>This class is typically used internally by {@link DataFixerBuilder} and
 * {@link DataFixerImpl}. Direct usage is rarely needed.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataFixerBuilder
 * @since 0.1.0
 */
public final class DataFixRegistry {

    private Map<TypeReference, NavigableMap<DataVersion, List<DataFix<?>>>> fixesByType = new HashMap<>();
    private volatile boolean frozen = false;

    /**
     * Registers a fix for a type at its from-version.
     *
     * <p>Validates fix invariants at registration time to avoid repeated checks during migration.</p>
     *
     * @param type the type reference this fix applies to, must not be {@code null}
     * @param fix  the data fix to register, must not be {@code null}
     * @throws NullPointerException     if type or fix is {@code null}
     * @throws IllegalStateException    if this registry is frozen
     * @throws IllegalArgumentException if fix.fromVersion &gt; fix.toVersion
     */
    public void register(@NotNull final TypeReference type, @NotNull final DataFix<?> fix) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fix, "fix must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");
        // Validate fix version ordering once at registration (avoids repeated checks during migration)
        Preconditions.checkArgument(
                fix.fromVersion().compareTo(fix.toVersion()) <= 0,
                "fix.fromVersion must be <= fix.toVersion for fix '%s'",
                fix.name()
        );

        this.fixesByType
                .computeIfAbsent(type, k -> new TreeMap<>())
                .computeIfAbsent(fix.fromVersion(), k -> new ArrayList<>())
                .add(fix);
    }

    /**
     * Gets all fixes for a type at a specific version.
     *
     * @param type        the type reference to look up, must not be {@code null}
     * @param fromVersion the exact version to get fixes for, must not be {@code null}
     * @return an immutable list of fixes, never {@code null}
     * @throws NullPointerException if type or fromVersion is {@code null}
     */
    @NotNull
    public List<DataFix<?>> getStepFixes(@NotNull final TypeReference type, @NotNull final DataVersion fromVersion) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");

        final NavigableMap<DataVersion, List<DataFix<?>>> fixesByVersion = this.fixesByType.get(type);
        if (fixesByVersion == null) {
            return List.of();
        }

        final List<DataFix<?>> fixes = fixesByVersion.get(fromVersion);
        return fixes != null ? List.copyOf(fixes) : List.of();
    }

    /**
     * Gets all fixes for a type within a version range.
     *
     * <p>Returns fixes where the fix's {@link DataFix#fromVersion()} falls within
     * the specified range (both bounds inclusive). The returned list is ordered
     * by version.</p>
     *
     * <p><b>Note:</b> This method uses inclusive bounds on both sides, unlike
     * {@link #hasFixesInRange(TypeReference, DataVersion, DataVersion)} which uses
     * exclusive start. This is intentional: when applying fixes from v1 to v3,
     * you want fixes starting at v1, v2, and v3.</p>
     *
     * @param type          the type reference to look up, must not be {@code null}
     * @param fromInclusive the start version (inclusive), must not be {@code null}
     * @param toInclusive   the end version (inclusive), must not be {@code null}
     * @return an immutable list of fixes in version order, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public List<DataFix<?>> getFixes(
            @NotNull final TypeReference type,
            @NotNull final DataVersion fromInclusive,
            @NotNull final DataVersion toInclusive
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fromInclusive, "fromInclusive must not be null");
        Preconditions.checkNotNull(toInclusive, "toInclusive must not be null");

        final NavigableMap<DataVersion, List<DataFix<?>>> fixesByVersion = this.fixesByType.get(type);
        if (fixesByVersion == null) {
            return List.of();
        }

        final var rangeValues = fixesByVersion.subMap(fromInclusive, true, toInclusive, true).values();
        if (rangeValues.isEmpty()) {
            return List.of();
        }

        // Pre-calculate total size to avoid ArrayList resizing
        int totalSize = 0;
        for (List<DataFix<?>> fixes : rangeValues) {
            totalSize += fixes.size();
        }

        if (totalSize == 0) {
            return List.of();
        }

        // Pre-allocate list with exact capacity
        final List<DataFix<?>> result = new ArrayList<>(totalSize);
        for (List<DataFix<?>> fixes : rangeValues) {
            result.addAll(fixes);
        }

        // Use unmodifiableList instead of copyOf to avoid second copy
        return Collections.unmodifiableList(result);
    }

    /**
     * Checks if any fixes exist in a version range (exclusive start).
     *
     * <p>Returns true if any fixes exist where the fix's {@link DataFix#fromVersion()}
     * falls in the range (fromExclusive, toInclusive]. This is useful for checking
     * if updates are needed: when at version 1, you want to know if there are
     * fixes at version 2 or later, not at version 1 itself.</p>
     *
     * <p><b>Note:</b> This method uses exclusive start, unlike
     * {@link #getFixes(TypeReference, DataVersion, DataVersion)} which uses
     * inclusive bounds on both sides. This semantic difference is intentional
     * for the different use cases.</p>
     *
     * @param type          the type reference to check, must not be {@code null}
     * @param fromExclusive the start version (exclusive), must not be {@code null}
     * @param toInclusive   the end version (inclusive), must not be {@code null}
     * @return {@code true} if fixes exist in the range, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean hasFixesInRange(
            @NotNull final TypeReference type,
            @NotNull final DataVersion fromExclusive,
            @NotNull final DataVersion toInclusive
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fromExclusive, "fromExclusive must not be null");
        Preconditions.checkNotNull(toInclusive, "toInclusive must not be null");

        final NavigableMap<DataVersion, List<DataFix<?>>> fixesByVersion = this.fixesByType.get(type);
        if (fixesByVersion == null) {
            return false;
        }

        return !fixesByVersion.subMap(fromExclusive, false, toInclusive, true).isEmpty();
    }

    /**
     * Freezes this registry, making it immutable.
     *
     * <p>After freezing, any attempt to modify the registry via {@link #register(TypeReference, DataFix)}
     * will throw an {@link IllegalStateException}.</p>
     *
     * <p>This method is idempotent - calling it multiple times has no effect after the first call.</p>
     */
    public void freeze() {
        if (!this.frozen) {
            // Create an immutable deep copy
            final Map<TypeReference, NavigableMap<DataVersion, List<DataFix<?>>>> immutableCopy = new HashMap<>();
            for (Map.Entry<TypeReference, NavigableMap<DataVersion, List<DataFix<?>>>> entry : this.fixesByType.entrySet()) {
                final NavigableMap<DataVersion, List<DataFix<?>>> immutableInner = new TreeMap<>();
                for (Map.Entry<DataVersion, List<DataFix<?>>> innerEntry : entry.getValue().entrySet()) {
                    immutableInner.put(innerEntry.getKey(), List.copyOf(innerEntry.getValue()));
                }
                immutableCopy.put(entry.getKey(), Collections.unmodifiableNavigableMap(immutableInner));
            }
            this.fixesByType = Collections.unmodifiableMap(immutableCopy);
            this.frozen = true;
        }
    }

    /**
     * Returns whether this registry is frozen (immutable).
     *
     * @return {@code true} if frozen, {@code false} if still mutable
     */
    public boolean isFrozen() {
        return this.frozen;
    }
}
