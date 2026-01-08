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

package de.splatgames.aether.datafixers.spring.actuator;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DataFixerHealthIndicator}.
 */
@DisplayName("DataFixerHealthIndicator")
class DataFixerHealthIndicatorTest {

    private DataFixerRegistry registry;
    private DataFixerHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        registry = new DataFixerRegistry();
        indicator = new DataFixerHealthIndicator(registry);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null registry")
        void rejectsNullRegistry() {
            assertThatThrownBy(() -> new DataFixerHealthIndicator(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }
    }

    @Nested
    @DisplayName("Empty Registry")
    class EmptyRegistry {

        @Test
        @DisplayName("returns UNKNOWN status when no fixers registered")
        void returnsUnknownWhenNoFixers() {
            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
            assertThat(health.getDetails()).containsKey("message");
        }

        @Test
        @DisplayName("includes appropriate message")
        void includesAppropriateMessage() {
            Health health = indicator.health();

            assertThat(health.getDetails().get("message"))
                    .isEqualTo("No DataFixers registered");
        }
    }

    @Nested
    @DisplayName("Healthy Fixers")
    class HealthyFixers {

        @Test
        @DisplayName("returns UP status when all fixers healthy")
        void returnsUpWhenAllHealthy() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("includes total domains count")
        void includesTotalDomainsCount() {
            AetherDataFixer fixer1 = mock(AetherDataFixer.class);
            AetherDataFixer fixer2 = mock(AetherDataFixer.class);
            when(fixer1.currentVersion()).thenReturn(new DataVersion(200));
            when(fixer2.currentVersion()).thenReturn(new DataVersion(150));
            registry.register("game", fixer1);
            registry.register("user", fixer2);

            Health health = indicator.health();

            assertThat(health.getDetails().get("totalDomains")).isEqualTo(2);
        }

        @Test
        @DisplayName("includes domain status details")
        void includesDomainStatusDetails() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            Health health = indicator.health();

            assertThat(health.getDetails())
                    .containsEntry("game.status", "UP")
                    .containsEntry("game.currentVersion", 200);
        }

        @Test
        @DisplayName("includes all domain details")
        void includesAllDomainDetails() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            AetherDataFixer userFixer = mock(AetherDataFixer.class);
            when(gameFixer.currentVersion()).thenReturn(new DataVersion(200));
            when(userFixer.currentVersion()).thenReturn(new DataVersion(150));
            registry.register("game", gameFixer);
            registry.register("user", userFixer);

            Health health = indicator.health();

            assertThat(health.getDetails())
                    .containsEntry("game.status", "UP")
                    .containsEntry("game.currentVersion", 200)
                    .containsEntry("user.status", "UP")
                    .containsEntry("user.currentVersion", 150);
        }
    }

    @Nested
    @DisplayName("Unhealthy Fixers")
    class UnhealthyFixers {

        @Test
        @DisplayName("returns DOWN status when fixer throws exception")
        void returnsDownWhenFixerThrows() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenThrow(new RuntimeException("Test error"));
            registry.register("game", fixer);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        }

        @Test
        @DisplayName("includes error details")
        void includesErrorDetails() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenThrow(new RuntimeException("Initialization failed"));
            registry.register("game", fixer);

            Health health = indicator.health();

            assertThat(health.getDetails())
                    .containsEntry("game.status", "DOWN")
                    .containsEntry("game.error", "Initialization failed");
        }

        @Test
        @DisplayName("still includes total domains count on failure")
        void includesTotalDomainsOnFailure() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenThrow(new RuntimeException("Error"));
            registry.register("game", fixer);

            Health health = indicator.health();

            assertThat(health.getDetails().get("totalDomains")).isEqualTo(1);
        }
    }
}
