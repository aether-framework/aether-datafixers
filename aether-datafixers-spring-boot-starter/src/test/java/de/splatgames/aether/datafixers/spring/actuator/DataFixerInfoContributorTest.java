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
import org.springframework.boot.actuate.info.Info;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for {@link DataFixerInfoContributor}.
 */
@DisplayName("DataFixerInfoContributor")
class DataFixerInfoContributorTest {

    private DataFixerRegistry registry;
    private DataFixerInfoContributor contributor;
    private Info.Builder builder;

    @BeforeEach
    void setUp() {
        registry = new DataFixerRegistry();
        contributor = new DataFixerInfoContributor(registry);
        builder = new Info.Builder();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null registry")
        void rejectsNullRegistry() {
            assertThatThrownBy(() -> new DataFixerInfoContributor(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }
    }

    @Nested
    @DisplayName("Empty Registry")
    class EmptyRegistry {

        @Test
        @DisplayName("contributes with zero domains")
        void contributesWithZeroDomains() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            assertThat(details).containsEntry("domains", 0);
        }

        @Test
        @DisplayName("contributes empty domain details")
        void contributesEmptyDomainDetails() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            assertThat(domainDetails).isEmpty();
        }
    }

    @Nested
    @DisplayName("Single Domain")
    class SingleDomain {

        @BeforeEach
        void setupSingleDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);
        }

        @Test
        @DisplayName("contributes domain count")
        void contributesDomainCount() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            assertThat(details.get("domains")).isEqualTo(1);
        }

        @Test
        @DisplayName("contributes domain details")
        void contributesDomainDetails() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            assertThat(domainDetails).containsKey("game");

            @SuppressWarnings("unchecked")
            Map<String, Object> gameInfo = (Map<String, Object>) domainDetails.get("game");
            assertThat(gameInfo.get("currentVersion")).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Multiple Domains")
    class MultipleDomains {

        @BeforeEach
        void setupMultipleDomains() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            AetherDataFixer userFixer = mock(AetherDataFixer.class);
            AetherDataFixer worldFixer = mock(AetherDataFixer.class);

            when(gameFixer.currentVersion()).thenReturn(new DataVersion(200));
            when(userFixer.currentVersion()).thenReturn(new DataVersion(150));
            when(worldFixer.currentVersion()).thenReturn(new DataVersion(300));

            registry.register("game", gameFixer);
            registry.register("user", userFixer);
            registry.register("world", worldFixer);
        }

        @Test
        @DisplayName("contributes all domains count")
        void contributesAllDomainsCount() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            assertThat(details.get("domains")).isEqualTo(3);
        }

        @Test
        @DisplayName("contributes all domain details")
        void contributesAllDomainDetails() {
            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            assertThat(domainDetails).containsKeys("game", "user", "world");

            @SuppressWarnings("unchecked")
            Map<String, Object> gameInfo = (Map<String, Object>) domainDetails.get("game");
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) domainDetails.get("user");
            @SuppressWarnings("unchecked")
            Map<String, Object> worldInfo = (Map<String, Object>) domainDetails.get("world");

            assertThat(gameInfo.get("currentVersion")).isEqualTo(200);
            assertThat(userInfo.get("currentVersion")).isEqualTo(150);
            assertThat(worldInfo.get("currentVersion")).isEqualTo(300);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("handles fixer exception gracefully")
        void handlesFixerExceptionGracefully() {
            AetherDataFixer failingFixer = mock(AetherDataFixer.class);
            when(failingFixer.currentVersion()).thenThrow(new RuntimeException("Init failed"));
            registry.register("failing", failingFixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            @SuppressWarnings("unchecked")
            Map<String, Object> failingInfo = (Map<String, Object>) domainDetails.get("failing");

            assertThat(failingInfo.get("error")).isEqualTo("Init failed");
            assertThat(failingInfo).doesNotContainKey("currentVersion");
        }

        @Test
        @DisplayName("handles mixed healthy and unhealthy fixers")
        void handlesMixedHealthyAndUnhealthyFixers() {
            AetherDataFixer healthyFixer = mock(AetherDataFixer.class);
            AetherDataFixer unhealthyFixer = mock(AetherDataFixer.class);

            when(healthyFixer.currentVersion()).thenReturn(new DataVersion(200));
            when(unhealthyFixer.currentVersion()).thenThrow(new RuntimeException("Error"));

            registry.register("healthy", healthyFixer);
            registry.register("unhealthy", unhealthyFixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            @SuppressWarnings("unchecked")
            Map<String, Object> healthyInfo = (Map<String, Object>) domainDetails.get("healthy");
            @SuppressWarnings("unchecked")
            Map<String, Object> unhealthyInfo = (Map<String, Object>) domainDetails.get("unhealthy");

            assertThat(healthyInfo.get("currentVersion")).isEqualTo(200);
            assertThat(unhealthyInfo.get("error")).isNotNull();
        }

        @Test
        @DisplayName("handles null exception message")
        void handlesNullExceptionMessage() {
            AetherDataFixer failingFixer = mock(AetherDataFixer.class);
            when(failingFixer.currentVersion()).thenThrow(new RuntimeException());
            registry.register("failing", failingFixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            @SuppressWarnings("unchecked")
            Map<String, Object> failingInfo = (Map<String, Object>) domainDetails.get("failing");

            // Should handle null message gracefully
            assertThat(failingInfo).containsKey("error");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles version zero")
        void handlesVersionZero() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(0));
            registry.register("zero", fixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            @SuppressWarnings("unchecked")
            Map<String, Object> zeroInfo = (Map<String, Object>) domainDetails.get("zero");

            assertThat(zeroInfo.get("currentVersion")).isEqualTo(0);
        }

        // Note: DataVersion does not allow negative values, so we don't test for negative versions

        @Test
        @DisplayName("handles very large version")
        void handlesVeryLargeVersion() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(Integer.MAX_VALUE));
            registry.register("large", fixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            @SuppressWarnings("unchecked")
            Map<String, Object> largeInfo = (Map<String, Object>) domainDetails.get("large");

            assertThat(largeInfo.get("currentVersion")).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("handles special characters in domain name")
        void handlesSpecialCharactersInDomainName() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(100));
            registry.register("game-v2.1_test", fixer);

            contributor.contribute(builder);
            Info info = builder.build();

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) info.getDetails().get("aether-datafixers");

            @SuppressWarnings("unchecked")
            Map<String, Object> domainDetails = (Map<String, Object>) details.get("domainDetails");

            assertThat(domainDetails).containsKey("game-v2.1_test");
        }
    }

    @Nested
    @DisplayName("Info Structure")
    class InfoStructure {

        @Test
        @DisplayName("uses correct root key")
        void usesCorrectRootKey() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(100));
            registry.register("game", fixer);

            contributor.contribute(builder);
            Info info = builder.build();

            assertThat(info.getDetails()).containsKey("aether-datafixers");
        }

        @Test
        @DisplayName("structure is correct for serialization")
        void structureIsCorrectForSerialization() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            contributor.contribute(builder);
            Info info = builder.build();

            // Verify structure is serializable (no complex types)
            Object rootDetail = info.getDetails().get("aether-datafixers");
            assertThat(rootDetail).isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) rootDetail;

            assertThat(details.get("domains")).isInstanceOf(Integer.class);
            assertThat(details.get("domainDetails")).isInstanceOf(Map.class);
        }
    }
}
