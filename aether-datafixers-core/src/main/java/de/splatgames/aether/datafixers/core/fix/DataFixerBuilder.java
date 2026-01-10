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

package de.splatgames.aether.datafixers.core.fix;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A builder for constructing {@link DataFixer} instances.
 *
 * <p>{@code DataFixerBuilder} implements {@link FixRegistrar} and provides a
 * fluent API for registering data fixes and building a configured data fixer.
 * It accumulates fixes during construction and produces an immutable fixer.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DataFixer fixer = new DataFixerBuilder(new DataVersion(5))
 *     .withDefaultContext(myLoggingContext)
 *     .addFix(TypeReferences.PLAYER, new RenamePlayerNameFix())
 *     .addFix(TypeReferences.PLAYER, new AddHealthFieldFix())
 *     .addFix(TypeReferences.WORLD, new UpgradeTerrainFix())
 *     .build();
 * }</pre>
 *
 * <h2>Default Context</h2>
 * <p>By default, the builder uses {@link SimpleSystemDataFixerContext#INSTANCE}
 * which logs to {@code System.out}. Use {@link #withDefaultContext(DataFixerContext)}
 * to provide a custom logging context.</p>
 *
 * <h2>Fix Registration</h2>
 * <p>Fixes can be registered using:</p>
 * <ul>
 *   <li>{@link #addFix(TypeReference, DataFix)} - Fluent single fix registration (returns this)</li>
 *   <li>{@link #addFixes(TypeReference, List)} - Fluent bulk registration (returns this)</li>
 *   <li>{@link #register(TypeReference, DataFix)} - {@link FixRegistrar} interface method (void)</li>
 *   <li>{@link #registerAll(TypeReference, Iterable)} - {@link FixRegistrar} bulk registration (void)</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DataFixer
 * @see FixRegistrar
 * @see DataFixerImpl
 * @since 0.1.0
 */
public final class DataFixerBuilder implements FixRegistrar {

    private final DataVersion currentVersion;
    private final DataFixRegistry registry;
    private DataFixerContext defaultContext;

    /**
     * Creates a new builder for the specified current version.
     *
     * @param currentVersion the current (latest) data version, must not be {@code null}
     * @throws NullPointerException if currentVersion is {@code null}
     */
    public DataFixerBuilder(@NotNull final DataVersion currentVersion) {
        Preconditions.checkNotNull(currentVersion, "currentVersion must not be null");

        this.currentVersion = currentVersion;
        this.registry = new DataFixRegistry();
        this.defaultContext = SimpleSystemDataFixerContext.INSTANCE;
    }

    /**
     * Sets the default context for logging and diagnostics.
     *
     * @param context the context to use, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if context is {@code null}
     */
    @NotNull
    public DataFixerBuilder withDefaultContext(@NotNull final DataFixerContext context) {
        Preconditions.checkNotNull(context, "context must not be null");
        this.defaultContext = context;
        return this;
    }

    /**
     * Registers a single data fix (interface method).
     *
     * <p>This method implements {@link FixRegistrar#register(TypeReference, DataFix)}
     * and delegates to {@link #addFix(TypeReference, DataFix)}. For fluent API usage,
     * prefer {@code addFix()} which returns this builder for chaining.</p>
     *
     * @param type the type reference this fix applies to, must not be {@code null}
     * @param fix  the data fix to register, must not be {@code null}
     */
    @Override
    public void register(@NotNull final TypeReference type, @NotNull final DataFix<?> fix) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fix, "fix must not be null");
        this.addFix(type, fix);
    }

    /**
     * Registers multiple data fixes (interface method).
     *
     * <p>This method implements {@link FixRegistrar#registerAll(TypeReference, Iterable)}.
     * For fluent API usage, prefer the overload that takes a {@link List} and returns
     * this builder for chaining.</p>
     *
     * @param type  the type reference these fixes apply to, must not be {@code null}
     * @param fixes the data fixes to register, must not be {@code null}
     */
    @Override
    public void registerAll(@NotNull final TypeReference type, @NotNull final Iterable<? extends DataFix<?>> fixes) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fixes, "fixes must not be null");

        for (final DataFix<?> fix : fixes) {
            Preconditions.checkNotNull(fix, "fix must not be null");
            this.registry.register(type, fix);
        }
    }

    /**
     * Adds a single fix to the builder (fluent API).
     *
     * @param type the type reference this fix applies to, must not be {@code null}
     * @param fix  the data fix to add, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException     if type or fix is {@code null}
     * @throws IllegalArgumentException if fix.fromVersion &gt; fix.toVersion
     */
    @NotNull
    public DataFixerBuilder addFix(@NotNull final TypeReference type, @NotNull final DataFix<?> fix) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fix, "fix must not be null");
        Preconditions.checkArgument(
                fix.fromVersion().compareTo(fix.toVersion()) <= 0,
                "fix.fromVersion must be <= fix.toVersion"
        );

        this.registry.register(type, fix);
        return this;
    }

    /**
     * Adds multiple fixes to the builder (fluent API).
     *
     * <p>This is the fluent variant of {@link #registerAll(TypeReference, Iterable)}
     * that returns this builder for method chaining. Uses a {@link List} parameter
     * to enable more specific type inference.</p>
     *
     * @param type  the type reference these fixes apply to, must not be {@code null}
     * @param fixes the data fixes to add, must not be {@code null}
     * @return this builder for chaining
     * @throws NullPointerException if type or fixes is {@code null}
     */
    @NotNull
    public DataFixerBuilder addFixes(
            @NotNull final TypeReference type,
            @NotNull final List<? extends DataFix<?>> fixes
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fixes, "fixes must not be null");

        for (final DataFix<?> fix : fixes) {
            Preconditions.checkNotNull(fix, "fix must not be null");
            this.registry.register(type, fix);
        }
        return this;
    }

    /**
     * Builds an immutable {@link DataFixer} with all registered fixes.
     *
     * <p>This method freezes the internal fix registry, making it immutable
     * and thread-safe for concurrent access.</p>
     *
     * @return a new data fixer, never {@code null}
     */
    @NotNull
    public DataFixer build() {
        this.registry.freeze();
        return new DataFixerImpl(this.currentVersion, this.registry, this.defaultContext);
    }

    /**
     * Returns the internal fix registry.
     *
     * <p>This method provides access to the underlying {@link DataFixRegistry}
     * for analysis and introspection purposes. The registry may be mutable
     * if {@link #build()} has not been called yet.</p>
     *
     * <p><b>Warning:</b> Modifying the returned registry after build() has been
     * called will have no effect, as build() creates an immutable copy.</p>
     *
     * @return the fix registry, never {@code null}
     * @since 0.3.0
     */
    @NotNull
    public DataFixRegistry getFixRegistry() {
        return this.registry;
    }
}
