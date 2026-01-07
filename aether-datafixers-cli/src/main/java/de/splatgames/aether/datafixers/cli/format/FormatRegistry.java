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

package de.splatgames.aether.datafixers.cli.format;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry for format handlers, discovered via ServiceLoader.
 *
 * <p>This registry provides access to all available {@link FormatHandler}
 * implementations. Built-in handlers (Gson, Jackson) are registered first,
 * followed by any handlers discovered via {@link ServiceLoader}.</p>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public final class FormatRegistry {

    private static final Map<String, FormatHandler<?>> HANDLERS = new LinkedHashMap<>();

    static {
        // Load built-in handlers first
        register(new JsonGsonFormatHandler());
        register(new JsonJacksonFormatHandler());

        // Load SPI handlers (may override built-in handlers)
        final ServiceLoader<FormatHandler> loader = ServiceLoader.load(FormatHandler.class);
        for (final FormatHandler<?> handler : loader) {
            // Skip built-in handlers that might be registered via SPI
            if (!"json-gson".equals(handler.formatId())
                    && !"json-jackson".equals(handler.formatId())) {
                register(handler);
            }
        }
    }

    private FormatRegistry() {
        // Utility class
    }

    /**
     * Registers a format handler.
     *
     * <p>If a handler with the same format ID already exists, it will be replaced.</p>
     *
     * @param handler the handler to register
     */
    public static void register(@NotNull final FormatHandler<?> handler) {
        HANDLERS.put(handler.formatId(), handler);
    }

    /**
     * Gets a format handler by its ID.
     *
     * @param formatId the format identifier
     * @return the format handler, or {@code null} if not found
     */
    @Nullable
    public static FormatHandler<?> get(@NotNull final String formatId) {
        return HANDLERS.get(formatId);
    }

    /**
     * Gets a format handler by file extension.
     *
     * <p>Returns the first handler that supports the given file extension.</p>
     *
     * @param extension the file extension without the leading dot
     * @return the format handler, or {@code null} if no handler supports this extension
     */
    @Nullable
    public static FormatHandler<?> getByExtension(@NotNull final String extension) {
        for (final FormatHandler<?> handler : HANDLERS.values()) {
            for (final String ext : handler.fileExtensions()) {
                if (ext.equalsIgnoreCase(extension)) {
                    return handler;
                }
            }
        }
        return null;
    }

    /**
     * Returns all available format IDs.
     *
     * @return list of format IDs in registration order
     */
    @NotNull
    public static List<String> availableFormats() {
        return new ArrayList<>(HANDLERS.keySet());
    }

    /**
     * Returns all registered format handlers.
     *
     * @return list of format handlers in registration order
     */
    @NotNull
    public static List<FormatHandler<?>> handlers() {
        return new ArrayList<>(HANDLERS.values());
    }
}
