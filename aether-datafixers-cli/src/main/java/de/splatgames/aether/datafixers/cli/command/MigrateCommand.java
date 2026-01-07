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
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import de.splatgames.aether.datafixers.cli.format.FormatRegistry;
import de.splatgames.aether.datafixers.cli.report.ReportFormatter;
import de.splatgames.aether.datafixers.cli.util.VersionExtractor;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Migrate command for transforming data files between versions.
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
@Command(
        name = "migrate",
        description = "Migrate data files from one version to another.",
        mixinStandardHelpOptions = true
)
public class MigrateCommand implements Callable<Integer> {

    @Parameters(
            index = "0..*",
            description = "Input file(s) or directory to migrate.",
            arity = "1..*"
    )
    private List<File> inputFiles;

    @Option(
            names = {"-o", "--output"},
            description = "Output file or directory (defaults to stdout for single file, or in-place with backup)."
    )
    private File output;

    @Option(
            names = {"--from"},
            description = "Source data version (auto-detected if --version-field is specified)."
    )
    private Integer fromVersion;

    @Option(
            names = {"--to"},
            description = "Target data version.",
            required = true
    )
    private int toVersion;

    @Option(
            names = {"-t", "--type"},
            description = "Type reference ID (e.g., 'player', 'world').",
            required = true
    )
    private String typeId;

    @Option(
            names = {"--version-field"},
            description = "JSON field path containing the data version (e.g., 'dataVersion' or 'meta.version').",
            defaultValue = "dataVersion"
    )
    private String versionField;

    @Option(
            names = {"--format"},
            description = "Input/output format: json-gson, json-jackson (default: json-gson).",
            defaultValue = "json-gson"
    )
    private String format;

    @Option(
            names = {"--backup"},
            description = "Create .bak backup files before overwriting.",
            defaultValue = "true"
    )
    private boolean backup;

    @Option(
            names = {"--bootstrap"},
            description = "Fully qualified class name of DataFixerBootstrap implementation.",
            required = true
    )
    private String bootstrapClass;

    @Option(
            names = {"--report"},
            description = "Generate migration report."
    )
    private boolean generateReport;

    @Option(
            names = {"--report-format"},
            description = "Report format: text, json (default: text).",
            defaultValue = "text"
    )
    private String reportFormat;

    @Option(
            names = {"--report-file"},
            description = "Write report to file instead of stderr."
    )
    private File reportFile;

    @Option(
            names = {"--fail-fast"},
            description = "Stop processing on first error (default: false).",
            defaultValue = "false"
    )
    private boolean failFast;

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output."
    )
    private boolean verbose;

    @Option(
            names = {"--pretty"},
            description = "Pretty-print output JSON.",
            defaultValue = "true"
    )
    private boolean prettyPrint;

    @Override
    public Integer call() {
        try {
            // 1. Load bootstrap
            final DataFixerBootstrap bootstrap = BootstrapLoader.load(bootstrapClass);

            // 2. Create fixer
            final DataVersion targetVersion = new DataVersion(toVersion);
            final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                    .create(targetVersion, bootstrap);

            // 3. Get format handler
            final FormatHandler<?> handler = FormatRegistry.get(format);
            if (handler == null) {
                System.err.println("Unknown format: " + format);
                System.err.println("Available formats: " + FormatRegistry.availableFormats());
                return 1;
            }

            // 4. Process files
            final TypeReference typeRef = new TypeReference(typeId);
            int successCount = 0;
            int errorCount = 0;
            final StringBuilder reportBuilder = new StringBuilder();

            for (final File inputFile : inputFiles) {
                try {
                    final MigrationResult result = processFile(
                            inputFile, fixer, handler, typeRef, targetVersion);
                    successCount++;

                    if (generateReport) {
                        reportBuilder.append(result.report).append("\n");
                    }
                } catch (final Exception e) {
                    errorCount++;
                    System.err.println("Error processing " + inputFile + ": " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace(System.err);
                    }
                    if (failFast) {
                        return 1;
                    }
                }
            }

            // Write report
            if (generateReport && !reportBuilder.isEmpty()) {
                final String reportContent = reportBuilder.toString();
                if (reportFile != null) {
                    Files.writeString(reportFile.toPath(), reportContent);
                } else {
                    System.err.println(reportContent);
                }
            }

            // Summary
            if (inputFiles.size() > 1 || verbose) {
                System.err.println("Completed: " + successCount + " migrated, " + errorCount + " errors");
            }

            return errorCount > 0 ? 1 : 0;

        } catch (final Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(System.err);
            }
            return 1;
        }
    }

    private <T> MigrationResult processFile(
            final File inputFile,
            final AetherDataFixer fixer,
            final FormatHandler<T> handler,
            final TypeReference typeRef,
            final DataVersion targetVersion
    ) throws IOException {
        final Instant startTime = Instant.now();

        // Read input
        final String content = Files.readString(inputFile.toPath());
        final T data = handler.parse(content);

        // Determine source version
        final DataVersion sourceVersion;
        if (fromVersion != null) {
            sourceVersion = new DataVersion(fromVersion);
        } else {
            sourceVersion = VersionExtractor.extract(data, handler.ops(), versionField);
        }

        // Check if migration is needed
        if (sourceVersion.getVersion() >= targetVersion.getVersion()) {
            if (verbose) {
                System.err.println("Skipping " + inputFile + " (already at v"
                        + sourceVersion.getVersion() + ")");
            }
            return new MigrationResult("", Duration.ZERO);
        }

        // Create dynamic and migrate
        final Dynamic<T> dynamic = new Dynamic<>(handler.ops(), data);
        final TaggedDynamic tagged = new TaggedDynamic(typeRef, dynamic);

        // Perform migration
        final TaggedDynamic migrated = fixer.update(tagged, sourceVersion, targetVersion);

        // Extract result
        @SuppressWarnings("unchecked")
        final Dynamic<T> resultDynamic = (Dynamic<T>) migrated.value();
        final String outputContent = prettyPrint
                ? handler.serializePretty(resultDynamic.value())
                : handler.serialize(resultDynamic.value());

        // Write output
        writeOutput(inputFile, outputContent);

        final Duration duration = Duration.between(startTime, Instant.now());

        if (verbose) {
            System.err.println("Migrated: " + inputFile
                    + " (v" + sourceVersion.getVersion() + " -> v" + targetVersion.getVersion()
                    + " in " + duration.toMillis() + "ms)");
        }

        // Generate report
        String report = "";
        if (generateReport) {
            final ReportFormatter formatter = ReportFormatter.forFormat(reportFormat);
            report = formatter.formatSimple(
                    inputFile.getName(),
                    typeRef.getId(),
                    sourceVersion.getVersion(),
                    targetVersion.getVersion(),
                    duration
            );
        }

        return new MigrationResult(report, duration);
    }

    private void writeOutput(final File inputFile, final String content) throws IOException {
        if (output != null) {
            // Write to specified output
            if (output.isDirectory()) {
                final Path outPath = output.toPath().resolve(inputFile.getName());
                Files.writeString(outPath, content);
            } else if (inputFiles.size() == 1) {
                Files.writeString(output.toPath(), content);
            } else {
                throw new IllegalArgumentException(
                        "Output must be a directory when multiple input files are specified");
            }
        } else if (inputFiles.size() == 1 && output == null) {
            // Single file with no output: stdout
            System.out.println(content);
        } else {
            // Multiple files: in-place with backup
            if (backup) {
                final Path backupPath = inputFile.toPath().resolveSibling(
                        inputFile.getName() + ".bak");
                Files.copy(inputFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(inputFile.toPath(), content);
        }
    }

    private record MigrationResult(String report, Duration duration) {
    }
}
