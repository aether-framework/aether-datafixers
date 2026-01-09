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
 * <strong>DEPRECATED:</strong> Legacy package for Jackson-based {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps}
 * implementation. This package is retained only for backwards compatibility and will be removed in version 1.0.0.
 *
 * <p>This package contains the original {@link de.splatgames.aether.datafixers.codec.jackson.JacksonOps} class
 * from the pre-0.4.0 package structure. All classes in this package are deprecated and delegate to their
 * replacements in the reorganized {@link de.splatgames.aether.datafixers.codec.json.jackson} package.</p>
 *
 * <h2>Migration Guide</h2>
 * <p>To migrate from this deprecated package to the new package structure:</p>
 *
 * <h3>Import Changes</h3>
 * <pre>{@code
 * // Old imports (deprecated, will be removed in 1.0.0)
 * import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
 *
 * // New imports (recommended)
 * import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
 * }</pre>
 *
 * <h3>Code Changes</h3>
 * <p>The class has been renamed from {@code JacksonOps} to {@code JacksonJsonOps} for clarity:</p>
 * <pre>{@code
 * // Old code (deprecated)
 * JacksonOps ops = JacksonOps.INSTANCE;
 * JacksonOps customOps = new JacksonOps(customMapper);
 * Dynamic<JsonNode> dynamic = new Dynamic<>(ops, jsonNode);
 *
 * // New code (recommended)
 * JacksonJsonOps ops = JacksonJsonOps.INSTANCE;
 * JacksonJsonOps customOps = new JacksonJsonOps(customMapper);
 * Dynamic<JsonNode> dynamic = new Dynamic<>(ops, jsonNode);
 * }</pre>
 *
 * <h2>Deprecation Timeline</h2>
 * <table border="1">
 *   <caption>Deprecation and Removal Schedule</caption>
 *   <tr><th>Version</th><th>Status</th><th>Action Required</th></tr>
 *   <tr><td>0.4.0</td><td>Deprecated</td><td>Update imports and class names; old code continues to work</td></tr>
 *   <tr><td>0.5.0 - 0.9.x</td><td>Deprecated</td><td>Warnings during compilation; functionality unchanged</td></tr>
 *   <tr><td>1.0.0</td><td><strong>Removed</strong></td><td>Package deleted; migration required before upgrade</td></tr>
 * </table>
 *
 * <h2>Why This Change?</h2>
 * <p>The package reorganization in version 0.4.0 introduced a cleaner, more scalable structure:</p>
 * <ul>
 *   <li><strong>Format-Based Organization:</strong> All JSON implementations are now grouped under
 *       {@code codec.json.*}, YAML under {@code codec.yaml.*}, etc.</li>
 *   <li><strong>Library-Based Subpackages:</strong> Each format has subpackages for different
 *       libraries (e.g., {@code json.gson}, {@code json.jackson})</li>
 *   <li><strong>Disambiguated Naming:</strong> {@code JacksonOps} is now {@code JacksonJsonOps} to
 *       distinguish it from {@code JacksonYamlOps}, {@code JacksonTomlOps}, and {@code JacksonXmlOps}</li>
 * </ul>
 *
 * <h2>New Package Structure</h2>
 * <pre>
 * de.splatgames.aether.datafixers.codec
 * ├── json
 * │   ├── gson/GsonOps.java
 * │   └── jackson/JacksonJsonOps.java   (renamed from JacksonOps)
 * ├── yaml
 * │   ├── jackson/JacksonYamlOps.java   (new)
 * │   └── snakeyaml/SnakeYamlOps.java   (new)
 * ├── toml
 * │   └── jackson/JacksonTomlOps.java   (new)
 * └── xml
 *     └── jackson/JacksonXmlOps.java    (new)
 * </pre>
 *
 * <h2>Class Mapping</h2>
 * <table border="1">
 *   <caption>Old to New Class Mapping</caption>
 *   <tr><th>Old Class (Deprecated)</th><th>New Class (Recommended)</th></tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.jackson.JacksonOps}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code JacksonOps.INSTANCE}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps#INSTANCE}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code new JacksonOps(mapper)}</td>
 *     <td>{@code new JacksonJsonOps(mapper)}</td>
 *   </tr>
 * </table>
 *
 * <h2>Delegation Pattern</h2>
 * <p>The deprecated {@link de.splatgames.aether.datafixers.codec.jackson.JacksonOps} class uses the
 * delegation pattern to forward all method calls to the new
 * {@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps} implementation. This ensures:</p>
 * <ul>
 *   <li>Identical behavior between deprecated and new implementations</li>
 *   <li>Bug fixes applied to the new implementation automatically benefit deprecated users</li>
 *   <li>No performance overhead beyond a single method delegation</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All classes in this deprecated package maintain the same thread-safety guarantees as their
 * replacements. The singleton {@link de.splatgames.aether.datafixers.codec.jackson.JacksonOps#INSTANCE}
 * can be safely shared across multiple threads. Custom instances created with a custom
 * {@link com.fasterxml.jackson.databind.ObjectMapper} are thread-safe if the provided mapper is thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
 * @see de.splatgames.aether.datafixers.codec.json
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @since 0.1.0
 * @deprecated Since 0.4.0. Use classes from {@link de.splatgames.aether.datafixers.codec.json.jackson}
 *             instead. This package will be removed in version 1.0.0.
 */
@Deprecated(since = "0.4.0", forRemoval = true)
package de.splatgames.aether.datafixers.codec.jackson;
