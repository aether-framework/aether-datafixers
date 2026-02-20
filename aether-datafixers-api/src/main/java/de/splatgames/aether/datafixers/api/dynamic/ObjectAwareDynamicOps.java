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

package de.splatgames.aether.datafixers.api.dynamic;

import de.splatgames.aether.datafixers.api.result.DataResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * An extension of {@link DynamicOps} that supports creating and reading
 * arbitrary Java objects through the dynamic data abstraction layer.
 *
 * <p>This interface follows the <em>Capability Pattern</em>: rather than
 * requiring all {@code DynamicOps} implementations to handle (or explicitly
 * reject) Java object operations, only implementations that actually support
 * these operations implement this sub-interface. Callers use
 * {@link DynamicOps#asObjectAware()} to discover at runtime whether the
 * capability is available.</p>
 *
 * <h2>Security Considerations</h2>
 * <p>Format-bound implementations such as {@code GsonOps}, {@code JacksonJsonOps},
 * and other codec-module implementations should <em>not</em> implement this
 * interface, as arbitrary object serialization/deserialization poses security
 * risks (including Remote Code Execution) with untrusted input.</p>
 *
 * <h2>When to Implement</h2>
 * <p>Implement this interface only when your {@code DynamicOps} works with a value
 * type that can naturally carry arbitrary Java objects. Examples include:</p>
 * <ul>
 *   <li>In-memory test implementations (e.g., {@code TestOps})</li>
 *   <li>Object-graph-based pipelines in trusted environments</li>
 *   <li>Custom implementations that need opaque value passthrough</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DynamicOps<Object> ops = ...;
 *
 * // Check for capability
 * Optional<ObjectAwareDynamicOps<Object>> objectAware = ops.asObjectAware();
 *
 * // Use if available
 * objectAware.ifPresent(oa -> {
 *     Object created = oa.createObject(myPojo);
 *     DataResult<Object> read = oa.getObjectValue(created);
 * });
 * }</pre>
 *
 * @param <T> the underlying value representation
 * @author Erik Pförtner
 * @see DynamicOps#asObjectAware()
 * @since 1.1.0
 */
@ApiStatus.Experimental
public interface ObjectAwareDynamicOps<T> extends DynamicOps<T> {

    /**
     * Creates a value from an arbitrary Java object.
     *
     * <p>The semantics of this method depend on the implementation. For
     * object-based implementations (e.g., {@code TestOps}), the object may be
     * stored as-is. Implementations must document what object types they
     * accept and how they are represented.</p>
     *
     * @param value the Java object to convert; must not be {@code null}
     * @return the created value; never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @since 1.1.0
     */
    @NotNull T createObject(@NotNull final Object value);

    /**
     * Reads an arbitrary Java object from a format value.
     *
     * <p>This is the read counterpart to {@link #createObject(Object)}.
     * Implementations should return a success result containing the Java
     * object, or an error result if the value cannot be converted.</p>
     *
     * @param input the format value to convert; must not be {@code null}
     * @return a {@link DataResult} containing the Java object on success,
     *         or an error describing why the operation failed
     * @throws NullPointerException if {@code input} is {@code null}
     * @see #createObject(Object)
     * @since 1.1.0
     */
    @NotNull DataResult<Object> getObjectValue(@NotNull final T input);

    /**
     * {@inheritDoc}
     *
     * <p>Since this implementation already is an {@code ObjectAwareDynamicOps},
     * this method returns {@code Optional.of(this)}.</p>
     *
     * @return an {@link Optional} containing {@code this}; never empty
     */
    @Override
    default @NotNull Optional<ObjectAwareDynamicOps<T>> asObjectAware() {
        return Optional.of(this);
    }
}
