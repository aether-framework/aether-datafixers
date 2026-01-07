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

package de.splatgames.aether.datafixers.cli.command;

import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidateCommand")
class ValidateCommandTest {

    @TempDir
    Path tempDir;

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    @DisplayName("Required options")
    class RequiredOptions {

        @Test
        @DisplayName("requires --type option")
        void requiresTypeOption() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires --bootstrap option")
        void requiresBootstrapOption() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--to", "10");

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires --to option")
        void requiresToOption() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName());

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires at least one input file")
        void requiresInputFiles() {
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isNotEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Validation results")
    class ValidationResults {

        @Test
        @DisplayName("returns 0 for up-to-date file")
        void returnsZeroForUpToDate() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 10}", "uptodate.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("OK:");
        }

        @Test
        @DisplayName("returns 0 for file with version higher than target")
        void returnsZeroForHigherVersion() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 15}", "newer.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("OK:");
        }

        @Test
        @DisplayName("returns 2 for file needing migration")
        void returnsTwoForNeedsMigration() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1}", "old.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(2);
            assertThat(outContent.toString()).contains("MIGRATE:");
        }

        @Test
        @DisplayName("returns 1 for invalid file")
        void returnsOneForInvalidFile() throws IOException {
            final Path file = createTestFile("not valid json", "invalid.txt");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(1);
            assertThat(outContent.toString()).contains("ERROR:");
        }

        @Test
        @DisplayName("returns 1 for file missing version field")
        void returnsOneForMissingVersion() throws IOException {
            final Path file = createTestFile("{\"name\": \"test\"}", "noversion.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(1);
            assertThat(outContent.toString()).contains("ERROR:");
        }
    }

    @Nested
    @DisplayName("Multiple files")
    class MultipleFiles {

        @Test
        @DisplayName("validates all files and returns summary")
        void validatesAllFiles() throws IOException {
            final Path upToDate = createTestFile("{\"dataVersion\": 10}", "uptodate.json");
            final Path needsMigration = createTestFile("{\"dataVersion\": 1}", "old.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    upToDate.toString(),
                    needsMigration.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(2); // Has files needing migration
            final String output = outContent.toString();
            assertThat(output).contains("Summary:");
            assertThat(output).contains("1 up-to-date");
            assertThat(output).contains("1 need migration");
        }

        @Test
        @DisplayName("returns 1 when errors occur regardless of valid files")
        void returnsOneOnError() throws IOException {
            final Path valid = createTestFile("{\"dataVersion\": 10}", "valid.json");
            final Path invalid = createTestFile("not json", "invalid.txt");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    valid.toString(),
                    invalid.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Custom version field")
    class CustomVersionField {

        @Test
        @DisplayName("extracts version from custom field path")
        void usesCustomVersionField() throws IOException {
            final Path file = createTestFile("{\"meta\":{\"version\":10}}", "custom.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10",
                    "--version-field", "meta.version");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("OK:");
        }
    }

    @Nested
    @DisplayName("Format handling")
    class FormatHandling {

        @Test
        @DisplayName("uses json-gson by default")
        void usesGsonByDefault() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 10}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("accepts json-jackson format")
        void acceptsJacksonFormat() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 10}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10",
                    "--format", "json-jackson");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("returns error for unknown format")
        void returnsErrorForUnknownFormat() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 10}", "test.json");
            final CommandLine cmd = new CommandLine(new ValidateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "test",
                    "--bootstrap", TestBootstrapForValidate.class.getName(),
                    "--to", "10",
                    "--format", "unknown");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Unknown format");
        }
    }

    @Nested
    @DisplayName("Help option")
    class HelpOption {

        @Test
        @DisplayName("displays help with --help")
        void displaysHelp() {
            final CommandLine cmd = new CommandLine(new ValidateCommand());
            cmd.execute("--help");

            final String output = outContent.toString();
            assertThat(output).contains("validate");
            assertThat(output).contains("--type");
            assertThat(output).contains("--bootstrap");
            assertThat(output).contains("--to");
        }
    }

    private Path createTestFile(final String content, final String name) throws IOException {
        final Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    // Test fixture
    public static class TestBootstrapForValidate implements DataFixerBootstrap {
        public TestBootstrapForValidate() {
            // Public no-arg constructor required
        }

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry schemas) {
            // Minimal bootstrap for testing
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar fixes) {
            // No fixes needed for validate command tests
        }
    }
}
