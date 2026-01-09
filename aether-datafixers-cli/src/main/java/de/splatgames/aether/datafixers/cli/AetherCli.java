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

import de.splatgames.aether.datafixers.cli.command.InfoCommand;
import de.splatgames.aether.datafixers.cli.command.MigrateCommand;
import de.splatgames.aether.datafixers.cli.command.ValidateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.util.concurrent.Callable;

/**
 * Main entry point for the Aether Datafixers CLI.
 *
 * <p>The CLI provides commands for migrating, validating, and inspecting
 * data files using Aether Datafixers.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * # Migrate a single file
 * aether-cli migrate --from 100 --to 200 --type player --bootstrap com.example.MyBootstrap input.json
 *
 * # Migrate with auto-detected version
 * aether-cli migrate --to 200 --type player --version-field dataVersion --bootstrap com.example.MyBootstrap input.json
 *
 * # Validate files
 * aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap *.json
 *
 * # Show info
 * aether-cli info --formats
 * aether-cli info --bootstrap com.example.MyBootstrap
 * }</pre>
 *
 * @author Erik Pförtner
 * @since 0.3.0
 */
@Command(
        name = "aether-cli",
        mixinStandardHelpOptions = true,
        version = "Aether Datafixers CLI 0.3.0",
        description = "Command-line data migration tool using Aether Datafixers.",
        subcommands = {
                MigrateCommand.class,
                ValidateCommand.class,
                InfoCommand.class,
                HelpCommand.class
        },
        synopsisHeading = "%nUsage:%n%n",
        descriptionHeading = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n"
)
public class AetherCli implements Callable<Integer> {

    /**
     * Main entry point for the Aether Datafixers CLI application.
     *
     * <p>This method initializes the picocli {@link CommandLine} parser with the
     * root {@link AetherCli} command and executes it with the provided arguments.
     * The process exit code is set based on the command execution result.</p>
     *
     * <h4>Exit Codes</h4>
     * <ul>
     *   <li>{@code 0} - Success (or help displayed)</li>
     *   <li>{@code 1} - Error occurred during command execution</li>
     *   <li>{@code 2} - Validation found files needing migration (validate command only)</li>
     * </ul>
     *
     * <h4>Configuration</h4>
     * <p>The CommandLine instance is configured with:</p>
     * <ul>
     *   <li>Case-insensitive enum value parsing enabled</li>
     * </ul>
     *
     * @param args command-line arguments passed from the shell; may be empty
     *             but should not be {@code null}
     * @see CommandLine#execute(String...)
     */
    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new AetherCli())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }

    /**
     * Executes the root command when invoked without a subcommand.
     *
     * <p>When the CLI is invoked without specifying a subcommand (e.g., just
     * {@code aether-cli}), this method is called. It displays the help message
     * showing available commands and options.</p>
     *
     * <p>This behavior provides a user-friendly experience where running the
     * CLI without arguments shows how to use it, rather than doing nothing
     * or showing an error.</p>
     *
     * @return {@code 0} indicating successful execution (help was displayed)
     * @see CommandLine#usage(Object, java.io.PrintStream)
     */
    @Override
    public Integer call() {
        // Print help when no subcommand is specified
        CommandLine.usage(this, System.out);
        return 0;
    }
}
