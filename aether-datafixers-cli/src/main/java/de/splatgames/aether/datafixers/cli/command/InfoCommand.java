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
 * Info command to display available types, formats, and version information.
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
@Command(
        name = "info",
        description = "Display version and configuration information.",
        mixinStandardHelpOptions = true
)
public class InfoCommand implements Callable<Integer> {

    @Option(
            names = {"--bootstrap"},
            description = "Fully qualified class name of DataFixerBootstrap implementation."
    )
    private String bootstrapClass;

    @Option(
            names = {"--formats"},
            description = "List available format handlers."
    )
    private boolean listFormats;

    @Option(
            names = {"--to"},
            description = "Target version (required when using --bootstrap)."
    )
    private Integer toVersion;

    @Override
    public Integer call() {
        System.out.println("Aether Datafixers CLI v0.3.0");
        System.out.println("============================");
        System.out.println();

        if (listFormats || bootstrapClass == null) {
            System.out.println("Available Formats:");
            for (final FormatHandler<?> handler : FormatRegistry.handlers()) {
                System.out.println("  - " + handler.formatId() + ": " + handler.description());
                System.out.println("    Extensions: " + String.join(", ", handler.fileExtensions()));
            }
            System.out.println();
        }

        if (bootstrapClass != null) {
            try {
                final DataFixerBootstrap bootstrap = BootstrapLoader.load(bootstrapClass);

                if (toVersion == null) {
                    System.err.println("Error: --to version is required when using --bootstrap");
                    return 1;
                }

                final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                        .create(new de.splatgames.aether.datafixers.api.DataVersion(toVersion), bootstrap);

                System.out.println("Bootstrap Information:");
                System.out.println("  Class: " + bootstrapClass);
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
