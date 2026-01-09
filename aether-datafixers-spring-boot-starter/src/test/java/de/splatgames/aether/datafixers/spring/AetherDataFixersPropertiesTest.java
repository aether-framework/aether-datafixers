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

package de.splatgames.aether.datafixers.spring;

import de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties;
import de.splatgames.aether.datafixers.spring.config.DynamicOpsFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AetherDataFixersProperties}.
 */
@DisplayName("AetherDataFixersProperties")
class AetherDataFixersPropertiesTest {

    private AetherDataFixersProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AetherDataFixersProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("enabled defaults to true")
        void enabledDefaultsToTrue() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("defaultFormat defaults to GSON")
        void defaultFormatDefaultsToGson() {
            assertThat(properties.getDefaultFormat()).isEqualTo(DynamicOpsFormat.GSON);
        }

        @Test
        @DisplayName("defaultCurrentVersion defaults to null")
        void defaultCurrentVersionDefaultsToNull() {
            assertThat(properties.getDefaultCurrentVersion()).isNull();
        }

        @Test
        @DisplayName("domains defaults to empty map")
        void domainsDefaultsToEmptyMap() {
            assertThat(properties.getDomains()).isEmpty();
        }

        @Test
        @DisplayName("actuator defaults are set")
        void actuatorDefaultsAreSet() {
            assertThat(properties.getActuator()).isNotNull();
            assertThat(properties.getActuator().isIncludeSchemaDetails()).isTrue();
            assertThat(properties.getActuator().isIncludeFixDetails()).isTrue();
        }

        @Test
        @DisplayName("metrics defaults are set")
        void metricsDefaultsAreSet() {
            assertThat(properties.getMetrics()).isNotNull();
            assertThat(properties.getMetrics().isTiming()).isTrue();
            assertThat(properties.getMetrics().isCounting()).isTrue();
            assertThat(properties.getMetrics().getDomainTag()).isEqualTo("domain");
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("setEnabled works correctly")
        void setEnabledWorksCorrectly() {
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("setDefaultFormat works correctly")
        void setDefaultFormatWorksCorrectly() {
            properties.setDefaultFormat(DynamicOpsFormat.JACKSON);

            assertThat(properties.getDefaultFormat()).isEqualTo(DynamicOpsFormat.JACKSON);
        }

        @Test
        @DisplayName("setDefaultCurrentVersion works correctly")
        void setDefaultCurrentVersionWorksCorrectly() {
            properties.setDefaultCurrentVersion(200);

            assertThat(properties.getDefaultCurrentVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("setDomains works correctly")
        void setDomainsWorksCorrectly() {
            DataFixerDomainProperties domain = new DataFixerDomainProperties();
            domain.setCurrentVersion(200);

            properties.setDomains(Map.of("game", domain));

            assertThat(properties.getDomains()).containsKey("game");
            assertThat(properties.getDomains().get("game").getCurrentVersion()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("ActuatorProperties")
    class ActuatorPropertiesTests {

        @Test
        @DisplayName("setIncludeSchemaDetails works correctly")
        void setIncludeSchemaDetailsWorksCorrectly() {
            properties.getActuator().setIncludeSchemaDetails(false);

            assertThat(properties.getActuator().isIncludeSchemaDetails()).isFalse();
        }

        @Test
        @DisplayName("setIncludeFixDetails works correctly")
        void setIncludeFixDetailsWorksCorrectly() {
            properties.getActuator().setIncludeFixDetails(false);

            assertThat(properties.getActuator().isIncludeFixDetails()).isFalse();
        }

        @Test
        @DisplayName("can replace entire actuator properties")
        void canReplaceEntireActuatorProperties() {
            AetherDataFixersProperties.ActuatorProperties newActuator =
                    new AetherDataFixersProperties.ActuatorProperties();
            newActuator.setIncludeSchemaDetails(false);
            newActuator.setIncludeFixDetails(false);

            properties.setActuator(newActuator);

            assertThat(properties.getActuator().isIncludeSchemaDetails()).isFalse();
            assertThat(properties.getActuator().isIncludeFixDetails()).isFalse();
        }
    }

    @Nested
    @DisplayName("MetricsProperties")
    class MetricsPropertiesTests {

        @Test
        @DisplayName("setTiming works correctly")
        void setTimingWorksCorrectly() {
            properties.getMetrics().setTiming(false);

            assertThat(properties.getMetrics().isTiming()).isFalse();
        }

        @Test
        @DisplayName("setCounting works correctly")
        void setCountingWorksCorrectly() {
            properties.getMetrics().setCounting(false);

            assertThat(properties.getMetrics().isCounting()).isFalse();
        }

        @Test
        @DisplayName("setDomainTag works correctly")
        void setDomainTagWorksCorrectly() {
            properties.getMetrics().setDomainTag("custom-domain");

            assertThat(properties.getMetrics().getDomainTag()).isEqualTo("custom-domain");
        }

        @Test
        @DisplayName("can replace entire metrics properties")
        void canReplaceEntireMetricsProperties() {
            AetherDataFixersProperties.MetricsProperties newMetrics =
                    new AetherDataFixersProperties.MetricsProperties();
            newMetrics.setTiming(false);
            newMetrics.setCounting(false);
            newMetrics.setDomainTag("custom");

            properties.setMetrics(newMetrics);

            assertThat(properties.getMetrics().isTiming()).isFalse();
            assertThat(properties.getMetrics().isCounting()).isFalse();
            assertThat(properties.getMetrics().getDomainTag()).isEqualTo("custom");
        }
    }
}
