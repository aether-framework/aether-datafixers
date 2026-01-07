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
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InfoCommand")
class InfoCommandTest {

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
    @DisplayName("Without options")
    class WithoutOptions {

        @Test
        @DisplayName("returns exit code 0")
        void returnsZeroExitCode() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            final int exitCode = cmd.execute();

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("displays version header")
        void displaysVersionHeader() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            cmd.execute();

            assertThat(outContent.toString()).contains("Aether Datafixers CLI");
        }

        @Test
        @DisplayName("lists available formats by default")
        void listsFormatsWithoutBootstrap() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            cmd.execute();

            final String output = outContent.toString();
            assertThat(output).contains("Available Formats:");
            assertThat(output).contains("json-gson");
            assertThat(output).contains("json-jackson");
        }
    }

    @Nested
    @DisplayName("--formats option")
    class FormatsOption {

        @Test
        @DisplayName("lists all available formats")
        void listsAllFormats() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            cmd.execute("--formats");

            final String output = outContent.toString();
            assertThat(output).contains("json-gson");
            assertThat(output).contains("json-jackson");
        }

        @Test
        @DisplayName("includes format descriptions")
        void includesFormatDescriptions() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            cmd.execute("--formats");

            final String output = outContent.toString();
            assertThat(output).contains("Extensions:");
        }
    }

    @Nested
    @DisplayName("--bootstrap option")
    class BootstrapOption {

        @Test
        @DisplayName("requires --to option")
        void requiresToOption() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            final int exitCode = cmd.execute("--bootstrap", TestBootstrapForInfo.class.getName());

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("--to version is required");
        }

        @Test
        @DisplayName("loads valid bootstrap with --to")
        void loadsValidBootstrap() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            final int exitCode = cmd.execute(
                    "--bootstrap", TestBootstrapForInfo.class.getName(),
                    "--to", "100");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("Bootstrap Information:");
            assertThat(output).contains(TestBootstrapForInfo.class.getName());
        }

        @Test
        @DisplayName("reports error for non-existent bootstrap class")
        void reportsErrorForNonExistentClass() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            final int exitCode = cmd.execute(
                    "--bootstrap", "com.nonexistent.Bootstrap",
                    "--to", "100");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Error loading bootstrap");
        }

        @Test
        @DisplayName("reports error for invalid bootstrap class")
        void reportsErrorForInvalidClass() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            final int exitCode = cmd.execute(
                    "--bootstrap", String.class.getName(),
                    "--to", "100");

            assertThat(exitCode).isEqualTo(1);
            assertThat(errContent.toString()).contains("Error loading bootstrap");
        }
    }

    @Nested
    @DisplayName("Help option")
    class HelpOption {

        @Test
        @DisplayName("displays help with --help")
        void displaysHelp() {
            final CommandLine cmd = new CommandLine(new InfoCommand());
            cmd.execute("--help");

            final String output = outContent.toString();
            assertThat(output).contains("info");
            assertThat(output).contains("--formats");
            assertThat(output).contains("--bootstrap");
        }
    }

    // Test fixture
    public static class TestBootstrapForInfo implements DataFixerBootstrap {
        public TestBootstrapForInfo() {
            // Public no-arg constructor required
        }

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry schemas) {
            // Minimal bootstrap for testing
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar fixes) {
            // No fixes needed for info command tests
        }
    }
}
