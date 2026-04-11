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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import de.splatgames.aether.datafixers.cli.format.FormatRegistry;
import de.splatgames.aether.datafixers.cli.report.ReportFormatter;
import de.splatgames.aether.datafixers.cli.util.VersionExtractor;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * CLI command for migrating data files from one schema version to another.
 *
 * <p>The migrate command reads data files, applies the appropriate
 * {@link de.splatgames.aether.datafixers.api.fix.DataFix DataFix} instances
 * to transform the data, and writes the migrated output.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * # Migrate a single file to stdout
 * aether-cli migrate --from 100 --to 200 --type player --bootstrap com.example.MyBootstrap input.json
 *
 * # Migrate with auto-detected version from data field
 * aether-cli migrate --to 200 --type player --version-field dataVersion --bootstrap com.example.MyBootstrap input.json
 *
 * # Migrate multiple files in-place with backup
 * aether-cli migrate --to 200 --type player --backup --bootstrap com.example.MyBootstrap *.json
 *
 * # Migrate to specific output directory
 * aether-cli migrate --to 200 --type player --output ./migrated/ --bootstrap com.example.MyBootstrap *.json
 *
 * # Generate migration report
 * aether-cli migrate --to 200 --type player --report --report-format json --bootstrap com.example.MyBootstrap input.json
 * }</pre>
 *
 * <h2>Exit Codes</h2>
 * <ul>
 *   <li>{@code 0} - All files migrated successfully</li>
 *   <li>{@code 1} - One or more errors occurred during migration</li>
 * </ul>
 *
 * <h2>Output Modes</h2>
 * <ul>
 *   <li><b>Single file, no output:</b> Result is written to stdout</li>
 *   <li><b>Multiple files, no output:</b> Files are modified in-place (with backup if {@code --backup} is set)</li>
 *   <li><b>Output file specified:</b> Result is written to the specified file or directory</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.cli.AetherCli
 * @see ValidateCommand
 * @since 0.3.0
 */
@Command(
        name = "migrate",
        description = "Migrate data files from one version to another.",
        mixinStandardHelpOptions = true
)
public class MigrateCommand implements Callable<Integer> {

    /**
     * List of input files or directories to migrate.
     *
     * <p>Accepts one or more file paths. When a directory is specified,
     * all files within it will be processed. Glob patterns (e.g., {@code *.json})
     * are expanded by the shell before being passed to the CLI.</p>
     *
     * <p>CLI usage: positional arguments after all options</p>
     *
     * @see #output
     */
    @Parameters(
            index = "0..*",
            description = "Input file(s) or directory to migrate.",
            arity = "1..*"
    )
    @Nullable
    private List<File> inputFiles;

    /**
     * Output file or directory for migrated data.
     *
     * <p>Behavior varies based on context:</p>
     * <ul>
     *   <li>If {@code null} and single input file: output goes to stdout</li>
     *   <li>If {@code null} and multiple input files: files are modified in-place</li>
     *   <li>If specified as file: only valid for single input file</li>
     *   <li>If specified as directory: migrated files are written there with original names</li>
     * </ul>
     *
     * <p>CLI usage: {@code -o <path>} or {@code --output <path>}</p>
     *
     * @see #backup
     * @see #writeOutput(File, String)
     */
    @Option(
            names = {"-o", "--output"},
            description = "Output file or directory (defaults to stdout for single file, or in-place with backup)."
    )
    @Nullable
    private File output;

    /**
     * Source data version for migration.
     *
     * <p>When specified, this version is used as the starting point for all files.
     * When {@code null}, the version is auto-detected from each file using the
     * field specified by {@link #versionField}.</p>
     *
     * <p>CLI usage: {@code --from <version>}</p>
     *
     * @see #toVersion
     * @see #versionField
     */
    @Option(
            names = {"--from"},
            description = "Source data version (auto-detected if --version-field is specified)."
    )
    @Nullable
    private Integer fromVersion;

    /**
     * Target data version for migration.
     *
     * <p>All input files will be migrated to this version. Files already at or
     * above this version are skipped (no migration needed).</p>
     *
     * <p>This is a required option.</p>
     *
     * <p>CLI usage: {@code --to <version>}</p>
     *
     * @see #fromVersion
     */
    @Option(
            names = {"--to"},
            description = "Target data version.",
            required = true
    )
    private int toVersion;

    /**
     * Type reference identifier for the data being migrated.
     *
     * <p>This ID is used to look up the appropriate type definition and route
     * the data to the correct fixers. Common examples include "player", "world",
     * "entity", etc.</p>
     *
     * <p>This is a required option.</p>
     *
     * <p>CLI usage: {@code -t <type>} or {@code --type <type>}</p>
     *
     * @see TypeReference
     */
    @Option(
            names = {"-t", "--type"},
            description = "Type reference ID (e.g., 'player', 'world').",
            required = true
    )
    @Nullable
    private String typeId;

    /**
     * JSON field path for extracting the data version from input files.
     *
     * <p>Supports dot notation for nested fields (e.g., "meta.version" to access
     * {@code {"meta": {"version": 100}}}). Used when {@link #fromVersion} is not
     * explicitly specified.</p>
     *
     * <p>Default value: "dataVersion"</p>
     *
     * <p>CLI usage: {@code --version-field <path>}</p>
     *
     * @see VersionExtractor#extract(Object, de.splatgames.aether.datafixers.api.dynamic.DynamicOps, String)
     */
    @Option(
            names = {"--version-field"},
            description = "JSON field path containing the data version (e.g., 'dataVersion' or 'meta.version').",
            defaultValue = "dataVersion"
    )
    @Nullable
    private String versionField;

    /**
     * Serialization format identifier for input and output.
     *
     * <p>Determines which {@link FormatHandler} is used for parsing input files
     * and serializing output. Available formats can be listed with
     * {@code aether-cli info --formats}.</p>
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
            description = "Input/output format: json-gson, json-jackson (default: json-gson).",
            defaultValue = "json-gson"
    )
    @Nullable
    private String format;

    /**
     * Whether to create backup files before in-place modification.
     *
     * <p>When {@code true} and files are modified in-place (no {@link #output}
     * specified with multiple files), a backup copy with ".bak" extension is
     * created before overwriting.</p>
     *
     * <p>Default value: {@code true}</p>
     *
     * <p>CLI usage: {@code --backup} or {@code --backup=false}</p>
     *
     * @see #writeOutput(File, String)
     */
    @Option(
            names = {"--backup"},
            description = "Create .bak backup files before overwriting.",
            defaultValue = "true"
    )
    private boolean backup;

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
    @Nullable
    private String bootstrapClass;

    /**
     * Whether to generate a migration report.
     *
     * <p>When {@code true}, a report is generated for each migrated file containing
     * metadata such as source/target versions and migration duration. The report
     * format is controlled by {@link #reportFormat} and output destination by
     * {@link #reportFile}.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * <p>CLI usage: {@code --report}</p>
     *
     * @see #reportFormat
     * @see #reportFile
     * @see ReportFormatter
     */
    @Option(
            names = {"--report"},
            description = "Generate migration report."
    )
    private boolean generateReport;

    /**
     * Output format for the migration report.
     *
     * <p>Available formats:</p>
     * <ul>
     *   <li>{@code text} - Human-readable plain text (default)</li>
     *   <li>{@code json} - Machine-readable JSON format</li>
     * </ul>
     *
     * <p>Only used when {@link #generateReport} is {@code true}.</p>
     *
     * <p>CLI usage: {@code --report-format <format>}</p>
     *
     * @see #generateReport
     * @see ReportFormatter#forFormat(String)
     */
    @Option(
            names = {"--report-format"},
            description = "Report format: text, json (default: text).",
            defaultValue = "text"
    )
    @Nullable
    private String reportFormat;

    /**
     * Output file for the migration report.
     *
     * <p>When specified, the report is written to this file instead of stderr.
     * If {@code null}, the report is printed to stderr.</p>
     *
     * <p>Only used when {@link #generateReport} is {@code true}.</p>
     *
     * <p>CLI usage: {@code --report-file <path>}</p>
     *
     * @see #generateReport
     */
    @Option(
            names = {"--report-file"},
            description = "Write report to file instead of stderr."
    )
    @Nullable
    private File reportFile;

    /**
     * Whether to stop processing on the first error.
     *
     * <p>When {@code true}, the command exits immediately with code 1 when any
     * file fails to migrate. When {@code false}, processing continues with
     * remaining files and errors are reported at the end.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * <p>CLI usage: {@code --fail-fast}</p>
     */
    @Option(
            names = {"--fail-fast"},
            description = "Stop processing on first error (default: false).",
            defaultValue = "false"
    )
    private boolean failFast;

    /**
     * Whether to enable verbose output.
     *
     * <p>When {@code true}, additional information is printed to stderr including:</p>
     * <ul>
     *   <li>Progress messages for each file</li>
     *   <li>Skipped files (already at target version)</li>
     *   <li>Migration timing information</li>
     *   <li>Full stack traces for errors</li>
     * </ul>
     *
     * <p><b>Security note:</b> Full stack traces may expose internal implementation
     * details, file paths, and class names. Do not expose verbose output to untrusted
     * users if the CLI is wrapped in a service or web interface.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * <p>CLI usage: {@code -v} or {@code --verbose}</p>
     */
    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output (includes stack traces; see security note in docs)."
    )
    private boolean verbose;

    /**
     * Whether to enable field-level diagnostics output.
     *
     * <p>When {@code true}, a detailed diagnostic report is generated for each
     * migrated file, including information about every fix execution, rule
     * application, and field-level operation (renames, removals, additions,
     * transforms, etc.).</p>
     *
     * <p>Diagnostics output is written alongside the migration report: to
     * {@link #reportFile} if specified, or to stderr otherwise. The output
     * format follows the selected {@link #reportFormat}.</p>
     *
     * <p>Default value: {@code false}</p>
     *
     * <p>CLI usage: {@code --diagnostics}</p>
     *
     * @see DiagnosticContext
     * @see de.splatgames.aether.datafixers.api.diagnostic.FieldOperation
     * @since 1.0.0
     */
    @Option(
            names = {"--diagnostics"},
            description = "Enable field-level diagnostics output showing detailed fix and field operation information."
    )
    private boolean diagnostics;

    /**
     * Whether to pretty-print the output JSON.
     *
     * <p>When {@code true}, output is formatted with indentation and line breaks
     * for human readability. When {@code false}, output is compact (single line).</p>
     *
     * <p>Default value: {@code true}</p>
     *
     * <p>CLI usage: {@code --pretty} or {@code --pretty=false}</p>
     *
     * @see FormatHandler#serializePretty(Object)
     * @see FormatHandler#serialize(Object)
     */
    @Option(
            names = {"--pretty"},
            description = "Pretty-print output JSON.",
            defaultValue = "true"
    )
    private boolean prettyPrint;

    /**
     * Executes the migrate command.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Loads the {@link DataFixerBootstrap} from {@link #bootstrapClass}</li>
     *   <li>Creates an {@link AetherDataFixer} instance for the target version</li>
     *   <li>Resolves the {@link FormatHandler} for the specified {@link #format}</li>
     *   <li>Iterates through all {@link #inputFiles} and migrates each one</li>
     *   <li>Collects and outputs the migration report if {@link #generateReport} is enabled</li>
     *   <li>Prints a summary of successful and failed migrations</li>
     * </ol>
     *
     * <p>Error handling behavior is controlled by {@link #failFast}:</p>
     * <ul>
     *   <li>If {@code true}, exits immediately on first error</li>
     *   <li>If {@code false}, continues processing and reports all errors at the end</li>
     * </ul>
     *
     * @return {@code 0} if all files were migrated successfully,
     *         {@code 1} if any errors occurred
     * @see #processFile(File, AetherDataFixer, FormatHandler, TypeReference, DataVersion)
     */
    @Override
    @NotNull
    public Integer call() {
        try {
            // 0. Validate version range
            if (this.toVersion < 0) {
                System.err.println("Error: --to version must be non-negative, got: " + this.toVersion);
                return 1;
            }
            if (this.fromVersion != null && this.fromVersion < 0) {
                System.err.println("Error: --from version must be non-negative, got: " + this.fromVersion);
                return 1;
            }

            // 1. Load bootstrap
            final DataFixerBootstrap bootstrap = BootstrapLoader.load(
                    Objects.requireNonNull(this.bootstrapClass, "--bootstrap is required"));

            // 2. Create fixer
            final DataVersion targetVersion = new DataVersion(this.toVersion);
            final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                    .create(targetVersion, bootstrap);

            // 3. Get format handler
            final FormatHandler<?> handler = FormatRegistry.get(
                    Objects.requireNonNull(this.format, "--format has a default"));
            if (handler == null) {
                System.err.println("Unknown format: " + this.format);
                System.err.println("Available formats: " + FormatRegistry.availableFormats());
                return 1;
            }

            // 4. Process files
            final TypeReference typeRef = new TypeReference(
                    Objects.requireNonNull(this.typeId, "--type is required"));
            int successCount = 0;
            int errorCount = 0;
            final StringBuilder reportBuilder = new StringBuilder();

            final StringBuilder diagnosticBuilder = new StringBuilder();

            final List<File> files = Objects.requireNonNull(this.inputFiles, "inputFiles positional arg required");
            for (final File inputFile : files) {
                try {
                    final MigrationResult result = processFile(
                            inputFile, fixer, handler, typeRef, targetVersion);
                    successCount++;

                    if (this.generateReport) {
                        reportBuilder.append(result.report).append("\n");
                    }
                    if (this.diagnostics && !result.diagnosticOutput.isEmpty()) {
                        diagnosticBuilder.append(result.diagnosticOutput).append("\n");
                    }
                } catch (final Exception e) {
                    errorCount++;
                    System.err.println("Error processing " + inputFile + ": " + e.getMessage());
                    if (this.verbose) {
                        e.printStackTrace(System.err);
                    }
                    if (this.failFast) {
                        return 1;
                    }
                }
            }

            // Write report
            if (this.generateReport && !reportBuilder.isEmpty()) {
                final String reportContent = reportBuilder.toString();
                if (this.reportFile != null) {
                    Files.writeString(this.reportFile.toPath(), reportContent, StandardCharsets.UTF_8);
                } else {
                    System.err.println(reportContent);
                }
            }

            // Write diagnostic output
            if (this.diagnostics && !diagnosticBuilder.isEmpty()) {
                final String diagnosticContent = diagnosticBuilder.toString();
                if (this.reportFile != null) {
                    Files.writeString(this.reportFile.toPath(), diagnosticContent,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                } else {
                    System.err.println(diagnosticContent);
                }
            }

            // Summary
            if (this.inputFiles.size() > 1 || this.verbose) {
                System.err.println("Completed: " + successCount + " migrated, " + errorCount + " errors");
            }

            return errorCount > 0 ? 1 : 0;

        } catch (final Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            if (this.verbose) {
                e.printStackTrace(System.err);
            }
            return 1;
        }
    }

    /**
     * Processes a single file for migration.
     *
     * <p>This method performs the complete migration workflow for one file:</p>
     * <ol>
     *   <li>Reads the file content as a string</li>
     *   <li>Parses the content using the format handler</li>
     *   <li>Determines the source version (from {@link #fromVersion} or extracted from data)</li>
     *   <li>Skips migration if file is already at or above target version</li>
     *   <li>Wraps the data in a {@link Dynamic} and {@link TaggedDynamic}</li>
     *   <li>Applies the data fixer to migrate the data</li>
     *   <li>Serializes the result (pretty or compact based on {@link #prettyPrint})</li>
     *   <li>Writes the output using {@link #writeOutput(File, String)}</li>
     *   <li>Optionally generates a migration report</li>
     * </ol>
     *
     * @param <T>           the underlying data representation type (e.g., JsonElement, JsonNode)
     * @param inputFile     the file to migrate, must not be {@code null}
     * @param fixer         the data fixer instance to use for migration, must not be {@code null}
     * @param handler       the format handler for parsing and serialization, must not be {@code null}
     * @param typeRef       the type reference for routing the data, must not be {@code null}
     * @param targetVersion the target version to migrate to, must not be {@code null}
     * @return a {@link MigrationResult} containing the report string and migration duration
     * @throws IOException if reading or writing the file fails
     * @see #call()
     * @see #writeOutput(File, String)
     */
    private <T> MigrationResult processFile(
            @NotNull final File inputFile,
            @NotNull final AetherDataFixer fixer,
            @NotNull final FormatHandler<T> handler,
            @NotNull final TypeReference typeRef,
            @NotNull final DataVersion targetVersion
    ) throws IOException {
        Preconditions.checkNotNull(inputFile, "inputFile must not be null");
        Preconditions.checkNotNull(fixer, "fixer must not be null");
        Preconditions.checkNotNull(handler, "handler must not be null");
        Preconditions.checkNotNull(typeRef, "typeRef must not be null");
        Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");

        final Instant startTime = Instant.now();

        // Check file size to prevent OOM on very large files
        final long fileSize = Files.size(inputFile.toPath());
        if (fileSize > 100 * 1024 * 1024) {
            throw new IOException("File exceeds maximum size (100MB): " + inputFile);
        }

        // Read input (strip UTF-8 BOM if present)
        String content = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        final T data = handler.parse(content);

        // Determine source version
        final DataVersion sourceVersion;
        if (this.fromVersion != null) {
            sourceVersion = new DataVersion(this.fromVersion);
        } else {
            sourceVersion = VersionExtractor.extract(data, handler.ops(),
                    Objects.requireNonNull(this.versionField, "--version-field has a default"));
        }

        // Check if migration is needed
        if (sourceVersion.getVersion() >= targetVersion.getVersion()) {
            if (this.verbose) {
                System.err.println("Skipping " + inputFile + " (already at v"
                        + sourceVersion.getVersion() + ")");
            }
            return new MigrationResult("", Duration.ZERO, "");
        }

        // Create dynamic and migrate
        final Dynamic<T> dynamic = new Dynamic<>(handler.ops(), data);
        final TaggedDynamic tagged = new TaggedDynamic(typeRef, dynamic);

        // Perform migration (with diagnostics if enabled)
        final DiagnosticContext diagCtx = this.diagnostics
                ? DiagnosticContext.create(DiagnosticOptions.builder()
                        .captureSnapshots(false)
                        .captureRuleDetails(true)
                        .captureFieldDetails(true)
                        .build())
                : null;
        final TaggedDynamic migrated = diagCtx != null
                ? fixer.update(tagged, sourceVersion, targetVersion, diagCtx)
                : fixer.update(tagged, sourceVersion, targetVersion);

        // Extract result
        @SuppressWarnings("unchecked")
        final Dynamic<T> resultDynamic = (Dynamic<T>) migrated.value();
        final String outputContent = this.prettyPrint
                ? handler.serializePretty(resultDynamic.value())
                : handler.serialize(resultDynamic.value());

        // Write output
        writeOutput(inputFile, outputContent);

        final Duration duration = Duration.between(startTime, Instant.now());

        if (this.verbose) {
            System.err.println("Migrated: " + inputFile
                    + " (v" + sourceVersion.getVersion() + " -> v" + targetVersion.getVersion()
                    + " in " + duration.toMillis() + "ms)");
        }

        // Generate report
        String report = "";
        if (this.generateReport) {
            final ReportFormatter formatter = ReportFormatter.forFormat(
                    Objects.requireNonNull(this.reportFormat, "--report-format has a default"));
            report = formatter.formatSimple(
                    inputFile.getName(),
                    typeRef.getId(),
                    sourceVersion.getVersion(),
                    targetVersion.getVersion(),
                    duration
            );
        }

        // Generate diagnostic report
        final MigrationReport diagnosticReport = diagCtx != null ? diagCtx.getReport() : null;
        String diagnosticOutput = "";
        if (diagnosticReport != null) {
            final ReportFormatter formatter = ReportFormatter.forFormat(
                    Objects.requireNonNull(this.reportFormat, "--report-format has a default"));
            diagnosticOutput = formatter.formatDiagnostic(
                    inputFile.getName(),
                    typeRef.getId(),
                    diagnosticReport
            );
        }

        return new MigrationResult(report, duration, diagnosticOutput);
    }

    /**
     * Writes the migrated content to the appropriate destination.
     *
     * <p>The output destination is determined by the following rules:</p>
     * <ol>
     *   <li>If {@link #output} is specified and is a directory: write to
     *       {@code output/inputFile.getName()}</li>
     *   <li>If {@link #output} is specified and is a file (single input only):
     *       write directly to that file</li>
     *   <li>If {@link #output} is {@code null} and single input file:
     *       write to stdout</li>
     *   <li>If {@link #output} is {@code null} and multiple input files:
     *       write in-place, creating a backup if {@link #backup} is enabled</li>
     * </ol>
     *
     * <p>Backup files are created with a ".bak" extension in the same directory
     * as the original file.</p>
     *
     * @param inputFile the original input file (used for naming and backup), must not be {@code null}
     * @param content   the migrated content to write, must not be {@code null}
     * @throws IOException              if writing the file fails
     * @throws IllegalArgumentException if {@link #output} is a file but multiple input files
     *                                  were specified
     * @see #output
     * @see #backup
     * @see #inputFiles
     */
    private void writeOutput(@NotNull final File inputFile, @NotNull final String content) throws IOException {
        Preconditions.checkNotNull(inputFile, "inputFile must not be null");
        Preconditions.checkNotNull(content, "content must not be null");
        if (this.output != null) {
            // Validate output path: reject path traversal via unnormalized segments
            final Path outputPath = this.output.toPath();
            if (!outputPath.normalize().equals(outputPath)
                    || outputPath.toString().contains("..")) {
                throw new IOException(
                        "Output path contains path traversal: " + this.output);
            }

            final List<File> files = Objects.requireNonNull(this.inputFiles, "inputFiles set by picocli");
            if (this.output.isDirectory()) {
                final Path outPath = this.output.toPath().resolve(inputFile.getName());
                Files.writeString(outPath, content, StandardCharsets.UTF_8);
            } else if (files.size() == 1) {
                Files.writeString(this.output.toPath(), content, StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException(
                        "Output must be a directory when multiple input files are specified");
            }
        } else if (Objects.requireNonNull(this.inputFiles, "inputFiles set by picocli").size() == 1) {
            // Single file with no output: stdout
            System.out.println(content);
        } else {
            // Multiple files: in-place with atomic write via temp file
            final Path tempPath = Files.createTempFile(
                    inputFile.toPath().getParent(), "migrate_", ".tmp");
            try {
                Files.writeString(tempPath, content, StandardCharsets.UTF_8);
                if (this.backup) {
                    final String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
                    final Path backupPath = inputFile.toPath().resolveSibling(
                            inputFile.getName() + ".bak." + timestamp);
                    Files.move(inputFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tempPath, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                Files.deleteIfExists(tempPath);
                throw e;
            }
        }
    }

    /**
     * Holds the result of a single file migration operation.
     *
     * <p>This record captures the outcome of migrating one file, including
     * the formatted report string (if reporting is enabled), the time
     * taken for the migration, and the optional diagnostic output (if
     * {@code --diagnostics} was enabled).</p>
     *
     * @param report           the formatted migration report string, empty if reporting is disabled
     *                         or if the file was skipped (already at target version)
     * @param duration         the time elapsed during the migration process, including file I/O
     * @param diagnosticOutput the formatted diagnostic report string, empty if diagnostics are disabled
     * @see #processFile(File, AetherDataFixer, FormatHandler, TypeReference, DataVersion)
     * @see ReportFormatter
     */
    private record MigrationResult(String report, Duration duration, String diagnosticOutput) {
    }
}
