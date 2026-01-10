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
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import de.splatgames.aether.datafixers.core.diagnostic.DiagnosticRuleWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract base class for data fixes that use schema-based type rewrite rules.
 *
 * <p>{@code SchemaDataFix} combines the {@link DataFix} interface with
 * {@link TypeRewriteRule}-based transformations. Subclasses implement
 * {@link #makeRule(Schema, Schema)} to define the migration logic using
 * the type-safe rule API.</p>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * public class RenamePlayerFieldFix extends SchemaDataFix {
 *     public RenamePlayerFieldFix(SchemaRegistry schemas) {
 *         super("rename_player_field",
 *               new DataVersion(1),
 *               new DataVersion(2),
 *               schemas);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
 *         return Fixes.renameField(
 *             GsonOps.INSTANCE,
 *             "playerName",
 *             "name",
 *             inputSchema.require(TypeReferences.PLAYER)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Schema Access</h2>
 * <p>The {@link #makeRule(Schema, Schema)} method receives both the input and
 * output schemas, allowing access to type definitions for both versions.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Subclasses should be stateless and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see TypeRewriteRule
 * @see Schema
 * @since 0.1.0
 */
public abstract class SchemaDataFix implements DataFix<Object> {

    private final String name;
    private final DataVersion from;
    private final DataVersion to;
    private final SchemaRegistry schemas;

    /**
     * Creates a new schema-based data fix.
     *
     * @param name    the fix name for logging and debugging, must not be {@code null}
     * @param from    the source version this fix migrates from, must not be {@code null}
     * @param to      the target version this fix migrates to, must not be {@code null}
     * @param schemas the schema registry for accessing type definitions, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    protected SchemaDataFix(
            @NotNull final String name,
            @NotNull final DataVersion from,
            @NotNull final DataVersion to,
            @NotNull final SchemaRegistry schemas
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(from, "from must not be null");
        Preconditions.checkNotNull(to, "to must not be null");
        Preconditions.checkNotNull(schemas, "schemas must not be null");

        this.name = name;
        this.from = from;
        this.to = to;
        this.schemas = schemas;
    }

    @Override
    public final @NotNull String name() {
        return this.name;
    }

    @Override
    public final @NotNull DataVersion fromVersion() {
        return this.from;
    }

    @Override
    public final @NotNull DataVersion toVersion() {
        return this.to;
    }

    /**
     * Creates the type rewrite rule for this fix.
     *
     * <p>Subclasses implement this method to define the data transformation
     * using the type-safe rule API. Both input and output schemas are provided
     * for accessing type definitions.</p>
     *
     * @param inputSchema  the schema for the source version, never {@code null}
     * @param outputSchema the schema for the target version, never {@code null}
     * @return the rewrite rule to apply, never {@code null}
     */
    protected abstract @NotNull TypeRewriteRule makeRule(
            @NotNull Schema inputSchema,
            @NotNull Schema outputSchema
    );

    @Override
    public final @NotNull Dynamic<Object> apply(
            @NotNull final TypeReference type,
            @NotNull final Dynamic<Object> input,
            @NotNull final DataFixerContext context
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(context, "context must not be null");

        final Schema in = this.schemas.require(this.from);
        final Schema out = this.schemas.require(this.to);

        final Type<?> logical = in.require(type);

        TypeRewriteRule rule = this.makeRule(in, out);

        // Wrap rule with diagnostics if enabled
        if (context instanceof DiagnosticContext dc && dc.isDiagnosticEnabled()) {
            rule = DiagnosticRuleWrapper.wrap(rule, dc);
        }

        @SuppressWarnings("unchecked")
        final Typed<?> typedIn = new Typed<>((Type<Object>) logical, input);
        final Typed<?> typedOut = rule.apply(typedIn);

        @SuppressWarnings("unchecked")
        final Dynamic<Object> result = (Dynamic<Object>) typedOut.value();

        return result;
    }
}
