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

package de.splatgames.aether.datafixers.cli.util;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import org.jetbrains.annotations.NotNull;

/**
 * Extracts data version from a Dynamic value.
 *
 * <p>Supports nested field paths using dot notation (e.g., "meta.version").</p>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public final class VersionExtractor {

    private VersionExtractor() {
        // Utility class
    }

    /**
     * Extracts the data version from the specified field path.
     *
     * @param data      the data to extract from
     * @param ops       the DynamicOps for the data format
     * @param fieldPath the field path (supports dot notation for nested fields)
     * @param <T>       the data type
     * @return the extracted DataVersion
     * @throws IllegalArgumentException if the version field is missing or invalid
     */
    @NotNull
    public static <T> DataVersion extract(
            @NotNull final T data,
            @NotNull final DynamicOps<T> ops,
            @NotNull final String fieldPath
    ) {
        // Validate field path
        if (fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Field path cannot be empty");
        }
        if (fieldPath.startsWith(".") || fieldPath.endsWith(".") || fieldPath.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid field path format: " + fieldPath);
        }

        Dynamic<T> dynamic = new Dynamic<>(ops, data);

        // Support nested paths like "meta.version"
        final String[] parts = fieldPath.split("\\.");

        for (final String part : parts) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid field path format: " + fieldPath);
            }
            final Dynamic<T> next = dynamic.get(part);
            if (next == null || next.value() == null) {
                throw new IllegalArgumentException(
                        "Version field not found: " + fieldPath);
            }
            dynamic = next;
        }

        final Dynamic<T> finalDynamic = dynamic;
        return finalDynamic.asInt()
                .result()
                .map(DataVersion::new)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version field is not a valid integer: " + fieldPath
                                + " (value: " + finalDynamic.value() + ")"));
    }
}
