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

package de.splatgames.aether.datafixers.api.type.template;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.type.Type;
import org.jetbrains.annotations.NotNull;

/**
 * A template for creating types that can be instantiated with type parameters.
 *
 * <p>A {@code TypeTemplate} is a blueprint for constructing concrete {@link Type} instances.
 * Templates enable parameterized and recursive type definitions, allowing complex type structures to be defined
 * declaratively and instantiated with different parameters.</p>
 *
 * <h2>Purpose</h2>
 * <p>Type templates serve several key purposes in the data fixing system:</p>
 * <ul>
 *   <li>Define parameterized types that can be instantiated with different type arguments</li>
 *   <li>Enable recursive type definitions (types that reference themselves)</li>
 *   <li>Provide a declarative DSL for defining schema types</li>
 *   <li>Support type abstraction and reuse across different schemas</li>
 * </ul>
 *
 * <h2>Core Operation</h2>
 * <ul>
 *   <li>{@link #apply(TypeFamily)} - Instantiate this template with a type family to get a concrete type</li>
 *   <li>{@link #bind(String)} - Give this template a name for recursive references</li>
 *   <li>{@link #describe()} - Get a human-readable description of the template</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a simple type template
 * TypeTemplate playerTemplate = new TypeTemplate() {
 *     public Type<?> apply(TypeFamily family) {
 *         return Type.product(
 *             Type.field("name", Type.STRING),
 *             Type.field("level", Type.INT)
 *         );
 *     }
 * };
 *
 * // Instantiate the template with an empty family
 * Type<?> playerType = playerTemplate.apply(TypeFamily.empty());
 *
 * // Define a recursive type template (e.g., a tree)
 * TypeTemplate treeTemplate = new TypeTemplate() {
 *     public Type<?> apply(TypeFamily family) {
 *         Type<?> selfRef = family.apply(0);  // Reference to self
 *         return Type.product(
 *             Type.field("value", Type.INT),
 *             Type.field("children", Type.list(selfRef))
 *         );
 *     }
 * };
 *
 * // Instantiate with recursive family
 * TypeFamily treeFamily = TypeFamily.recursive(treeTemplate::apply);
 * Type<?> treeType = treeFamily.apply(0);
 * }</pre>
 *
 * <h2>Relationship to TypeFamily</h2>
 * <p>A {@link TypeFamily} provides the type parameters that the template uses during
 * instantiation. For simple types, an empty family suffices. For parameterized or
 * recursive types, the family provides the necessary type arguments.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>TypeTemplate implementations should be stateless and thread-safe. The same
 * template can be instantiated multiple times with different families.</p>
 *
 * @author Erik Pförtner
 * @see Type
 * @see TypeFamily
 * @since 0.1.0
 */
public interface TypeTemplate {

    /**
     * Instantiates this template with the given type family to produce a concrete type.
     *
     * <p>This is the core operation of a type template. The template uses the type
     * family to resolve any type parameters or recursive references, producing a fully instantiated {@link Type} that
     * can be used in the data fixing system.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Simple template - doesn't use family parameters
     * TypeTemplate simple = family -> Type.STRING;
     * Type<?> stringType = simple.apply(TypeFamily.empty());
     *
     * // Parameterized template - uses family for parameters
     * TypeTemplate listOf = family -> Type.list(family.apply(0));
     * TypeFamily stringFamily = TypeFamily.of(Type.STRING);
     * Type<?> stringListType = listOf.apply(stringFamily);
     *
     * // Recursive template - family provides self-reference
     * TypeFamily treeFamily = TypeFamily.recursive(self ->
     *     Type.product(
     *         Type.field("value", Type.INT),
     *         Type.field("children", Type.list(self.apply(0)))
     *     )
     * );
     * Type<?> treeType = treeFamily.apply(0);
     * }</pre>
     *
     * @param family the type family providing type parameters and recursive references, must not be {@code null}
     * @return the fully instantiated type, never {@code null}
     * @throws NullPointerException      if {@code family} is {@code null}
     * @throws IndexOutOfBoundsException if the template accesses a type index not provided by the family
     */
    @NotNull
    Type<?> apply(@NotNull final TypeFamily family);

    /**
     * Returns a human-readable description of this template.
     *
     * <p>The description is used for debugging, logging, and error messages.
     * By default, it returns the simple class name, but implementations can override this to provide more meaningful
     * descriptions.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeTemplate playerTemplate = ...;
     * String desc = playerTemplate.describe();  // e.g., "TypeTemplate$$Lambda"
     *
     * TypeTemplate namedTemplate = playerTemplate.bind("Player");
     * String namedDesc = namedTemplate.describe();  // "Player=TypeTemplate$$Lambda"
     * }</pre>
     *
     * @return a human-readable description of this template, never {@code null}
     */
    @NotNull
    default String describe() {
        return getClass().getSimpleName();
    }

    /**
     * Binds a name to this template, enhancing its description.
     *
     * <p>The bound name becomes part of the template's description in the format
     * {@code "name=originalDescription"}. This is primarily useful for debugging and logging, making it easier to
     * identify templates in error messages.</p>
     *
     * <p>The template's behavior is unchanged—only its {@link #describe()} output
     * is affected.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeTemplate anonymous = family -> Type.product(
     *     Type.field("name", Type.STRING),
     *     Type.field("level", Type.INT)
     * );
     *
     * TypeTemplate named = anonymous.bind("Player");
     *
     * System.out.println(named.describe());  // "Player=..."
     *
     * // Behavior is identical
     * Type<?> type1 = anonymous.apply(family);
     * Type<?> type2 = named.apply(family);  // Same as type1
     * }</pre>
     *
     * @param name the name to bind to this template, must not be {@code null}
     * @return a new template with the bound name, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @NotNull
    default TypeTemplate bind(@NotNull final String name) {
        Preconditions.checkNotNull(name, "name must not be null");
        final TypeTemplate self = this;
        return new TypeTemplate() {
            @NotNull
            @Override
            public Type<?> apply(@NotNull final TypeFamily family) {
                Preconditions.checkNotNull(family, "family must not be null");
                return self.apply(family);
            }

            @NotNull
            @Override
            public String describe() {
                return name + "=" + self.describe();
            }
        };
    }
}
