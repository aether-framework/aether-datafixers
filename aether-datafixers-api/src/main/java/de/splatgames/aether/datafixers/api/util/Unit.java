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

package de.splatgames.aether.datafixers.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * A singleton type representing the absence of a meaningful value.
 *
 * <p>The {@code Unit} type is used when a type parameter is required but no actual
 * value is needed. It is similar to {@link Void} but can be instantiated (there is exactly one instance:
 * {@link #INSTANCE}). This is useful in generic contexts where a type must be specified but the value is
 * irrelevant.</p>
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li>As a type parameter for operations that don't produce a meaningful result:
 *       {@code DataResult<Unit>} for an operation that succeeds without a return value</li>
 *   <li>As the second type parameter in {@link Pair} when only one value is needed</li>
 *   <li>To indicate "no data" in codec operations</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Use Unit when no value is needed
 * DataResult<Unit> validationResult = validate(input);
 * if (validationResult.isSuccess()) {
 *     // Validation passed, no value needed
 * }
 *
 * // Use in generic APIs that require a type
 * Codec<Unit> unitCodec = Codec.unit(Unit.INSTANCE);
 *
 * // Compare with the singleton
 * if (result == Unit.INSTANCE) {
 *     // This is always true for any Unit instance
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe as it is an immutable singleton.</p>
 *
 * @author Erik Pförtner
 * @see Pair
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @since 0.1.0
 */
public final class Unit implements Serializable {

    /**
     * The singleton instance of Unit.
     *
     * <p>This is the only instance of {@code Unit} that should be used.
     * All equality comparisons with any {@code Unit} will return {@code true}.</p>
     */
    @NotNull
    public static final Unit INSTANCE = new Unit();

    /**
     * Serial version UID for serialization compatibility.
     *
     * <p>This value is arbitrary but should be changed if the class structure changes in a way that affects
     * serialization.</p>
     */
    @Serial
    private static final long serialVersionUID = -7628105959309438762L;

    /**
     * Private constructor to prevent external instantiation.
     *
     * <p>Use {@link #INSTANCE} to obtain the singleton instance.</p>
     */
    private Unit() {
        // private constructor to prevent external instantiation
    }

    /**
     * Returns a string representation of this Unit.
     *
     * @return the string {@code "Unit"}, never {@code null}
     */
    @Override
    @NotNull
    public String toString() {
        return "Unit";
    }

    /**
     * Returns a constant hash code for this Unit.
     *
     * <p>Since there is only one Unit instance, the hash code is always 1.</p>
     *
     * @return the constant hash code value 1
     */
    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * Compares this Unit to the specified object for equality.
     *
     * <p>Returns {@code true} if the specified object is also a Unit.
     * Since Unit is a singleton, this effectively checks if the object is an instance of the Unit class.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if the specified object is a Unit, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable final Object obj) {
        return obj instanceof Unit;
    }

    /**
     * Preserves the singleton guarantee during deserialization.
     *
     * @return {@link #INSTANCE}
     */
    @Serial
    private Object readResolve() {
        return INSTANCE;
    }
}
