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
 * <p>This package provides the {@link de.splatgames.aether.datafixers.api.result.DataResult}
 * type, a functional alternative to exceptions for representing operations that may succeed or fail. This approach
 * enables explicit error handling and composition of fallible operations.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.result.DataResult} - Represents
 *       either a successful result with a value, or a failure with an error message.
 *       Similar to {@code Either<Error, T>} or Rust's {@code Result<T, E>}.</li>
 * </ul>
 *
 * <h2>Creating Results</h2>
 * <pre>{@code
 * // Success
 * DataResult<Integer> success = DataResult.success(42);
 *
 * // Failure
 * DataResult<Integer> failure = DataResult.error("Value must be positive");
 * }</pre>
 *
 * <h2>Handling Results</h2>
 * <pre>{@code
 * DataResult<Player> result = playerCodec.decode(dynamic);
 *
 * // Pattern matching style
 * result.ifSuccess(player -> System.out.println("Loaded: " + player.name()))
 *       .ifError(error -> System.err.println("Failed: " + error.message()));
 *
 * // Get with default
 * Player player = result.resultOrPartial(System.err::println)
 *                       .orElse(Player.DEFAULT);
 *
 * // Throw on error
 * Player player = result.getOrThrow(DecodeException::new);
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>DataResult supports monadic composition for chaining fallible operations:</p>
 * <pre>{@code
 * DataResult<Config> loadConfig(Path path) {
 *     return readFile(path)                    // DataResult<String>
 *         .flatMap(this::parseJson)            // DataResult<JsonElement>
 *         .flatMap(configCodec::decode);       // DataResult<Config>
 * }
 *
 * // Transform successful values
 * DataResult<Integer> doubled = result.map(x -> x * 2);
 *
 * // Provide fallback on error
 * DataResult<Integer> withFallback = result.mapError(e -> 0);
 * }</pre>
 *
 * <h2>Partial Results</h2>
 * <p>DataResult can represent partial success, where an operation produces both
 * a result and error information. This is useful for lenient parsing:</p>
 * <pre>{@code
 * // Parse with warnings
 * DataResult<Config> result = parseConfig(input);
 * if (result.hasPartialResult()) {
 *     Config partial = result.partialResult().get();
 *     String warnings = result.error().get().message();
 *     log.warn("Loaded config with warnings: {}", warnings);
 * }
 * }</pre>
 *
 * <h2>Why DataResult over Exceptions?</h2>
 * <ul>
 *   <li>Makes error handling explicit in the type system</li>
 *   <li>Enables functional composition without try-catch blocks</li>
 *   <li>Supports partial results and error recovery</li>
 *   <li>Better performance for expected failures (no stack trace)</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.result;
