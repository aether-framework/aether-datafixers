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

/**
 * Result types for representing success/failure outcomes.
 *
 * <p>This package provides the
 * {@link de.splatgames.aether.datafixers.api.result.DataResult} type, a
 * functional alternative to exceptions for operations that may succeed or
 * fail. It makes error handling explicit in the type system and supports
 * partial successes (a value produced alongside a warning).</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.result.DataResult} —
 *       Represents a successful result with a value, or a failure with a
 *       {@link java.lang.String} error message. Conceptually similar to
 *       Rust's {@code Result<T, String>} but with optional partial results.</li>
 * </ul>
 *
 * <h2>Creating Results</h2>
 * <pre>{@code
 * // Success
 * DataResult<Integer> success = DataResult.success(42);
 *
 * // Failure
 * DataResult<Integer> failure = DataResult.error("Value must be positive");
 *
 * // Failure carrying a best-effort partial value
 * DataResult<Integer> partial = DataResult.error("Out of range, clamped", 100);
 * }</pre>
 *
 * <h2>Handling Results</h2>
 * <pre>{@code
 * DataResult<Pair<Player, JsonElement>> result = playerCodec.decode(ops, root);
 *
 * // Chainable inspection — both blocks return the same DataResult
 * result
 *     .ifSuccess(pair -> System.out.println("Loaded: " + pair.first().name()))
 *     .ifError(message -> System.err.println("Failed: " + message));
 *
 * // Get with a default; side-channel any error message for logging
 * Player player = result.resultOrPartial(System.err::println)
 *     .map(Pair::first)
 *     .orElse(Player.DEFAULT);
 *
 * // Throw on error
 * Pair<Player, JsonElement> decoded = result.getOrThrow(DecodeException::new);
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>{@code DataResult} supports monadic composition for chaining fallible
 * operations. {@link de.splatgames.aether.datafixers.api.result.DataResult#mapError(java.util.function.Function) mapError}
 * transforms the <i>error message</i>, not the success value:</p>
 * <pre>{@code
 * DataResult<Config> loadConfig(Path path) {
 *     return readFile(path)                  // DataResult<String>
 *         .flatMap(this::parseJson)          // DataResult<JsonElement>
 *         .flatMap(configCodec::decode)      // DataResult<Pair<Config, JsonElement>>
 *         .map(Pair::first);              // DataResult<Config>
 * }
 *
 * // Transform successful values
 * DataResult<Integer> doubled = result.map(x -> x * 2);
 *
 * // Rewrite the error message while preserving the shape
 * DataResult<Integer> annotated = result.mapError(m -> "config load failed: " + m);
 * }</pre>
 *
 * <h2>Partial Results</h2>
 * <p>{@code DataResult} can represent partial success, where an operation
 * produces both a best-effort value and a warning. Inspect
 * {@link de.splatgames.aether.datafixers.api.result.DataResult#isError() isError}
 * together with
 * {@link de.splatgames.aether.datafixers.api.result.DataResult#partialResult() partialResult}
 * to detect this case. This is useful for lenient parsing paths:</p>
 * <pre>{@code
 * DataResult<Config> result = parseConfig(input);
 * if (result.isError() && result.partialResult().isPresent()) {
 *     Config partial = result.partialResult().get();
 *     String warnings = result.error().orElse("unknown error");
 *     log.warn("Loaded config with warnings: {}", warnings);
 *     return partial;
 * }
 * }</pre>
 *
 * <h2>Why {@code DataResult} over Exceptions?</h2>
 * <ul>
 *   <li>Makes error handling explicit in the type system.</li>
 *   <li>Enables functional composition without {@code try}/{@code catch} blocks.</li>
 *   <li>Supports partial results and error recovery.</li>
 *   <li>Better performance for expected failures — no stack trace is built.</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.result;
