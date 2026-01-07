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

package de.splatgames.aether.datafixers.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AetherCli")
class AetherCliTest {

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
    @DisplayName("Without arguments")
    class WithoutArguments {

        @Test
        @DisplayName("prints help when called without arguments")
        void printsHelpWithoutArguments() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute();

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("aether-cli");
            assertThat(output).contains("Usage:");
        }

        @Test
        @DisplayName("lists available commands")
        void listsAvailableCommands() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            cmd.execute();

            final String output = outContent.toString();
            assertThat(output).contains("migrate");
            assertThat(output).contains("validate");
            assertThat(output).contains("info");
            assertThat(output).contains("help");
        }
    }

    @Nested
    @DisplayName("Help options")
    class HelpOptions {

        @Test
        @DisplayName("displays help with --help")
        void displaysHelpWithHelpOption() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("--help");

            assertThat(exitCode).isEqualTo(0);
            final String output = outContent.toString();
            assertThat(output).contains("Usage:");
            assertThat(output).contains("Commands:");
        }

        @Test
        @DisplayName("displays help with -h")
        void displaysHelpWithShortOption() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("-h");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("Usage:");
        }

        @Test
        @DisplayName("displays help for subcommand")
        void displaysHelpForSubcommand() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            cmd.execute("help", "migrate");

            final String output = outContent.toString();
            assertThat(output).contains("migrate");
            assertThat(output).contains("--from");
            assertThat(output).contains("--to");
        }
    }

    @Nested
    @DisplayName("Version option")
    class VersionOption {

        @Test
        @DisplayName("displays version with --version")
        void displaysVersionWithVersionOption() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("--version");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("0.3.0");
        }

        @Test
        @DisplayName("displays version with -V")
        void displaysVersionWithShortOption() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("-V");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("Aether Datafixers CLI");
        }
    }

    @Nested
    @DisplayName("Subcommand routing")
    class SubcommandRouting {

        @Test
        @DisplayName("routes to info command")
        void routesToInfoCommand() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("info", "--formats");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("json-gson");
        }

        @Test
        @DisplayName("routes to validate command with --help")
        void routesToValidateCommand() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("validate", "--help");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("validate");
        }

        @Test
        @DisplayName("routes to migrate command with --help")
        void routesToMigrateCommand() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("migrate", "--help");

            assertThat(exitCode).isEqualTo(0);
            assertThat(outContent.toString()).contains("migrate");
        }
    }

    @Nested
    @DisplayName("Invalid commands")
    class InvalidCommands {

        @Test
        @DisplayName("returns error for unknown command")
        void returnsErrorForUnknownCommand() {
            final CommandLine cmd = new CommandLine(new AetherCli());
            final int exitCode = cmd.execute("unknown");

            assertThat(exitCode).isNotEqualTo(0);
        }
    }
}
