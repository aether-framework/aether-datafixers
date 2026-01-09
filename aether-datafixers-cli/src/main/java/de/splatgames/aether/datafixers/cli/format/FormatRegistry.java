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

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Central registry for {@link FormatHandler} implementations.
 *
 * <p>This registry provides a central access point for all available format handlers
 * in the CLI. It combines built-in handlers with handlers discovered via the Java
 * {@link ServiceLoader} mechanism.</p>
 *
 * <h2>Built-in Handlers</h2>
 * <p>The following handlers are registered by default (in order):</p>
 * <ol>
 *   <li>{@link JsonGsonFormatHandler} - JSON using Google Gson ({@code json-gson})</li>
 *   <li>{@link JsonJacksonFormatHandler} - JSON using Jackson ({@code json-jackson})</li>
 * </ol>
 *
 * <h2>Custom Handler Registration</h2>
 * <p>Additional handlers can be registered in two ways:</p>
 * <ol>
 *   <li><b>ServiceLoader:</b> Create a file at
 *       {@code META-INF/services/de.splatgames.aether.datafixers.cli.format.FormatHandler}
 *       containing the fully qualified class name of your implementation</li>
 *   <li><b>Programmatic:</b> Call {@link #register(FormatHandler)} at runtime</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>The registry is initialized at class load time in a thread-safe manner.
 * However, the {@link #register(FormatHandler)} method is not synchronized,
 * so concurrent registration after initialization is not thread-safe.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get handler by format ID
 * FormatHandler<?> handler = FormatRegistry.get("json-gson");
 *
 * // Get handler by file extension
 * FormatHandler<?> handler = FormatRegistry.getByExtension("json");
 *
 * // List all available formats
 * List<String> formats = FormatRegistry.availableFormats();
 * // Returns: ["json-gson", "json-jackson", ...]
 * }</pre>
 *
 * @author Erik Pförtner
 * @see FormatHandler
 * @see ServiceLoader
 * @since 0.3.0
 */
public final class FormatRegistry {

    /**
     * Internal map storing registered format handlers keyed by their format ID.
     *
     * <p>Uses {@link LinkedHashMap} to preserve insertion order, ensuring that
     * built-in handlers are always listed first when iterating.</p>
     */
    private static final Map<String, FormatHandler<?>> HANDLERS = new LinkedHashMap<>();

    /*
     * Static initializer block that registers built-in handlers and discovers
     * additional handlers via ServiceLoader.
     *
     * Order of registration:
     * 1. JsonGsonFormatHandler (json-gson)
     * 2. JsonJacksonFormatHandler (json-jackson)
     * 3. ServiceLoader-discovered handlers (excluding duplicates of built-in IDs)
     */
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

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods and should not be instantiated.</p>
     */
    private FormatRegistry() {
        // Utility class
    }

    /**
     * Registers a format handler with the registry.
     *
     * <p>If a handler with the same format ID already exists, it will be replaced
     * by the new handler. This allows for runtime overriding of format handlers.</p>
     *
     * <p><b>Note:</b> This method is not thread-safe. If handlers need to be
     * registered concurrently after class initialization, external synchronization
     * is required.</p>
     *
     * @param handler the format handler to register, must not be {@code null}
     * @see FormatHandler#formatId()
     */
    public static void register(@NotNull final FormatHandler<?> handler) {
        Preconditions.checkNotNull(handler, "handler must not be null");

        HANDLERS.put(handler.formatId(), handler);
    }

    /**
     * Retrieves a format handler by its unique format ID.
     *
     * <p>Format IDs are case-sensitive strings such as "json-gson" or "json-jackson".
     * The ID is defined by {@link FormatHandler#formatId()}.</p>
     *
     * @param formatId the format identifier to look up, must not be {@code null}
     * @return the format handler with the specified ID, or {@code null} if no
     *         handler is registered with that ID
     * @see #getByExtension(String)
     * @see #availableFormats()
     */
    @Nullable
    public static FormatHandler<?> get(@NotNull final String formatId) {
        Preconditions.checkNotNull(formatId, "formatId must not be null");

        return HANDLERS.get(formatId);
    }

    /**
     * Retrieves a format handler by file extension.
     *
     * <p>This method searches through all registered handlers and returns the
     * first one that supports the given file extension. The search is case-insensitive.</p>
     *
     * <p>If multiple handlers support the same extension (e.g., both json-gson and
     * json-jackson support ".json"), the one registered first is returned.</p>
     *
     * @param extension the file extension without the leading dot (e.g., "json", "yaml"),
     *                  must not be {@code null}
     * @return the first format handler supporting the extension, or {@code null}
     *         if no handler supports it
     * @see FormatHandler#fileExtensions()
     * @see #get(String)
     */
    @Nullable
    public static FormatHandler<?> getByExtension(@NotNull final String extension) {
        Preconditions.checkNotNull(extension, "extension must not be null");

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
     * Returns a list of all registered format IDs.
     *
     * <p>The list is returned in registration order, with built-in handlers first.</p>
     *
     * <p>The returned list is a copy and can be modified without affecting
     * the registry.</p>
     *
     * @return a new list containing all registered format IDs, never {@code null}
     * @see #handlers()
     */
    @NotNull
    public static List<String> availableFormats() {
        return new ArrayList<>(HANDLERS.keySet());
    }

    /**
     * Returns a list of all registered format handlers.
     *
     * <p>The list is returned in registration order, with built-in handlers first.</p>
     *
     * <p>The returned list is a copy and can be modified without affecting
     * the registry.</p>
     *
     * @return a new list containing all registered format handlers, never {@code null}
     * @see #availableFormats()
     */
    @NotNull
    public static List<FormatHandler<?>> handlers() {
        return new ArrayList<>(HANDLERS.values());
    }
}
