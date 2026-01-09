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

package de.splatgames.aether.datafixers.spring.autoconfigure;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for managing multiple {@link AetherDataFixer} instances across domains.
 *
 * <p>This registry provides centralized management of DataFixer instances in multi-domain
 * setups. Each domain represents a separate category of data (e.g., game data, user data,
 * world data) with its own independent version history and migration path.</p>
 *
 * <h2>Purpose</h2>
 * <p>In enterprise applications, different types of data often evolve independently.
 * For example:</p>
 * <ul>
 *   <li><strong>Game Domain:</strong> Player saves, game state, achievements</li>
 *   <li><strong>User Domain:</strong> Profiles, preferences, settings</li>
 *   <li><strong>World Domain:</strong> Map data, chunk information, entity positions</li>
 * </ul>
 * <p>Each domain can have its own DataFixer with separate schemas and fixes, allowing
 * for independent versioning and migration strategies.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Injection and Usage</h3>
 * <pre>{@code
 * @Service
 * public class MigrationService {
 *
 *     private final DataFixerRegistry registry;
 *
 *     @Autowired
 *     public MigrationService(DataFixerRegistry registry) {
 *         this.registry = registry;
 *     }
 *
 *     public TaggedDynamic<?> migrateGameData(TaggedDynamic<?> data) {
 *         AetherDataFixer fixer = registry.require("game");
 *         return fixer.update(
 *             TypeReferences.GAME_SAVE,
 *             data,
 *             fixer.currentVersion()
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h3>Checking Domain Availability</h3>
 * <pre>{@code
 * if (registry.contains("legacy")) {
 *     AetherDataFixer legacyFixer = registry.get("legacy");
 *     // Handle legacy data migration
 * }
 * }</pre>
 *
 * <h3>Iterating All Domains</h3>
 * <pre>{@code
 * for (String domain : registry.getDomains()) {
 *     AetherDataFixer fixer = registry.require(domain);
 *     log.info("Domain '{}' at version {}",
 *         domain, fixer.currentVersion().getVersion());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations are backed by a {@link ConcurrentHashMap},
 * allowing safe concurrent access from multiple threads. However, note that:</p>
 * <ul>
 *   <li>Registration typically occurs during application startup</li>
 *   <li>After startup, the registry is effectively read-only</li>
 *   <li>Attempting to register a duplicate domain will throw an exception</li>
 * </ul>
 *
 * <h2>Default Domain</h2>
 * <p>Single-bootstrap setups use the {@link #DEFAULT_DOMAIN} constant as the domain name.
 * This allows applications to start simple and migrate to multi-domain later without
 * changing their code.</p>
 *
 * @author Erik Pförtner
 * @see AetherDataFixer
 * @see DataFixerAutoConfiguration
 * @see de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties
 * @since 0.4.0
 */
public class DataFixerRegistry {

    /**
     * The default domain name used for single-bootstrap setups.
     *
     * <p>When an application defines only one {@code DataFixerBootstrap} bean
     * without a qualifier, the resulting {@code AetherDataFixer} is registered
     * under this default domain name.</p>
     *
     * <p>Value: {@value}</p>
     */
    public static final String DEFAULT_DOMAIN = "default";

    /**
     * Thread-safe storage for domain-to-fixer mappings.
     *
     * <p>Uses ConcurrentHashMap to ensure safe concurrent access during
     * both registration (startup) and retrieval (runtime) phases.</p>
     */
    private final Map<String, AetherDataFixer> fixers = new ConcurrentHashMap<>();

    /**
     * Registers a DataFixer instance for the specified domain.
     *
     * <p>This method associates a domain name with an {@link AetherDataFixer} instance.
     * Once registered, the fixer can be retrieved using {@link #get(String)} or
     * {@link #require(String)}.</p>
     *
     * <p><strong>Note:</strong> Each domain can only have one registered fixer.
     * Attempting to register a second fixer for the same domain will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param domain the unique domain name identifying this fixer, must not be {@code null}
     * @param fixer  the DataFixer instance to register, must not be {@code null}
     * @throws IllegalArgumentException if a fixer is already registered for this domain
     * @throws NullPointerException     if domain or fixer is {@code null}
     */
    public void register(@NotNull final String domain, @NotNull final AetherDataFixer fixer) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        Preconditions.checkNotNull(fixer, "fixer must not be null");
        if (this.fixers.containsKey(domain)) {
            throw new IllegalArgumentException(
                    "DataFixer already registered for domain: " + domain
            );
        }
        this.fixers.put(domain, fixer);
    }

    /**
     * Returns the DataFixer for the specified domain, or {@code null} if not found.
     *
     * <p>This method performs a lenient lookup that returns {@code null} when the
     * domain is not registered. Use this when the domain's presence is optional or
     * when you want to handle missing domains gracefully.</p>
     *
     * @param domain the domain name to look up, must not be {@code null}
     * @return the DataFixer for the domain, or {@code null} if not registered
     * @throws NullPointerException if domain is {@code null}
     * @see #require(String) for a strict lookup that throws on missing domains
     */
    @Nullable
    public AetherDataFixer get(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        return this.fixers.get(domain);
    }

    /**
     * Returns the DataFixer for the specified domain, throwing if not found.
     *
     * <p>This method performs a strict lookup that throws an exception when the
     * domain is not registered. Use this when the domain must exist for correct
     * operation, as it provides clear error messages including available domains.</p>
     *
     * <p>The error message includes the list of available domains to help with
     * debugging configuration issues.</p>
     *
     * @param domain the domain name to look up, must not be {@code null}
     * @return the DataFixer for the domain, never {@code null}
     * @throws IllegalArgumentException if no fixer is registered for this domain
     * @throws NullPointerException     if domain is {@code null}
     * @see #get(String) for a lenient lookup that returns null on missing domains
     */
    @NotNull
    public AetherDataFixer require(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        final AetherDataFixer fixer = this.fixers.get(domain);
        if (fixer == null) {
            throw new IllegalArgumentException(
                    "No DataFixer registered for domain: " + domain +
                    ". Available domains: " + this.fixers.keySet()
            );
        }
        return fixer;
    }

    /**
     * Returns the DataFixer registered under the default domain.
     *
     * <p>This is a convenience method equivalent to calling {@code get(DEFAULT_DOMAIN)}.
     * It's particularly useful in single-domain setups where the default domain is
     * implicitly used.</p>
     *
     * @return the default DataFixer, or {@code null} if no default is registered
     * @see #DEFAULT_DOMAIN
     */
    @Nullable
    public AetherDataFixer getDefault() {
        return this.fixers.get(DEFAULT_DOMAIN);
    }

    /**
     * Returns an immutable snapshot of all registered domain-to-fixer mappings.
     *
     * <p>The returned map is a defensive copy that will not reflect subsequent
     * registrations. This is useful for introspection and actuator endpoints.</p>
     *
     * @return an unmodifiable map of domain names to DataFixer instances, never {@code null}
     */
    @NotNull
    public Map<String, AetherDataFixer> getAll() {
        return Map.copyOf(this.fixers);
    }

    /**
     * Returns an immutable snapshot of all registered domain names.
     *
     * <p>The returned set is a defensive copy that will not reflect subsequent
     * registrations. The set can be safely iterated without risk of
     * {@link java.util.ConcurrentModificationException}.</p>
     *
     * @return an unmodifiable set of domain names, never {@code null}
     */
    @NotNull
    public Set<String> getDomains() {
        return Set.copyOf(this.fixers.keySet());
    }

    /**
     * Checks whether a DataFixer is registered for the specified domain.
     *
     * <p>This method allows checking domain availability without retrieving
     * the actual fixer instance.</p>
     *
     * @param domain the domain name to check, must not be {@code null}
     * @return {@code true} if a fixer is registered for this domain, {@code false} otherwise
     * @throws NullPointerException if domain is {@code null}
     */
    public boolean contains(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        return this.fixers.containsKey(domain);
    }

    /**
     * Returns the total number of registered DataFixer instances.
     *
     * <p>This count includes all domains, including the default domain if registered.</p>
     *
     * @return the number of registered fixers, always non-negative
     */
    public int size() {
        return this.fixers.size();
    }

    /**
     * Checks whether the registry contains any registered DataFixer instances.
     *
     * <p>An empty registry typically indicates that no {@code DataFixerBootstrap}
     * beans were found during application startup, or that auto-configuration
     * is disabled.</p>
     *
     * @return {@code true} if no DataFixers are registered, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.fixers.isEmpty();
    }
}
