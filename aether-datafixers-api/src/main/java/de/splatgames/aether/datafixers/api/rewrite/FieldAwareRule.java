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

package de.splatgames.aether.datafixers.api.rewrite;

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A marker interface for {@link TypeRewriteRule} implementations that carry
 * structured field-level metadata.
 *
 * <p>{@code FieldAwareRule} is a separate interface rather than a default method
 * on {@link TypeRewriteRule} because {@code TypeRewriteRule} is a
 * {@link FunctionalInterface} and cannot have additional abstract methods.
 * Rules created by the field operation methods in {@link Rules} (such as
 * {@code renameField}, {@code removeField}, etc.) implement both
 * {@code TypeRewriteRule} and {@code FieldAwareRule}.</p>
 *
 * <h2>Usage in Diagnostics</h2>
 * <p>The diagnostic system uses {@code instanceof FieldAwareRule} to detect
 * whether a rule carries field-level metadata. When present, the metadata is
 * included in the {@link RuleApplication}
 * record for that rule execution.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TypeRewriteRule rule = Rules.renameField(ops, "oldName", "newName");
 *
 * if (rule instanceof FieldAwareRule fieldAware) {
 *     List<FieldOperation> fieldOps = fieldAware.fieldOperations();
 *     // fieldOps contains: [FieldOperation.rename("oldName", "newName")]
 * }
 * }</pre>
 *
 * <h2>Implementing Custom Field-Aware Rules</h2>
 * <p>Custom rules can implement this interface to participate in field-level
 * diagnostics:</p>
 * <pre>{@code
 * public class MyCustomRule implements TypeRewriteRule, FieldAwareRule {
 *     @Override
 *     public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
 *         // rule logic
 *     }
 *
 *     @Override
 *     public List<FieldOperation> fieldOperations() {
 *         return List.of(FieldOperation.transform("myField"));
 *     }
 * }
 * }</pre>
 *
 * @author Erik Pförtner
 * @see TypeRewriteRule
 * @see FieldOperation
 * @see Rules
 * @since 1.0.0
 */
public interface FieldAwareRule {

    /**
     * Returns the list of field-level operations that this rule performs.
     *
     * <p>The returned list describes which fields are affected and how. For
     * simple rules (e.g., a single {@code renameField}), this typically contains
     * one entry. For batch rules (e.g., {@code renameFields} with multiple
     * mappings), this may contain multiple entries.</p>
     *
     * <p>The returned list must be immutable.</p>
     *
     * @return an unmodifiable list of field operations, never {@code null}
     */
    @NotNull
    List<FieldOperation> fieldOperations();
}
