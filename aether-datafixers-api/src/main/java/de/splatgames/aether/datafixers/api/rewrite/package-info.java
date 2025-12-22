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
 * Type rewrite rules for schema transformations.
 *
 * <p>This package provides the infrastructure for defining and applying
 * transformation rules to typed data. Rewrite rules are the building blocks
 * of data fixes, describing how to transform data from one schema to another.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} -
 *       Represents a transformation that can be applied to typed data. Rules
 *       are composable and can be combined to create complex migrations.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.Rules} - Factory class
 *       providing common rewrite rule combinators for typical data transformations.</li>
 * </ul>
 *
 * <h2>Common Rules</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.rewrite.Rules} class provides
 * factory methods for common transformations:</p>
 * <ul>
 *   <li>{@code Rules.renameField(oldName, newName)} - Renames a field</li>
 *   <li>{@code Rules.addField(name, defaultValue)} - Adds a new field with default</li>
 *   <li>{@code Rules.removeField(name)} - Removes a field</li>
 *   <li>{@code Rules.transformField(name, function)} - Transforms a field's value</li>
 *   <li>{@code Rules.restructure(...)} - Complex structural changes</li>
 * </ul>
 *
 * <h2>Usage in DataFix</h2>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *     public PlayerV1ToV2Fix() {
 *         super(new DataVersion(100), new DataVersion(110), TypeReferences.PLAYER);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema input, Schema output) {
 *         // Combine multiple rules
 *         return Rules.sequence(
 *             Rules.renameField("playerName", "name"),
 *             Rules.renameField("xp", "experience"),
 *             Rules.transformField("gameMode", this::gameModeToString),
 *             Rules.restructure(this::groupPositionFields)
 *         );
 *     }
 *
 *     private Dynamic<?> groupPositionFields(Dynamic<?> data) {
 *         // Move x, y, z into a nested "position" object
 *         return data.createMap(Map.of(
 *             "position", data.createMap(Map.of(
 *                 "x", data.get("x").result().orElse(data.createDouble(0)),
 *                 "y", data.get("y").result().orElse(data.createDouble(0)),
 *                 "z", data.get("z").result().orElse(data.createDouble(0))
 *             ))
 *         )).merge(data.remove("x").remove("y").remove("z"));
 *     }
 * }
 * }</pre>
 *
 * <h2>Rule Composition</h2>
 * <p>Rules can be composed in several ways:</p>
 * <ul>
 *   <li>{@code Rules.sequence(rule1, rule2, ...)} - Apply rules in order</li>
 *   <li>{@code Rules.all(rule)} - Apply rule to all matching types</li>
 *   <li>{@code Rules.one(rule)} - Apply rule to first matching type</li>
 *   <li>{@code rule1.andThen(rule2)} - Chain two rules</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.rewrite;
