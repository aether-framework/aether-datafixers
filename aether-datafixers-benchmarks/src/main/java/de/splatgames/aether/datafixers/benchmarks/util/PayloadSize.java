/*
 * Copyright (c) 2026 Splatgames.de Software and Contributors
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

package de.splatgames.aether.datafixers.benchmarks.util;

/**
 * Defines payload sizes for benchmark test data generation.
 *
 * <p>Each size configuration controls the complexity of generated test data:
 * <ul>
 *   <li><b>SMALL</b> - Quick benchmarks, minimal data (5 fields, 2 nesting levels, 10 list items)</li>
 *   <li><b>MEDIUM</b> - Balanced benchmarks (20 fields, 4 nesting levels, 100 list items)</li>
 *   <li><b>LARGE</b> - Stress testing (50 fields, 6 nesting levels, 1000 list items)</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public enum PayloadSize {

    /**
     * Small payload: 5 fields, 2 nesting levels, 10 list items.
     * Suitable for quick benchmark iterations.
     */
    SMALL(5, 2, 10),

    /**
     * Medium payload: 20 fields, 4 nesting levels, 100 list items.
     * Balanced for typical performance testing.
     */
    MEDIUM(20, 4, 100),

    /**
     * Large payload: 50 fields, 6 nesting levels, 1000 list items.
     * Suitable for stress testing and worst-case analysis.
     */
    LARGE(50, 6, 1000);

    private final int fieldCount;
    private final int nestingDepth;
    private final int listSize;

    PayloadSize(final int fieldCount, final int nestingDepth, final int listSize) {
        this.fieldCount = fieldCount;
        this.nestingDepth = nestingDepth;
        this.listSize = listSize;
    }

    /**
     * Returns the number of top-level fields to generate.
     *
     * @return the field count
     */
    public int getFieldCount() {
        return this.fieldCount;
    }

    /**
     * Returns the maximum nesting depth for nested objects.
     *
     * @return the nesting depth
     */
    public int getNestingDepth() {
        return this.nestingDepth;
    }

    /**
     * Returns the number of items to generate in lists.
     *
     * @return the list size
     */
    public int getListSize() {
        return this.listSize;
    }
}
