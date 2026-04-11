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

package de.splatgames.aether.datafixers.core.diagnostic;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A wrapper around {@link TypeRewriteRule} that captures diagnostic information
 * about rule applications.
 *
 * <p>This wrapper intercepts the {@link #rewrite(Type, Typed)} method to record
 * timing and match status for each rule application. The captured data is added
 * to the {@link DiagnosticContext}'s report builder.</p>
 *
 * <h2>Usage</h2>
 * <p>This wrapper is applied automatically by {@code SchemaDataFix} when a
 * {@link DiagnosticContext} is provided. Users typically don't need to use
 * this class directly.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This wrapper is stateless regarding the rule itself but writes to the
 * provided {@link DiagnosticContext}. It should be used within a single
 * migration operation.</p>
 *
 * @author Erik Pförtner
 * @see DiagnosticContext
 * @see RuleApplication
 * @since 0.2.0
 */
public final class DiagnosticRuleWrapper implements TypeRewriteRule {

    /**
     * The underlying rule that performs the actual rewrite logic.
     */
    private final TypeRewriteRule delegate;

    /**
     * The diagnostic context used for recording rule application events.
     */
    private final DiagnosticContext context;

    /**
     * Creates a new diagnostic wrapper around a rule.
     *
     * @param delegate the rule to wrap, must not be {@code null}
     * @param context  the diagnostic context for recording, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public DiagnosticRuleWrapper(
            @NotNull final TypeRewriteRule delegate,
            @NotNull final DiagnosticContext context
    ) {
        Preconditions.checkNotNull(delegate, "delegate must not be null");
        Preconditions.checkNotNull(context, "context must not be null");

        this.delegate = delegate;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Intercepts the rewrite call to capture diagnostic information including
     * timing, match status, and field-level operation metadata from
     * {@link FieldAwareRule} delegates. If {@code captureRuleDetails} is disabled,
     * delegates directly without recording.</p>
     *
     * @param type  the type descriptor of the input, must not be {@code null}
     * @param input the typed value to potentially rewrite, must not be {@code null}
     * @return an {@link Optional} containing the rewritten value if the rule applies,
     *         or {@link Optional#empty()} if it doesn't match; never {@code null}
     * @throws NullPointerException if {@code type} or {@code input} is {@code null}
     */
    @Override
    @NotNull
    public Optional<Typed<?>> rewrite(
            @NotNull final Type<?> type,
            @NotNull final Typed<?> input
    ) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        // Only capture details if configured to do so
        if (!this.context.options().captureRuleDetails()) {
            return this.delegate.rewrite(type, input);
        }

        final Instant timestamp = Instant.now();
        final long startNano = System.nanoTime();
        final Optional<Typed<?>> result = this.delegate.rewrite(type, input);
        final Duration duration = Duration.ofNanos(System.nanoTime() - startNano);

        final List<FieldOperation> fieldOps;
        if (this.context.options().captureFieldDetails()
                && this.delegate instanceof FieldAwareRule fieldAware) {
            fieldOps = fieldAware.fieldOperations();
        } else {
            fieldOps = List.of();
        }

        final RuleApplication application = new RuleApplication(
                this.delegate.toString(),
                type.describe(),
                timestamp,
                duration,
                result.isPresent(),
                null,
                fieldOps
        );

        this.context.reportBuilder().recordRuleApplication(application);

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #rewrite(Type, Typed)} to ensure diagnostics are captured,
     * returning the original input if the rule doesn't match.</p>
     *
     * @param input the typed value to transform, must not be {@code null}
     * @return the rewritten result if matched, or the original input unchanged; never {@code null}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    @Override
    @NotNull
    public Typed<?> apply(@NotNull final Typed<?> input) {
        Preconditions.checkNotNull(input, "input must not be null");
        // Delegate to rewrite to capture diagnostics
        return this.rewrite(input.type(), input).orElse(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #rewrite(Type, Typed)} to ensure diagnostics are captured,
     * throwing if the rule doesn't match.</p>
     *
     * @param input the typed value to transform, must not be {@code null}
     * @return the rewritten result, never {@code null}
     * @throws IllegalStateException if the rule doesn't match the input type
     * @throws NullPointerException  if {@code input} is {@code null}
     */
    @Override
    @NotNull
    public Typed<?> applyOrThrow(@NotNull final Typed<?> input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.rewrite(input.type(), input)
                .orElseThrow(() -> new IllegalStateException(
                        "Rule did not match: " + input.type().describe()
                ));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Composes this rule with the next rule, wrapping the result in a new
     * {@link DiagnosticRuleWrapper} to maintain diagnostic capture across compositions.</p>
     *
     * @param next the rule to apply after this rule succeeds, must not be {@code null}
     * @return a composed diagnostic rule applying both rules in sequence, never {@code null}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @Override
    @NotNull
    public TypeRewriteRule andThen(@NotNull final TypeRewriteRule next) {
        Preconditions.checkNotNull(next, "next must not be null");
        // Wrap the composition result to maintain diagnostics
        return new DiagnosticRuleWrapper(this.delegate.andThen(unwrap(next)), this.context);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a fallback composition, wrapping the result in a new
     * {@link DiagnosticRuleWrapper} to maintain diagnostic capture.</p>
     *
     * @param fallback the rule to try if this rule doesn't match, must not be {@code null}
     * @return a composed diagnostic rule with fallback behavior, never {@code null}
     * @throws NullPointerException if {@code fallback} is {@code null}
     */
    @Override
    @NotNull
    public TypeRewriteRule orElse(@NotNull final TypeRewriteRule fallback) {
        Preconditions.checkNotNull(fallback, "fallback must not be null");
        return new DiagnosticRuleWrapper(this.delegate.orElse(unwrap(fallback)), this.context);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Makes this rule always succeed, wrapping the result in a new
     * {@link DiagnosticRuleWrapper} to maintain diagnostic capture.</p>
     *
     * @return a diagnostic rule that always succeeds, never {@code null}
     */
    @Override
    @NotNull
    public TypeRewriteRule orKeep() {
        return new DiagnosticRuleWrapper(this.delegate.orKeep(), this.context);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a type filter, wrapping the result in a new {@link DiagnosticRuleWrapper}
     * to maintain diagnostic capture.</p>
     *
     * @param targetType the type that must match for the rule to apply, must not be {@code null}
     * @return a filtered diagnostic rule, never {@code null}
     * @throws NullPointerException if {@code targetType} is {@code null}
     */
    @Override
    @NotNull
    public TypeRewriteRule ifType(@NotNull final Type<?> targetType) {
        Preconditions.checkNotNull(targetType, "targetType must not be null");
        return new DiagnosticRuleWrapper(this.delegate.ifType(targetType), this.context);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a name for debugging, wrapping the result in a new {@link DiagnosticRuleWrapper}
     * to maintain diagnostic capture.</p>
     *
     * @param name a descriptive name for this rule, must not be {@code null}
     * @return a named diagnostic rule, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @Override
    @NotNull
    public TypeRewriteRule named(@NotNull final String name) {
        Preconditions.checkNotNull(name, "name must not be null");
        return new DiagnosticRuleWrapper(this.delegate.named(name), this.context);
    }

    /**
     * Returns the string representation of the underlying delegate rule.
     *
     * @return the delegate rule's string representation, never {@code null}
     */
    @Override
    @NotNull
    public String toString() {
        return this.delegate.toString();
    }

    /**
     * Wraps a rule with diagnostics if not already wrapped.
     *
     * @param rule    the rule to wrap
     * @param context the diagnostic context
     * @return the wrapped rule
     */
    @NotNull
    public static TypeRewriteRule wrap(
            @NotNull final TypeRewriteRule rule,
            @NotNull final DiagnosticContext context
    ) {
        Preconditions.checkNotNull(rule, "rule must not be null");
        Preconditions.checkNotNull(context, "context must not be null");
        if (rule instanceof DiagnosticRuleWrapper) {
            return rule;
        }
        return new DiagnosticRuleWrapper(rule, context);
    }

    /**
     * Unwraps a diagnostic wrapper to get the underlying rule.
     *
     * @param rule the potentially wrapped rule
     * @return the underlying rule
     */
    @NotNull
    private static TypeRewriteRule unwrap(@NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(rule, "rule must not be null");
        if (rule instanceof DiagnosticRuleWrapper wrapper) {
            return wrapper.delegate;
        }
        return rule;
    }
}
