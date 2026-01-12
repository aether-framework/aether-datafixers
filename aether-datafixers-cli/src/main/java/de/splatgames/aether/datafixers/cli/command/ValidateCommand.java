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
 * CLI command to validate data files and check if migration is needed.
 *
 * <p>The validate command checks data files against a target schema version
 * without performing any modifications. This is useful for batch validation,
 * CI/CD pipelines, and pre-migration checks.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * # Validate a single file
 * aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap input.json
 *
 * # Validate multiple files
 * aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap *.json
 *
 * # Validate with custom version field
 * aether-cli validate --to 200 --type player --version-field meta.version --bootstrap com.example.MyBootstrap input.json
 * }</pre>
 *
 * <h2>Exit Codes</h2>
 * <ul>
 *   <li>{@code 0} - All files are up-to-date (no migration needed)</li>
 *   <li>{@code 1} - An error occurred (e.g., file not found, parse error)</li>
 *   <li>{@code 2} - One or more files need migration</li>
 * </ul>
 *
 * <h2>Output Format</h2>
 * <p>For each file, the command outputs one of:</p>
 * <ul>
 *   <li>{@code OK: filename (vN)} - File is at or above target version</li>
 *   <li>{@code MIGRATE: filename (vN -> vM)} - File needs migration</li>
 *   <li>{@code ERROR: filename - message} - File could not be validated</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.cli.AetherCli
 * @see MigrateCommand
 * @since 0.3.0
 */
@Command(
        name = "validate",
        description = "Validate data files and check if migration is needed.",
        mixinStandardHelpOptions = true
)
public class ValidateCommand implements Callable<Integer> {

    /**
     * List of input files to validate.
     *
     * <p>Accepts one or more file paths. Glob patterns (e.g., {@code *.json})
     * are expanded by the shell before being passed to the CLI. Each file is
     * checked independently and results are reported individually.</p>
     *
     * <p>CLI usage: positional arguments after all options</p>
     */
    @Parameters(
            index = "0..*",
            description = "Input file(s) to validate.",
            arity = "1..*"
    )
    private List<File> inputFiles;

    /**
     * Type reference identifier for the data being validated.
     *
     * <p>This ID identifies the type of data in the files. While not strictly
     * necessary for version checking, it is required to properly initialize the
     * data fixer infrastructure.</p>
     *
     * <p>This is a required option.</p>
     *
     * <p>CLI usage: {@code -t <type>} or {@code --type <type>}</p>
     *
     * @see de.splatgames.aether.datafixers.api.TypeReference
     */
    @Option(
            names = {"-t", "--type"},
            description = "Type reference ID (e.g., 'player', 'world').",
            required = true
    )
    private String typeId;

    /**
     * JSON field path for extracting the data version from input files.
     *
     * <p>Supports dot notation for nested fields (e.g., "meta.version" to access
     * {@code {"meta": {"version": 100}}}). The extracted version is compared
     * against {@link #toVersion} to determine if migration is needed.</p>
     *
     * <p>Default value: "dataVersion"</p>
     *
     * <p>CLI usage: {@code --version-field <path>}</p>
     *
     * @see VersionExtractor#extract(Object, de.splatgames.aether.datafixers.api.dynamic.DynamicOps, String)
     */
    @Option(
            names = {"--version-field"},
            description = "JSON field containing data version.",
            defaultValue = "dataVersion"
    )
    private String versionField;

    /**
     * Serialization format identifier for parsing input files.
     *
     * <p>Determines which {@link FormatHandler} is used for parsing the input.
     * Available formats can be listed with {@code aether-cli info --formats}.</p>
     *
     * <p>Built-in formats:</p>
     * <ul>
     *   <li>{@code json-gson} - JSON using Google Gson (default)</li>
     *   <li>{@code json-jackson} - JSON using Jackson Databind</li>
     * </ul>
     *
     * <p>CLI usage: {@code --format <format-id>}</p>
     *
     * @see FormatRegistry
     * @see FormatHandler
     */
    @Option(
            names = {"--format"},
            description = "Input format (default: json-gson).",
            defaultValue = "json-gson"
    )
    private String format;

    /**
     * Fully qualified class name of the {@link DataFixerBootstrap} implementation.
     *
     * <p>The specified class must:</p>
     * <ul>
     *   <li>Implement {@link DataFixerBootstrap}</li>
     *   <li>Have a public no-argument constructor</li>
     *   <li>Be available on the classpath</li>
     * </ul>
     *
     * <p>This is a required option.</p>
     *
     * <p>CLI usage: {@code --bootstrap <fully.qualified.ClassName>}</p>
     *
     * @see BootstrapLoader#load(String)
     * @see DataFixerBootstrap
     */
    @Option(
            names = {"--bootstrap"},
            description = "Fully qualified class name of DataFixerBootstrap implementation.",
            required = true
    )
    private String bootstrapClass;

    /**
     * Target data version to validate against.
     *
     * <p>Files with a version number less than this value are reported as
     * needing migration. Files at or above this version are reported as
     * up-to-date.</p>
     *
     * <p>This is a required option.</p>
     *
     * <p>CLI usage: {@code --to <version>}</p>
     */
    @Option(
            names = {"--to"},
            description = "Target version to check against.",
            required = true
    )
    private int toVersion;

    /**
     * Executes the validate command.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Loads the {@link DataFixerBootstrap} from {@link #bootstrapClass}</li>
     *   <li>Creates an {@link AetherDataFixer} instance for the target version</li>
     *   <li>Resolves the {@link FormatHandler} for the specified {@link #format}</li>
     *   <li>Iterates through all {@link #inputFiles} and validates each one</li>
     *   <li>Prints the validation status for each file</li>
     *   <li>Prints a summary with counts of up-to-date, needs-migration, and error files</li>
     * </ol>
     *
     * <p>Exit codes:</p>
     * <ul>
     *   <li>{@code 0} - All files are up-to-date</li>
     *   <li>{@code 1} - One or more errors occurred</li>
     *   <li>{@code 2} - No errors, but one or more files need migration</li>
     * </ul>
     *
     * @return exit code indicating validation result
     * @see #validateFile(File, FormatHandler, DataVersion)
     */
    @Override
    public Integer call() {
        try {
            final DataFixerBootstrap bootstrap = BootstrapLoader.load(this.bootstrapClass);
            final DataVersion targetVersion = new DataVersion(this.toVersion);
            new DataFixerRuntimeFactory()
                    .create(targetVersion, bootstrap);

            final FormatHandler<?> handler = FormatRegistry.get(this.format);
            if (handler == null) {
                System.err.println("Unknown format: " + this.format);
                System.err.println("Available formats: " + FormatRegistry.availableFormats());
                return 1;
            }

            int needsMigration = 0;
            int upToDate = 0;
            int errors = 0;

            for (final File file : this.inputFiles) {
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

    /**
     * Validates a single file against the target version.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Reads the file content as a string</li>
     *   <li>Parses the content using the format handler</li>
     *   <li>Extracts the version from the specified {@link #versionField}</li>
     *   <li>Compares the extracted version against the target version</li>
     * </ol>
     *
     * <p>The method catches all exceptions and returns an error result rather
     * than propagating exceptions, allowing batch validation to continue even
     * when individual files fail.</p>
     *
     * @param <T>           the underlying data representation type (e.g., JsonElement, JsonNode)
     * @param file          the file to validate, must not be {@code null}
     * @param handler       the format handler for parsing, must not be {@code null}
     * @param targetVersion the target version to compare against, must not be {@code null}
     * @return a {@link ValidationResult} indicating the file's status
     * @see #call()
     * @see VersionExtractor#extract(Object, de.splatgames.aether.datafixers.api.dynamic.DynamicOps, String)
     */
    private <T> ValidationResult validateFile(
            final File file,
            final FormatHandler<T> handler,
            final DataVersion targetVersion
    ) {
        try {
            final String content = Files.readString(file.toPath());
            final T data = handler.parse(content);

            final DataVersion fileVersion = VersionExtractor.extract(
                    data, handler.ops(), this.versionField);

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

    /**
     * Enumeration of possible validation outcomes for a single file.
     *
     * <p>Each status corresponds to a specific output format and contributes
     * to the final exit code calculation.</p>
     *
     * @see ValidationResult
     */
    private enum ValidationStatus {
        /**
         * File is at or above the target version, no migration needed.
         *
         * <p>Output format: {@code OK: filename (vN)}</p>
         */
        UP_TO_DATE,

        /**
         * File is below the target version and requires migration.
         *
         * <p>Output format: {@code MIGRATE: filename (vN -> vM)}</p>
         */
        NEEDS_MIGRATION,

        /**
         * An error occurred while validating the file.
         *
         * <p>Output format: {@code ERROR: filename - message}</p>
         * <p>Common causes include: file not found, parse errors, missing version field.</p>
         */
        ERROR
    }

    /**
     * Holds the result of validating a single file.
     *
     * <p>This record captures the validation outcome including the status,
     * the detected version (if available), and any error message.</p>
     *
     * @param status  the validation status indicating the outcome
     * @param version the detected data version, or {@code -1} if an error occurred
     * @param message the error message if status is {@link ValidationStatus#ERROR},
     *                otherwise {@code null}
     * @see #validateFile(File, FormatHandler, DataVersion)
     * @see ValidationStatus
     */
    private record ValidationResult(ValidationStatus status, int version, String message) {
    }
}
