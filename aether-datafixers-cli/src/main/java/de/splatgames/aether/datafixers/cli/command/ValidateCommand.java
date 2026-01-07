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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import de.splatgames.aether.datafixers.cli.format.FormatRegistry;
import de.splatgames.aether.datafixers.cli.util.VersionExtractor;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Validate command to check files without migrating.
 *
 * <p>Exit codes:</p>
 * <ul>
 *   <li>0 - All files are up-to-date</li>
 *   <li>1 - An error occurred</li>
 *   <li>2 - Some files need migration</li>
 * </ul>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
@Command(
        name = "validate",
        description = "Validate data files and check if migration is needed.",
        mixinStandardHelpOptions = true
)
public class ValidateCommand implements Callable<Integer> {

    @Parameters(
            index = "0..*",
            description = "Input file(s) to validate.",
            arity = "1..*"
    )
    private List<File> inputFiles;

    @Option(
            names = {"-t", "--type"},
            description = "Type reference ID (e.g., 'player', 'world').",
            required = true
    )
    private String typeId;

    @Option(
            names = {"--version-field"},
            description = "JSON field containing data version.",
            defaultValue = "dataVersion"
    )
    private String versionField;

    @Option(
            names = {"--format"},
            description = "Input format (default: json-gson).",
            defaultValue = "json-gson"
    )
    private String format;

    @Option(
            names = {"--bootstrap"},
            description = "Fully qualified class name of DataFixerBootstrap implementation.",
            required = true
    )
    private String bootstrapClass;

    @Option(
            names = {"--to"},
            description = "Target version to check against.",
            required = true
    )
    private int toVersion;

    @Override
    public Integer call() {
        try {
            final DataFixerBootstrap bootstrap = BootstrapLoader.load(bootstrapClass);
            final DataVersion targetVersion = new DataVersion(toVersion);
            final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                    .create(targetVersion, bootstrap);

            final FormatHandler<?> handler = FormatRegistry.get(format);
            if (handler == null) {
                System.err.println("Unknown format: " + format);
                System.err.println("Available formats: " + FormatRegistry.availableFormats());
                return 1;
            }

            int needsMigration = 0;
            int upToDate = 0;
            int errors = 0;

            for (final File file : inputFiles) {
                final ValidationResult result = validateFile(file, handler, targetVersion);
                switch (result.status) {
                    case UP_TO_DATE -> {
                        upToDate++;
                        System.out.println("OK: " + file + " (v" + result.version + ")");
                    }
                    case NEEDS_MIGRATION -> {
                        needsMigration++;
                        System.out.println("MIGRATE: " + file
                                + " (v" + result.version + " -> v" + targetVersion.getVersion() + ")");
                    }
                    case ERROR -> {
                        errors++;
                        System.out.println("ERROR: " + file + " - " + result.message);
                    }
                }
            }

            System.out.println();
            System.out.println("Summary: " + upToDate + " up-to-date, "
                    + needsMigration + " need migration, " + errors + " errors");

            if (errors > 0) {
                return 1;
            }
            return needsMigration > 0 ? 2 : 0;

        } catch (final Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private <T> ValidationResult validateFile(
            final File file,
            final FormatHandler<T> handler,
            final DataVersion targetVersion
    ) {
        try {
            final String content = Files.readString(file.toPath());
            final T data = handler.parse(content);

            final DataVersion fileVersion = VersionExtractor.extract(
                    data, handler.ops(), versionField);

            if (fileVersion.getVersion() >= targetVersion.getVersion()) {
                return new ValidationResult(ValidationStatus.UP_TO_DATE,
                        fileVersion.getVersion(), null);
            } else {
                return new ValidationResult(ValidationStatus.NEEDS_MIGRATION,
                        fileVersion.getVersion(), null);
            }
        } catch (final Exception e) {
            return new ValidationResult(ValidationStatus.ERROR, -1, e.getMessage());
        }
    }

    private enum ValidationStatus {
        UP_TO_DATE,
        NEEDS_MIGRATION,
        ERROR
    }

    private record ValidationResult(ValidationStatus status, int version, String message) {
    }
}
