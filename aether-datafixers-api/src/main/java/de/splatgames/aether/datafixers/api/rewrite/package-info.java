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
 * transformation rules to typed data. Rewrite rules are the building blocks of
 * data fixes, describing how to transform data from one schema version to the
 * next in a composable, format-agnostic way.</p>
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} —
 *       Abstract rule that can be applied to {@link de.splatgames.aether.datafixers.api.type.Typed}
 *       data. Rules are stateless and thread-safe; they compose via
 *       {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule#andThen andThen}
 *       or via the combinators in {@link de.splatgames.aether.datafixers.api.rewrite.Rules}.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.Rules} — Canonical
 *       factory of combinators and field-level operations. The 1.0.0 API exposes
 *       a rich DSL covering composition, traversal, top-level and batch field
 *       operations, path-based (nested) operations, and conditionals.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule} —
 *       Marker interface implemented by every rule returned from the field-level
 *       factories. Exposes structured
 *       {@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperation}
 *       metadata that powers the diagnostic system.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite.BatchTransform} —
 *       Imperative builder backing {@link de.splatgames.aether.datafixers.api.rewrite.Rules#batch(de.splatgames.aether.datafixers.api.dynamic.DynamicOps, java.util.function.Consumer)
 *       Rules.batch}; collects many field operations into a single pass over a
 *       {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}.</li>
 * </ul>
 *
 * <h2>Rule Categories</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.rewrite.Rules} factory is
 * organised into eight categories. The headings below link into the class-level
 * documentation of {@code Rules} for exhaustive per-method detail:</p>
 * <ol>
 *   <li><b>Basic composition</b> — {@code seq}, {@code seqAll}, {@code choice},
 *       {@code checkOnce}, {@code tryOnce}.</li>
 *   <li><b>Traversal</b> — {@code all}, {@code one}, {@code everywhere},
 *       {@code bottomUp}, {@code topDown} (both {@code DynamicOps}- and
 *       {@code Type}-based overloads).</li>
 *   <li><b>Type filters and type-aware updates</b> — {@code ifType},
 *       {@code transformType}, {@code updateAt}.</li>
 *   <li><b>Top-level field operations</b> — {@code renameField},
 *       {@code removeField}, {@code addField}, {@code transformField},
 *       {@code setField}.</li>
 *   <li><b>Batch field operations</b> — {@code renameFields},
 *       {@code removeFields}, {@code groupFields}, {@code flattenField},
 *       {@code moveField}, {@code copyField}, {@code batch}.</li>
 *   <li><b>Path-based (nested) field operations</b> — {@code transformFieldAt},
 *       {@code renameFieldAt}, {@code removeFieldAt}, {@code addFieldAt}.</li>
 *   <li><b>Conditional field operations</b> — {@code ifFieldExists},
 *       {@code ifFieldMissing}, {@code ifFieldEquals},
 *       {@code conditionalTransform}.</li>
 *   <li><b>Escape hatches and utilities</b> — {@code dynamicTransform},
 *       {@code noop}, {@code log}.</li>
 * </ol>
 *
 * <h2>Usage in DataFix</h2>
 * <p>A typical {@link de.splatgames.aether.datafixers.api.fix.DataFix} builds
 * its rule by composing high-level combinators. Prefer the dedicated helpers
 * (such as {@code groupFields} or {@code renameFields}) over manual
 * {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic} manipulation —
 * the helpers are declarative, carry diagnostic metadata, and handle the usual
 * edge cases (missing fields, non-map inputs) for you.</p>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *     public PlayerV1ToV2Fix(SchemaRegistry schemas) {
 *         super("player_v100_to_v110",
 *               new DataVersion(100), new DataVersion(110), schemas);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema input, Schema output) {
 *         DynamicOps<JsonElement> ops = GsonOps.INSTANCE;
 *         return Rules.seq(
 *             // Rename legacy field names in one pass
 *             Rules.renameFields(ops, Map.of(
 *                 "playerName", "name",
 *                 "xp",         "experience"
 *             )),
 *             // Collapse the flat x/y/z into a nested "position" object
 *             Rules.groupFields(ops, "position", "x", "y", "z"),
 *             // Transform a single field in place
 *             Rules.transformField(ops, "gameMode",
 *                 d -> d.createString(gameModeToString(d))),
 *             // Add a default only when the field is absent
 *             Rules.ifFieldMissing(ops, "version",
 *                 Rules.setField(ops, "version",
 *                     new Dynamic<>(ops, ops.createInt(1))))
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>Rules compose in several ways. Prefer {@code seq}/{@code seqAll}/{@code choice}
 * over manual chaining — they aggregate field-operation metadata for
 * diagnostics, while {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule#andThen andThen}
 * is the raw two-argument building block used internally:</p>
 * <ul>
 *   <li>{@code Rules.seq(rule1, rule2, ...)} — strict AND; all rules must
 *       succeed.</li>
 *   <li>{@code Rules.seqAll(rule1, rule2, ...)} — forgiving AND; tolerates
 *       individual failures.</li>
 *   <li>{@code Rules.choice(rule1, rule2, ...)} — first successful rule wins
 *       (OR).</li>
 *   <li>{@code rule1.andThen(rule2)} — raw sequential composition of two rules
 *       without metadata aggregation; use {@code seq} when you want diagnostics.</li>
 *   <li>{@code Rules.all(rule)}, {@code Rules.one(rule)},
 *       {@code Rules.everywhere(rule)}, {@code Rules.topDown(rule)},
 *       {@code Rules.bottomUp(rule)} — apply a rule across a structure.</li>
 * </ul>
 *
 * <h2>Field-Aware Diagnostics</h2>
 * <p>Every rule returned by the field-level factories in
 * {@link de.splatgames.aether.datafixers.api.rewrite.Rules} implements
 * {@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule}. When a
 * migration runs inside a
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}, the
 * produced {@link de.splatgames.aether.datafixers.api.diagnostic.MigrationReport}
 * attributes every field change — renames, removes, additions, transforms,
 * groupings, and conditionals — to the rule that caused it. Custom hand-written
 * {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} instances
 * can opt in by also implementing {@code FieldAwareRule} and returning their
 * operations from {@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule#fieldOperations()}.</p>
 *
 * @see de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
 * @see de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @see de.splatgames.aether.datafixers.api.diagnostic.MigrationReport
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.rewrite;
