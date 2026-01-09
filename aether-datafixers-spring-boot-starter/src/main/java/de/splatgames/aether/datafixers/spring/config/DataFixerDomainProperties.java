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

package de.splatgames.aether.datafixers.spring.config;

import org.jetbrains.annotations.Nullable;

/**
 * Configuration properties for a specific DataFixer domain.
 *
 * <p>This class encapsulates the configuration for an individual domain within
 * a multi-domain DataFixer setup. Each domain represents a separate data type
 * or entity category (e.g., game saves, user profiles, world data) that has its
 * own independent version history and migration path.</p>
 *
 * <h2>Domain Concept</h2>
 * <p>Domains allow you to manage multiple, independent DataFixer instances within
 * a single application. This is useful when:</p>
 * <ul>
 *   <li>Different data types evolve at different rates</li>
 *   <li>You want isolated version histories for different entity types</li>
 *   <li>Some data types require different migration strategies</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     domains:
 *       game:
 *         current-version: 200
 *         primary: true
 *         description: "Game save data migrations"
 *       user:
 *         current-version: 150
 *         primary: false
 *         description: "User profile migrations"
 *       world:
 *         current-version: 300
 *         description: "World chunk data migrations"
 * }</pre>
 *
 * <h2>Version Resolution</h2>
 * <p>When determining the current version for a domain, the following priority is used:</p>
 * <ol>
 *   <li>The {@link #currentVersion} configured in this properties object</li>
 *   <li>The {@code CURRENT_VERSION} constant defined in the domain's bootstrap class</li>
 *   <li>The global {@code default-current-version} from the parent properties</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is mutable and not thread-safe. It is designed to be configured
 * during application startup through Spring's property binding mechanism.</p>
 *
 * @author Erik Pfoertner
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry
 * @since 0.4.0
 */
public class DataFixerDomainProperties {

    /**
     * The current (latest) data version for this domain.
     *
     * <p>This value represents the version number that data should be migrated to.
     * If not set, the framework will attempt to determine the version from the
     * domain's bootstrap class or fall back to the global default.</p>
     *
     * <p>Version numbers should follow semantic conventions where applicable
     * (e.g., 100 for v1.0.0, 110 for v1.1.0, 200 for v2.0.0).</p>
     */
    @Nullable
    private Integer currentVersion;

    /**
     * Flag indicating whether this domain should be the primary bean.
     *
     * <p>When multiple DataFixer beans exist, Spring requires one to be marked
     * as primary for unqualified injection. Only one domain should have this
     * set to {@code true}.</p>
     */
    private boolean primary = false;

    /**
     * Human-readable description of this domain's purpose.
     *
     * <p>This description is displayed in actuator endpoints and can be useful
     * for operational documentation and monitoring dashboards.</p>
     */
    @Nullable
    private String description;

    /**
     * Returns the current data version for this domain.
     *
     * <p>The current version represents the target version for migrations.
     * Data with lower version numbers will be migrated up to this version.</p>
     *
     * @return the current version number, or {@code null} if not explicitly configured
     *         (in which case the version will be determined from the bootstrap or
     *         global defaults)
     */
    @Nullable
    public Integer getCurrentVersion() {
        return this.currentVersion;
    }

    /**
     * Sets the current data version for this domain.
     *
     * <p>Setting this value overrides any version defined in the bootstrap class.
     * This is useful for testing or for controlling version progression through
     * external configuration.</p>
     *
     * @param currentVersion the version number to set, or {@code null} to use
     *                       the bootstrap's version
     */
    public void setCurrentVersion(@Nullable final Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Returns whether this domain should be the primary bean.
     *
     * <p>The primary bean is used when a {@code DataFixer} is injected without
     * a {@code @Qualifier} annotation. In multi-domain setups, exactly one
     * domain should be marked as primary to avoid ambiguity errors.</p>
     *
     * @return {@code true} if this domain is the primary bean, {@code false} otherwise
     */
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * Sets whether this domain should be the primary bean.
     *
     * <p><strong>Important:</strong> Only one domain should be marked as primary.
     * If multiple domains are marked as primary, Spring will throw an ambiguity
     * error during application startup.</p>
     *
     * @param primary {@code true} to make this domain the primary bean,
     *                {@code false} otherwise
     */
    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }

    /**
     * Returns the description for this domain.
     *
     * <p>The description provides human-readable context about what type of
     * data this domain handles. It is exposed through actuator endpoints for
     * operational visibility.</p>
     *
     * @return the domain description, or {@code null} if not set
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description for this domain.
     *
     * <p>A good description should briefly explain:</p>
     * <ul>
     *   <li>What type of data this domain manages</li>
     *   <li>The purpose of the migrations</li>
     *   <li>Any relevant context for operators</li>
     * </ul>
     *
     * <h3>Example Descriptions</h3>
     * <ul>
     *   <li>"Game save data migrations for player progress"</li>
     *   <li>"User profile schema evolution"</li>
     *   <li>"Configuration file format migrations"</li>
     * </ul>
     *
     * @param description the description text, or {@code null} to clear it
     */
    public void setDescription(@Nullable final String description) {
        this.description = description;
    }
}
