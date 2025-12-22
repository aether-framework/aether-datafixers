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

package de.splatgames.aether.datafixers.api.fix;

import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;

/**
 * A registry for collecting {@link DataFix} instances during fixer construction.
 *
 * <p>{@code FixRegistrar} provides a simple interface for registering data fixes
 * with a {@link DataFixer} during its construction phase. Fixes are associated
 * with {@link TypeReference} values to indicate what type of data they transform.</p>
 *
 * <h2>Usage Pattern</h2>
 * <p>Registrars are typically provided to bootstrap code that populates fixes:</p>
 * <pre>{@code
 * public class MyDataFixerBootstrap {
 *     public static void registerFixes(FixRegistrar registrar) {
 *         // Register individual fixes
 *         registrar.register(TypeReferences.PLAYER, new RenamePlayerNameFix());
 *         registrar.register(TypeReferences.PLAYER, new AddHealthFieldFix());
 *
 *         // Register multiple fixes at once
 *         registrar.registerAll(TypeReferences.WORLD, List.of(
 *             new UpgradeTerrainFix(),
 *             new MigrateBiomesFix()
 *         ));
 *     }
 * }
 * }</pre>
 *
 * <h2>Fix Ordering</h2>
 * <p>Fixes are applied in version order (determined by {@link DataFix#fromVersion()}
 * and {@link DataFix#toVersion()}), not registration order. Implementations may
 * sort fixes after all registrations are complete.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataFixer
 * @see TypeReference
 * @since 0.1.0
 */
public interface FixRegistrar {

    /**
     * Registers a single data fix for a type.
     *
     * <p>The fix will be applied to data of the specified type when updating
     * across the version range defined by the fix.</p>
     *
     * @param type the type reference this fix applies to, must not be {@code null}
     * @param fix  the data fix to register, must not be {@code null}
     */
    void register(@NotNull final TypeReference type,
                  @NotNull final DataFix<?> fix);

    /**
     * Registers multiple data fixes for a type.
     *
     * <p>This is a convenience method for bulk registration. Each fix in the
     * iterable is registered as if {@link #register(TypeReference, DataFix)}
     * were called individually.</p>
     *
     * @param type  the type reference these fixes apply to, must not be {@code null}
     * @param fixes the data fixes to register, must not be {@code null}
     */
    void registerAll(@NotNull TypeReference type,
                     @NotNull Iterable<? extends DataFix<?>> fixes);
}
