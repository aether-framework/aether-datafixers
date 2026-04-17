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

import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Exception thrown when parsing input data fails.
 *
 * @author Erik Pförtner
 * @since 0.3.0
 */
public class FormatParseException extends RuntimeException {

    /**
     * Serial version UID for serialization compatibility.
     */
    @Serial
    private static final long serialVersionUID = -7928320612175804665L;

    /**
     * Constructs a new format parse exception with the specified message.
     *
     * @param message the detail message, may be {@code null}
     */
    public FormatParseException(@Nullable final String message) {
        super(message);
    }

    /**
     * Constructs a new format parse exception with the specified message and cause.
     *
     * @param message the detail message, may be {@code null}
     * @param cause   the cause of this exception, may be {@code null}
     */
    public FormatParseException(@Nullable final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
