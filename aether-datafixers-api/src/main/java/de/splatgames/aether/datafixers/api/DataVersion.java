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
     * Returns the numeric version value.
     *
     * @return the version number
     */
    public int getVersion() {
        return this.version;
    }

    @Override
    public int compareTo(@NotNull final DataVersion o) {
        return Integer.compare(this.version, o.version);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(this.version);
    }

    @Override
    public String toString() {
        return "DataVersion{" + "version=" + this.version + '}';
    }
}
