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

package de.splatgames.aether.datafixers.spring.service;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MigrationResult}.
 */
@DisplayName("MigrationResult")
class MigrationResultTest {

    private static final DataVersion FROM_VERSION = new DataVersion(100);
    private static final DataVersion TO_VERSION = new DataVersion(200);
    private static final String DOMAIN = "game";
    private static final Duration DURATION = Duration.ofMillis(50);

    @Nested
    @DisplayName("Success Result")
    class SuccessResult {

        @Test
        @DisplayName("success() creates successful result")
        void successCreatesSuccessfulResult() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
        }

        @Test
        @DisplayName("getData() returns migrated data")
        void getDataReturnsMigratedData() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getData()).isSameAs(data);
        }

        @Test
        @DisplayName("getDataOptional() returns non-empty optional")
        void getDataOptionalReturnsNonEmpty() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getDataOptional()).isPresent().contains(data);
        }

        @Test
        @DisplayName("getError() returns empty optional")
        void getErrorReturnsEmpty() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getError()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Failure Result")
    class FailureResult {

        @Test
        @DisplayName("failure() creates failed result")
        void failureCreatesFailedResult() {
            RuntimeException error = new RuntimeException("Migration failed");

            MigrationResult result = MigrationResult.failure(
                    FROM_VERSION, TO_VERSION, DOMAIN, DURATION, error
            );

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("getData() throws for failed result")
        void getDataThrowsForFailedResult() {
            RuntimeException error = new RuntimeException("Migration failed");

            MigrationResult result = MigrationResult.failure(
                    FROM_VERSION, TO_VERSION, DOMAIN, DURATION, error
            );

            assertThatThrownBy(result::getData)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("failed");
        }

        @Test
        @DisplayName("getDataOptional() returns empty optional")
        void getDataOptionalReturnsEmpty() {
            RuntimeException error = new RuntimeException("Migration failed");

            MigrationResult result = MigrationResult.failure(
                    FROM_VERSION, TO_VERSION, DOMAIN, DURATION, error
            );

            assertThat(result.getDataOptional()).isEmpty();
        }

        @Test
        @DisplayName("getError() returns the error")
        void getErrorReturnsTheError() {
            RuntimeException error = new RuntimeException("Migration failed");

            MigrationResult result = MigrationResult.failure(
                    FROM_VERSION, TO_VERSION, DOMAIN, DURATION, error
            );

            assertThat(result.getError()).isPresent().contains(error);
        }
    }

    @Nested
    @DisplayName("Version Information")
    class VersionInformation {

        @Test
        @DisplayName("getFromVersion() returns source version")
        void getFromVersionReturnsSourceVersion() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getFromVersion()).isEqualTo(FROM_VERSION);
        }

        @Test
        @DisplayName("getToVersion() returns target version")
        void getToVersionReturnsTargetVersion() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getToVersion()).isEqualTo(TO_VERSION);
        }

        @Test
        @DisplayName("getVersionSpan() returns absolute difference")
        void getVersionSpanReturnsAbsoluteDifference() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getVersionSpan()).isEqualTo(100);
        }

        @Test
        @DisplayName("getVersionSpan() handles same version")
        void getVersionSpanHandlesSameVersion() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, FROM_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getVersionSpan()).isZero();
        }
    }

    @Nested
    @DisplayName("Metadata")
    class Metadata {

        @Test
        @DisplayName("getDomain() returns domain name")
        void getDomainReturnsDomainName() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getDomain()).isEqualTo(DOMAIN);
        }

        @Test
        @DisplayName("getDuration() returns migration duration")
        void getDurationReturnsMigrationDuration() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.getDuration()).isEqualTo(DURATION);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("toString() includes success status")
        void toStringIncludesSuccess() {
            TaggedDynamic data = mock(TaggedDynamic.class);

            MigrationResult result = MigrationResult.success(
                    data, FROM_VERSION, TO_VERSION, DOMAIN, DURATION
            );

            assertThat(result.toString())
                    .contains("success=true")
                    .contains("fromVersion=100")
                    .contains("toVersion=200")
                    .contains("domain='game'")
                    .contains("50ms");
        }

        @Test
        @DisplayName("toString() includes error for failure")
        void toStringIncludesErrorForFailure() {
            RuntimeException error = new RuntimeException("Test error");

            MigrationResult result = MigrationResult.failure(
                    FROM_VERSION, TO_VERSION, DOMAIN, DURATION, error
            );

            assertThat(result.toString())
                    .contains("success=false")
                    .contains("error=Test error");
        }
    }
}
