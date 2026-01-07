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
import de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import de.splatgames.aether.datafixers.cli.format.FormatRegistry;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command to display version and configuration information.
 *
 * <p>The info command provides information about the CLI tool, available
 * format handlers, and optionally details about a specific bootstrap
 * configuration.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * # Show available formats
 * aether-cli info --formats
 *
 * # Show bootstrap information
 * aether-cli info --bootstrap com.example.MyBootstrap --to 200
 *
 * # Show all info (formats are shown by default when no bootstrap is specified)
 * aether-cli info
 * }</pre>
 *
 * <h2>Exit Codes</h2>
 * <ul>
 *   <li>{@code 0} - Information displayed successfully</li>
 *   <li>{@code 1} - Error loading bootstrap or missing required option</li>
 * </ul>
 *
 * @author Erik Pfoertner
 * @see de.splatgames.aether.datafixers.cli.AetherCli
 * @see de.splatgames.aether.datafixers.cli.format.FormatRegistry
 * @since 0.3.0
 */
@Command(
        name = "info",
        description = "Display version and configuration information.",
        mixinStandardHelpOptions = true
)
public class InfoCommand implements Callable<Integer> {

    /**
     * Fully qualified class name of the {@link DataFixerBootstrap} implementation to inspect.
     *
     * <p>When specified, the command loads and displays information about the bootstrap
     * configuration. This option requires {@link #toVersion} to be specified as well,
     * since a DataFixer must be instantiated with a target version.</p>
     *
     * <p>If {@code null} (not specified), bootstrap information is not displayed and
     * only format handlers are listed.</p>
     *
     * <p>CLI usage: {@code --bootstrap <fully.qualified.ClassName>}</p>
     *
     * @see BootstrapLoader#load(String)
     * @see #toVersion
     */
    @Option(
            names = {"--bootstrap"},
            description = "Fully qualified class name of DataFixerBootstrap implementation."
    )
    private String bootstrapClass;

    /**
     * Whether to explicitly list available format handlers.
     *
     * <p>When {@code true} or when no {@link #bootstrapClass} is specified, the command
     * outputs a list of all registered {@link FormatHandler} implementations including
     * their format IDs, descriptions, and supported file extensions.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * <p>CLI usage: {@code --formats}</p>
     *
     * @see FormatRegistry#handlers()
     * @see FormatHandler
     */
    @Option(
            names = {"--formats"},
            description = "List available format handlers."
    )
    private boolean listFormats;

    /**
     * Target data version for bootstrap inspection.
     *
     * <p>Required when {@link #bootstrapClass} is specified. The version is used to
     * create the {@link AetherDataFixer} instance, which allows the command to display
     * accurate bootstrap configuration information.</p>
     *
     * <p>If {@link #bootstrapClass} is specified but this option is missing, the command
     * prints an error and exits with code 1.</p>
     *
     * <p>CLI usage: {@code --to <version>}</p>
     *
     * @see #bootstrapClass
     */
    @Option(
            names = {"--to"},
            description = "Target version (required when using --bootstrap)."
    )
    private Integer toVersion;

    /**
     * Executes the info command.
     *
     * <p>This method displays information based on the provided options:</p>
     * <ol>
     *   <li>Always prints the CLI version header</li>
     *   <li>Lists available format handlers if {@link #listFormats} is {@code true}
     *       or if {@link #bootstrapClass} is not specified</li>
     *   <li>If {@link #bootstrapClass} is specified:
     *       <ul>
     *         <li>Validates that {@link #toVersion} is also specified</li>
     *         <li>Loads the bootstrap class</li>
     *         <li>Creates a DataFixer instance</li>
     *         <li>Displays bootstrap information including class name and target version</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p>Output format:</p>
     * <pre>
     * Aether Datafixers CLI v0.3.0
     * ============================
     *
     * Available Formats:
     *   - json-gson: JSON format using Gson
     *     Extensions: json
     *   - json-jackson: JSON format using Jackson
     *     Extensions: json
     *
     * Bootstrap Information:
     *   Class: com.example.MyBootstrap
     *   Target Version: 200
     * </pre>
     *
     * @return {@code 0} if information was displayed successfully,
     *         {@code 1} if an error occurred (missing --to option, bootstrap load failure)
     * @see FormatRegistry#handlers()
     * @see BootstrapLoader#load(String)
     */
    @Override
    public Integer call() {
        System.out.println("Aether Datafixers CLI v0.3.0");
        System.out.println("============================");
        System.out.println();

        if (this.listFormats || this.bootstrapClass == null) {
            System.out.println("Available Formats:");
            for (final FormatHandler<?> handler : FormatRegistry.handlers()) {
                System.out.println("  - " + handler.formatId() + ": " + handler.description());
                System.out.println("    Extensions: " + String.join(", ", handler.fileExtensions()));
            }
            System.out.println();
        }

        if (this.bootstrapClass != null) {
            try {
                final DataFixerBootstrap bootstrap = BootstrapLoader.load(this.bootstrapClass);

                if (this.toVersion == null) {
                    System.err.println("Error: --to version is required when using --bootstrap");
                    return 1;
                }

                final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                        .create(new de.splatgames.aether.datafixers.api.DataVersion(this.toVersion), bootstrap);

                System.out.println("Bootstrap Information:");
                System.out.println("  Class: " + this.bootstrapClass);
                System.out.println("  Target Version: " + fixer.currentVersion().getVersion());
                System.out.println();

            } catch (final Exception e) {
                System.err.println("Error loading bootstrap: " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }
}
