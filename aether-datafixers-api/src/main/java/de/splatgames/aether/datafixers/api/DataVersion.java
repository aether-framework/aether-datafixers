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

package de.splatgames.aether.datafixers.api;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.schema.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a version number for data schemas in the data fixing system.
 *
 * <p>{@code DataVersion} is used to track the version of serialized data,
 * enabling the {@link DataFixer} to determine which {@link DataFix} instances need to be applied when migrating data
 * between versions.</p>
 *
 * <h2>Version Ordering</h2>
 * <p>Versions are ordered numerically. A higher version number represents
 * newer data. The {@link #compareTo(DataVersion)} method enables natural ordering for version comparisons.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define versions
 * DataVersion v1 = new DataVersion(1);
 * DataVersion v2 = new DataVersion(2);
 * DataVersion current = new DataVersion(5);
 *
 * // Compare versions
 * if (dataVersion.compareTo(current) < 0) {
 *     // Data needs migration
 *     data = fixer.update(type, data, dataVersion, current);
 * }
 * }</pre>
 *
 * <h2>Storing Versions</h2>
 * <p>Applications typically store the version alongside serialized data
 * (e.g., as a "version" or "dataVersion" field) to enable future migrations.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataFixer
 * @see Schema
 * @since 0.1.0
 */
public final class DataVersion implements Comparable<DataVersion> {

    /**
     * The internal numeric representation of this data version.
     *
     * <p>This value is guaranteed to be non-negative and represents the version number
     * in a monotonically increasing sequence. Higher values indicate newer versions of the data schema.</p>
     */
    private final int version;

    /**
     * Creates a new data version with the specified version number.
     *
     * @param version the version number, must be non-negative
     * @throws IllegalArgumentException if version is negative
     */
    public DataVersion(final int version) {
        Preconditions.checkArgument(version >= 0, "Version must be non-negative");

        this.version = version;
    }

    /**
     * Returns the numeric version value of this data version.
     *
     * <p>The returned value is always non-negative and represents the sequential
     * version number of the data schema. This value can be used for:</p>
     * <ul>
     *   <li>Persisting the version alongside serialized data</li>
     *   <li>Calculating version ranges for migrations</li>
     *   <li>Displaying version information to users or in logs</li>
     * </ul>
     *
     * @return the version number as a non-negative integer
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Compares this data version with another data version for ordering.
     *
     * <p>This method establishes a natural ordering for data versions based on their
     * numeric values. The ordering follows these rules:</p>
     * <ul>
     *   <li>A version with a lower numeric value is considered "less than" a version
     *       with a higher numeric value</li>
     *   <li>Two versions with the same numeric value are considered equal</li>
     * </ul>
     *
     * <p>This ordering is consistent with {@link #equals(Object)}, meaning that
     * {@code v1.compareTo(v2) == 0} implies {@code v1.equals(v2)} and vice versa.</p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * DataVersion v100 = new DataVersion(100);
     * DataVersion v200 = new DataVersion(200);
     *
     * // Returns negative value (v100 < v200)
     * int result = v100.compareTo(v200);
     *
     * // Can be used for sorting
     * List<DataVersion> versions = Arrays.asList(v200, v100);
     * Collections.sort(versions); // Results in [v100, v200]
     * }</pre>
     *
     * @param o the data version to compare against; must not be {@code null}
     * @return a negative integer if this version is less than the specified version, zero if they are equal, or a al,
     * or a positive integer if this version is greater than the specified version
     * @throws NullPointerException if the specified data version is {@code null}
     */
    @Override
    public int compareTo(@NotNull final DataVersion o) {
        return Integer.compare(this.version, o.version);
    }

    /**
     * Indicates whether some other object is "equal to" this data version.
     *
     * <p>Two {@code DataVersion} instances are considered equal if and only if they
     * have the same numeric version value. This method adheres to the general contract of
     * {@link Object#equals(Object)}, providing:</p>
     * <ul>
     *   <li><b>Reflexivity:</b> For any non-null {@code DataVersion x}, {@code x.equals(x)}
     *       returns {@code true}</li>
     *   <li><b>Symmetry:</b> For any non-null {@code DataVersion} instances {@code x} and
     *       {@code y}, {@code x.equals(y)} returns {@code true} if and only if
     *       {@code y.equals(x)} returns {@code true}</li>
     *   <li><b>Transitivity:</b> For any non-null {@code DataVersion} instances {@code x},
     *       {@code y}, and {@code z}, if {@code x.equals(y)} returns {@code true} and
     *       {@code y.equals(z)} returns {@code true}, then {@code x.equals(z)} returns
     *       {@code true}</li>
     *   <li><b>Consistency:</b> Multiple invocations of {@code x.equals(y)} consistently
     *       return the same result, provided neither object is modified</li>
     *   <li><b>Non-nullity:</b> For any non-null {@code DataVersion x}, {@code x.equals(null)}
     *       returns {@code false}</li>
     * </ul>
     *
     * @param obj the reference object with which to compare; may be {@code null}
     * @return {@code true} if this data version is equal to the specified object; {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataVersion other)) {
            return false;
        }
        return this.version == other.version;
    }

    /**
     * Returns a hash code value for this data version.
     *
     * <p>The hash code is computed based on the numeric version value. This implementation
     * satisfies the general contract of {@link Object#hashCode()}, which states that:</p>
     * <ul>
     *   <li>If two {@code DataVersion} objects are equal according to the
     *       {@link #equals(Object)} method, then calling {@code hashCode()} on each of
     *       the two objects produces the same integer result</li>
     *   <li>It is not required that if two {@code DataVersion} objects are unequal according
     *       to {@link #equals(Object)}, then their hash codes must be distinct; however,
     *       producing distinct hash codes for unequal objects may improve hash table performance</li>
     * </ul>
     *
     * <p>This method is suitable for use in hash-based collections such as
     * {@link java.util.HashMap} and {@link java.util.HashSet}.</p>
     *
     * @return a hash code value for this data version
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.version);
    }

    /**
     * Returns a string representation of this data version.
     *
     * <p>The returned string follows the format {@code "DataVersion{version=N}"} where
     * {@code N} is the numeric version value. This format is intended for debugging and logging purposes and should not
     * be parsed programmatically.</p>
     *
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * new DataVersion(100).toString() // Returns "DataVersion{version=100}"
     * new DataVersion(0).toString()   // Returns "DataVersion{version=0}"
     * }</pre>
     *
     * @return a string representation of this data version in the format {@code "DataVersion{version=N}"}
     */
    @Override
    public String toString() {
        return "DataVersion{" + "version=" + this.version + '}';
    }
}
