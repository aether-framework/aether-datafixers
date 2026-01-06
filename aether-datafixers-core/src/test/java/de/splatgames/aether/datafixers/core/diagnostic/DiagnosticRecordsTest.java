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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.diagnostic.FixExecution;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RuleApplication} and {@link FixExecution} records.
 */
@DisplayName("Diagnostic Records")
class DiagnosticRecordsTest {

    @Nested
    @DisplayName("RuleApplication")
    class RuleApplicationTests {

        @Test
        @DisplayName("creates record with all fields")
        void createsRecordWithAllFields() {
            Instant now = Instant.now();
            Duration duration = Duration.ofMillis(50);

            RuleApplication rule = new RuleApplication(
                    "rename_field",
                    "player",
                    now,
                    duration,
                    true,
                    "Renamed playerName to name"
            );

            assertThat(rule.ruleName()).isEqualTo("rename_field");
            assertThat(rule.typeName()).isEqualTo("player");
            assertThat(rule.timestamp()).isEqualTo(now);
            assertThat(rule.duration()).isEqualTo(duration);
            assertThat(rule.matched()).isTrue();
            assertThat(rule.description()).isEqualTo("Renamed playerName to name");
        }

        @Test
        @DisplayName("of() factory creates record without description")
        void ofFactoryCreatesRecordWithoutDescription() {
            Instant now = Instant.now();
            Duration duration = Duration.ofMillis(25);

            RuleApplication rule = RuleApplication.of(
                    "add_field",
                    "entity",
                    now,
                    duration,
                    false
            );

            assertThat(rule.ruleName()).isEqualTo("add_field");
            assertThat(rule.typeName()).isEqualTo("entity");
            assertThat(rule.matched()).isFalse();
            assertThat(rule.description()).isNull();
            assertThat(rule.descriptionOpt()).isEmpty();
        }

        @Test
        @DisplayName("descriptionOpt() returns Optional with description")
        void descriptionOptReturnsOptionalWithDescription() {
            RuleApplication rule = new RuleApplication(
                    "rule", "type", Instant.now(), Duration.ZERO, true, "desc"
            );

            assertThat(rule.descriptionOpt()).contains("desc");
        }

        @Test
        @DisplayName("durationMillis() returns duration in milliseconds")
        void durationMillisReturnsDurationInMilliseconds() {
            RuleApplication rule = RuleApplication.of(
                    "rule", "type", Instant.now(), Duration.ofMillis(42), true
            );

            assertThat(rule.durationMillis()).isEqualTo(42);
        }

        @Test
        @DisplayName("toSummary() returns formatted string")
        void toSummaryReturnsFormattedString() {
            RuleApplication matched = RuleApplication.of(
                    "rename_field", "player", Instant.now(), Duration.ofMillis(10), true
            );
            RuleApplication skipped = RuleApplication.of(
                    "add_field", "entity", Instant.now(), Duration.ofMillis(5), false
            );

            assertThat(matched.toSummary()).contains("rename_field", "player", "matched", "10ms");
            assertThat(skipped.toSummary()).contains("add_field", "entity", "skipped", "5ms");
        }

        @Test
        @DisplayName("throws NullPointerException for null required fields")
        void throwsNullPointerExceptionForNullRequiredFields() {
            assertThatThrownBy(() -> new RuleApplication(null, "type", Instant.now(), Duration.ZERO, true, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RuleApplication("rule", null, Instant.now(), Duration.ZERO, true, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RuleApplication("rule", "type", null, Duration.ZERO, true, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RuleApplication("rule", "type", Instant.now(), null, true, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("FixExecution")
    class FixExecutionTests {

        @Test
        @DisplayName("creates record with all fields")
        void createsRecordWithAllFields() {
            Instant start = Instant.now();
            Duration duration = Duration.ofMillis(100);
            List<RuleApplication> rules = List.of(
                    RuleApplication.of("rule1", "type", start, Duration.ofMillis(50), true),
                    RuleApplication.of("rule2", "type", start.plusMillis(50), Duration.ofMillis(50), false)
            );

            FixExecution fix = new FixExecution(
                    "migrate_player",
                    new DataVersion(1),
                    new DataVersion(2),
                    start,
                    duration,
                    rules,
                    "{\"before\":true}",
                    "{\"after\":true}"
            );

            assertThat(fix.fixName()).isEqualTo("migrate_player");
            assertThat(fix.fromVersion()).isEqualTo(new DataVersion(1));
            assertThat(fix.toVersion()).isEqualTo(new DataVersion(2));
            assertThat(fix.startTime()).isEqualTo(start);
            assertThat(fix.duration()).isEqualTo(duration);
            assertThat(fix.ruleApplications()).hasSize(2);
            assertThat(fix.beforeSnapshot()).isEqualTo("{\"before\":true}");
            assertThat(fix.afterSnapshot()).isEqualTo("{\"after\":true}");
        }

        @Test
        @DisplayName("beforeSnapshotOpt() and afterSnapshotOpt() return Optionals")
        void snapshotOptReturnsOptionals() {
            FixExecution withSnapshots = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ZERO, List.of(),
                    "before", "after"
            );
            FixExecution withoutSnapshots = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ZERO, List.of(),
                    null, null
            );

            assertThat(withSnapshots.beforeSnapshotOpt()).contains("before");
            assertThat(withSnapshots.afterSnapshotOpt()).contains("after");
            assertThat(withoutSnapshots.beforeSnapshotOpt()).isEmpty();
            assertThat(withoutSnapshots.afterSnapshotOpt()).isEmpty();
        }

        @Test
        @DisplayName("durationMillis() returns duration in milliseconds")
        void durationMillisReturnsDurationInMilliseconds() {
            FixExecution fix = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ofMillis(123), List.of(),
                    null, null
            );

            assertThat(fix.durationMillis()).isEqualTo(123);
        }

        @Test
        @DisplayName("ruleCount() returns number of rule applications")
        void ruleCountReturnsNumberOfRuleApplications() {
            List<RuleApplication> rules = List.of(
                    RuleApplication.of("rule1", "type", Instant.now(), Duration.ZERO, true),
                    RuleApplication.of("rule2", "type", Instant.now(), Duration.ZERO, true),
                    RuleApplication.of("rule3", "type", Instant.now(), Duration.ZERO, false)
            );

            FixExecution fix = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ZERO, rules,
                    null, null
            );

            assertThat(fix.ruleCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("matchedRuleCount() returns number of matched rules")
        void matchedRuleCountReturnsNumberOfMatchedRules() {
            List<RuleApplication> rules = List.of(
                    RuleApplication.of("rule1", "type", Instant.now(), Duration.ZERO, true),
                    RuleApplication.of("rule2", "type", Instant.now(), Duration.ZERO, true),
                    RuleApplication.of("rule3", "type", Instant.now(), Duration.ZERO, false)
            );

            FixExecution fix = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ZERO, rules,
                    null, null
            );

            assertThat(fix.matchedRuleCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("toSummary() returns formatted string")
        void toSummaryReturnsFormattedString() {
            List<RuleApplication> rules = List.of(
                    RuleApplication.of("rule1", "type", Instant.now(), Duration.ZERO, true),
                    RuleApplication.of("rule2", "type", Instant.now(), Duration.ZERO, false)
            );

            FixExecution fix = new FixExecution(
                    "migrate_player", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ofMillis(50), rules,
                    null, null
            );

            String summary = fix.toSummary();
            assertThat(summary).contains("migrate_player");
            assertThat(summary).contains("v1");
            assertThat(summary).contains("v2");
            assertThat(summary).contains("50ms");
            assertThat(summary).contains("2 rules");
            assertThat(summary).contains("1 matched");
        }

        @Test
        @DisplayName("ruleApplications list is immutable")
        void ruleApplicationsListIsImmutable() {
            List<RuleApplication> rules = List.of(
                    RuleApplication.of("rule1", "type", Instant.now(), Duration.ZERO, true)
            );

            FixExecution fix = new FixExecution(
                    "fix", new DataVersion(1), new DataVersion(2),
                    Instant.now(), Duration.ZERO, rules,
                    null, null
            );

            assertThatThrownBy(() -> fix.ruleApplications().add(
                    RuleApplication.of("rule2", "type", Instant.now(), Duration.ZERO, true)
            )).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
