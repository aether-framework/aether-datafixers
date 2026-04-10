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

package de.splatgames.aether.datafixers.functional.diagnostic;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.type.SimpleType;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Test support utilities for the field-aware diagnostics E2E tests.
 *
 * <p>The diagnostic tests need to register types in mock schemas that accept
 * arbitrary {@code Dynamic} values without forcing a strict codec round-trip.
 * The built-in {@link Type#STRING}, {@link Type#INT} etc. all use codecs that
 * expect their respective Java type — but the field-rewriting rules
 * ({@link de.splatgames.aether.datafixers.api.rewrite.Rules#renameField}, etc.)
 * operate on free-form maps, so we need a passthrough type whose codec just
 * stores and retrieves the {@code Dynamic} unchanged.</p>
 *
 * <p>This is the same pattern used by the example schemas
 * ({@code Schema100#dynamicPassthroughCodec}); we duplicate it here so the
 * functional tests do not depend on the examples module.</p>
 */
final class DiagnosticsTestSupport {

    private DiagnosticsTestSupport() {
        // Utility class
    }

    /**
     * Creates a {@link Type} that wraps a {@code Dynamic<?>} value with a
     * passthrough codec.
     *
     * <p>The codec preserves the {@code Dynamic} unchanged on both encode and
     * decode, allowing field-rewriting rules to operate directly on the value
     * without going through a typed Java representation.</p>
     *
     * @param reference the type reference, must not be {@code null}
     * @return a passthrough type
     */
    @NotNull
    static Type<Dynamic<?>> passthroughType(@NotNull final TypeReference reference) {
        return new SimpleType<>(reference, dynamicPassthroughCodec());
    }

    /**
     * Creates a codec that treats {@code Dynamic<?>} as the value type and
     * passes it through unchanged in both directions.
     *
     * @return a passthrough codec
     */
    @NotNull
    static Codec<Dynamic<?>> dynamicPassthroughCodec() {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Dynamic<?> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                @SuppressWarnings("unchecked")
                final Dynamic<Object> dynamicObj = (Dynamic<Object>) input;
                final Dynamic<T> converted = dynamicObj.convert(ops);
                return DataResult.success(converted.value());
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<Dynamic<?>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                              @NotNull final T input) {
                final Dynamic<T> dynamic = new Dynamic<>(ops, input);
                return DataResult.success(Pair.of(dynamic, ops.empty()));
            }
        };
    }
}
