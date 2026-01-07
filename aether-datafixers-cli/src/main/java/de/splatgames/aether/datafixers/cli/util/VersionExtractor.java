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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for extracting data version numbers from Dynamic values.
 *
 * <p>This class provides functionality to locate and extract version information
 * from data structures using configurable field paths. It supports nested fields
 * via dot notation.</p>
 *
 * <h2>Field Path Syntax</h2>
 * <ul>
 *   <li>{@code dataVersion} - Access a top-level field</li>
 *   <li>{@code meta.version} - Access a nested field</li>
 *   <li>{@code data.metadata.schema_version} - Access deeply nested fields</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // JSON: {"dataVersion": 100}
 * DataVersion version = VersionExtractor.extract(jsonElement, GsonOps.INSTANCE, "dataVersion");
 * // version.getVersion() == 100
 *
 * // JSON: {"meta": {"version": 200}}
 * DataVersion version = VersionExtractor.extract(jsonElement, GsonOps.INSTANCE, "meta.version");
 * // version.getVersion() == 200
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>The extractor throws {@link IllegalArgumentException} for:</p>
 * <ul>
 *   <li>Empty field paths</li>
 *   <li>Invalid field path syntax (leading/trailing dots, consecutive dots)</li>
 *   <li>Missing fields in the data structure</li>
 *   <li>Non-integer values in the version field</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @author Erik Pfoertner
 * @see DataVersion
 * @see Dynamic
 * @since 0.3.0
 */
public final class VersionExtractor {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods and should not be instantiated.</p>
     */
    private VersionExtractor() {
        // Utility class
    }

    /**
     * Extracts the data version from a specified field path within data.
     *
     * <p>This method navigates through the data structure following the field path
     * and extracts the integer version value. The field path supports dot notation
     * for accessing nested fields.</p>
     *
     * <h3>Algorithm</h3>
     * <ol>
     *   <li>Validates the field path syntax</li>
     *   <li>Wraps the raw data in a {@link Dynamic}</li>
     *   <li>Splits the field path by dots and traverses each segment</li>
     *   <li>Extracts the final value as an integer</li>
     *   <li>Wraps the integer in a {@link DataVersion}</li>
     * </ol>
     *
     * <h3>Valid Field Paths</h3>
     * <ul>
     *   <li>{@code "version"} - Single field</li>
     *   <li>{@code "meta.version"} - Nested field</li>
     *   <li>{@code "a.b.c.version"} - Deeply nested field</li>
     * </ul>
     *
     * <h3>Invalid Field Paths</h3>
     * <ul>
     *   <li>{@code ""} - Empty</li>
     *   <li>{@code ".version"} - Leading dot</li>
     *   <li>{@code "version."} - Trailing dot</li>
     *   <li>{@code "meta..version"} - Consecutive dots</li>
     * </ul>
     *
     * @param <T>       the underlying data representation type (e.g., JsonElement, JsonNode)
     * @param data      the data to extract the version from, must not be {@code null}
     * @param ops       the {@link DynamicOps} implementation for the data format,
     *                  must not be {@code null}
     * @param fieldPath the dot-separated field path to the version field,
     *                  must not be {@code null} or empty
     * @return the extracted {@link DataVersion}, never {@code null}
     * @throws IllegalArgumentException if the field path is invalid, the field is
     *                                  not found, or the value is not a valid integer
     */
    @NotNull
    public static <T> DataVersion extract(
            @NotNull final T data,
            @NotNull final DynamicOps<T> ops,
            @NotNull final String fieldPath
    ) {
        Preconditions.checkNotNull(data, "data must not be null");
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(fieldPath, "fieldPath must not be null");

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
