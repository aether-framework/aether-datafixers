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

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
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

@DisplayName("MigrateCommand")
class MigrateCommandTest {

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
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2");

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires --bootstrap option")
        void requiresBootstrapOption() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--to", "2");

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires --to option")
        void requiresToOption() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName());

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("requires at least one input file")
        void requiresInputFiles() {
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2");

            assertThat(exitCode).isNotEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Single file migration")
    class SingleFileMigration {

        @Test
        @DisplayName("outputs to stdout when no output specified")
        void outputsToStdout() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("\"displayName\"");
        }

        @Test
        @DisplayName("writes to output file when -o specified")
        void writesToOutputFile() throws IOException {
            final Path inputFile = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "input.json");
            final Path outputFile = tempDir.resolve("output.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    inputFile.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "-o", outputFile.toString());

            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(outputFile)).isTrue();
            final String content = Files.readString(outputFile);
            assertThat(content).contains("\"displayName\"");
        }

        @Test
        @DisplayName("skips file that is already at target version")
        void skipsUpToDateFile() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 2, \"displayName\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "-v");

            assertThat(exitCode).isEqualTo(0);
            assertThat(errContent.toString()).contains("Skipping");
        }
    }

    @Nested
    @DisplayName("Multiple file migration")
    class MultipleFileMigration {

        @Test
        @DisplayName("creates backup files when --backup is true (default)")
        void createsBackupFiles() throws IOException {
            final Path file1 = createTestFile("{\"dataVersion\": 1, \"name\": \"alice\"}", "player1.json");
            final Path file2 = createTestFile("{\"dataVersion\": 1, \"name\": \"bob\"}", "player2.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file1.toString(),
                    file2.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(file1.resolveSibling("player1.json.bak"))).isTrue();
            assertThat(Files.exists(file2.resolveSibling("player2.json.bak"))).isTrue();
        }

        @Test
        @DisplayName("does not create backup when --backup=false")
        void skipsBackupWhenDisabled() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "single.json");
            // We need multiple files to trigger in-place mode
            final Path file2 = createTestFile("{\"dataVersion\": 1, \"name\": \"test2\"}", "single2.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    file2.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--backup=false");

            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(file.resolveSibling("single.json.bak"))).isFalse();
        }

        @Test
        @DisplayName("writes to directory when -o is a directory")
        void writesToDirectory() throws IOException {
            final Path inputFile = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "input.json");
            final Path outputDir = Files.createDirectory(tempDir.resolve("output"));
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    inputFile.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "-o", outputDir.toString());

            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(outputDir.resolve("input.json"))).isTrue();
        }

        @Test
        @DisplayName("prints summary for multiple files")
        void printsSummary() throws IOException {
            final Path file1 = createTestFile("{\"dataVersion\": 1, \"name\": \"alice\"}", "player1.json");
            final Path file2 = createTestFile("{\"dataVersion\": 1, \"name\": \"bob\"}", "player2.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file1.toString(),
                    file2.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(0);
            assertThat(errContent.toString()).contains("Completed:");
            assertThat(errContent.toString()).contains("2 migrated");
        }
    }

    @Nested
    @DisplayName("Version detection")
    class VersionDetection {

        @Test
        @DisplayName("auto-detects version from dataVersion field")
        void autoDetectsVersion() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("\"displayName\"");
        }

        @Test
        @DisplayName("uses custom version field path")
        void usesCustomVersionField() throws IOException {
            final Path file = createTestFile("{\"meta\":{\"version\": 1}, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--version-field", "meta.version");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("\"displayName\"");
        }

        @Test
        @DisplayName("--from overrides version field")
        void fromOverridesVersionField() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 999, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("\"displayName\"");
        }
    }

    @Nested
    @DisplayName("Format handling")
    class FormatHandling {

        @Test
        @DisplayName("uses json-gson by default")
        void usesGsonByDefault() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("accepts json-jackson format")
        void acceptsJacksonFormat() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--format", "json-jackson");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("returns error for unknown format")
        void returnsErrorForUnknownFormat() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--format", "unknown");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Unknown format");
        }
    }

    @Nested
    @DisplayName("Report generation")
    class ReportGeneration {

        @Test
        @DisplayName("generates text report when --report is specified")
        void generatesTextReport() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--report");

            assertThat(exitCode).isEqualTo(0);
            assertThat(errContent.toString()).contains("Migration:");
        }

        @Test
        @DisplayName("generates JSON report when --report-format json")
        void generatesJsonReport() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--report",
                    "--report-format", "json");

            assertThat(exitCode).isEqualTo(0);
            assertThat(errContent.toString()).contains("\"file\":");
        }

        @Test
        @DisplayName("writes report to file when --report-file specified")
        void writesReportToFile() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final Path reportFile = tempDir.resolve("report.txt");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--report",
                    "--report-file", reportFile.toString());

            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(reportFile)).isTrue();
            assertThat(Files.readString(reportFile)).contains("Migration:");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("returns error for invalid JSON")
        void returnsErrorForInvalidJson() throws IOException {
            final Path file = createTestFile("not valid json", "invalid.txt");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Error processing");
        }

        @Test
        @DisplayName("stops on first error with --fail-fast")
        void stopsOnFirstErrorWithFailFast() throws IOException {
            final Path invalid = createTestFile("not valid json", "invalid.txt");
            final Path valid = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "valid.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    invalid.toString(),
                    valid.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "--fail-fast");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("returns error for invalid bootstrap class")
        void returnsErrorForInvalidBootstrap() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", "com.nonexistent.Bootstrap",
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Fatal error");
        }

        @Test
        @DisplayName("returns error for non-existent input file")
        void returnsErrorForNonExistentFile() {
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    tempDir.resolve("does-not-exist.json").toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("returns error for empty file")
        void returnsErrorForEmptyFile() throws IOException {
            final Path file = createTestFile("", "empty.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("returns error for whitespace-only file")
        void returnsErrorForWhitespaceOnlyFile() throws IOException {
            final Path file = createTestFile("   \n\t  ", "whitespace.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("returns error for output to non-existent directory")
        void returnsErrorForNonExistentOutputDir() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final Path nonExistentDir = tempDir.resolve("nonexistent").resolve("subdir").resolve("output.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "-o", nonExistentDir.toString());

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("continues without fail-fast on multiple errors")
        void continuesWithoutFailFast() throws IOException {
            final Path invalid1 = createTestFile("not valid json 1", "invalid1.txt");
            final Path invalid2 = createTestFile("not valid json 2", "invalid2.txt");
            final Path valid = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "valid.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    invalid1.toString(),
                    invalid2.toString(),
                    valid.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            // Should process all files, exit with error due to failures
            assertThat(exitCode).isEqualTo(1);
            // But valid file should still be processed (check for summary)
            final String err = errContent.toString();
            assertThat(err).contains("Error");
        }

        @Test
        @DisplayName("handles file with missing version field when --from not specified")
        void handlesFileMissingVersionField() throws IOException {
            final Path file = createTestFile("{\"name\": \"test\"}", "noversion.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Error");
        }

        @Test
        @DisplayName("handles truncated JSON file")
        void handlesTruncatedJson() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\":", "truncated.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("handles binary content as input")
        void handlesBinaryContent() throws IOException {
            final Path file = tempDir.resolve("binary.json");
            Files.write(file, new byte[]{0x00, 0x01, (byte) 0xFF, (byte) 0xFE, 0x00});
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1");

            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Verbose output")
    class VerboseOutput {

        @Test
        @DisplayName("prints migration details with -v")
        void printsMigrationDetails() throws IOException {
            final Path file = createTestFile("{\"dataVersion\": 1, \"name\": \"test\"}", "test.json");
            final CommandLine cmd = new CommandLine(new MigrateCommand());

            final int exitCode = cmd.execute(
                    file.toString(),
                    "--type", "player",
                    "--bootstrap", MigrateTestBootstrap.class.getName(),
                    "--to", "2",
                    "--from", "1",
                    "-v");

            assertThat(exitCode).isEqualTo(0);
            assertThat(errContent.toString()).contains("Migrated:");
        }
    }

    @Nested
    @DisplayName("Help option")
    class HelpOption {

        @Test
        @DisplayName("displays help with --help")
        void displaysHelp() {
            final CommandLine cmd = new CommandLine(new MigrateCommand());
            cmd.execute("--help");

            final String output = outContent.toString();
            assertThat(output).contains("migrate");
            assertThat(output).contains("--from");
            assertThat(output).contains("--to");
            assertThat(output).contains("--type");
            assertThat(output).contains("--bootstrap");
        }
    }

    private Path createTestFile(final String content, final String name) throws IOException {
        final Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    /**
     * Test bootstrap that renames "name" to "displayName" when migrating from v1 to v2.
     */
    public static class MigrateTestBootstrap implements DataFixerBootstrap {
        public static final TypeReference PLAYER = new TypeReference("player");

        public MigrateTestBootstrap() {
            // Public no-arg constructor required
        }

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry schemas) {
            // Use MockSchemas to create minimal test schemas
            schemas.register(MockSchemas.minimal(1));
            schemas.register(MockSchemas.minimal(2));
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar fixes) {
            // Use QuickFix for a simple rename operation
            fixes.register(PLAYER, QuickFix.renameField(
                    GsonOps.INSTANCE,
                    "rename_name_to_displayName",
                    1, 2,
                    "name", "displayName"
            ));
        }
    }
}
