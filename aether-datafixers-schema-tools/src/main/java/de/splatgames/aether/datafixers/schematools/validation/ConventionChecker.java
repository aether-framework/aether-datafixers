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

package de.splatgames.aether.datafixers.schematools.validation;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import de.splatgames.aether.datafixers.schematools.introspection.TypeIntrospector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Checks naming conventions in schemas and fixes.
 *
 * <p>This checker validates that names follow the configured conventions:</p>
 * <ul>
 *   <li>Type reference names (e.g., snake_case)</li>
 *   <li>Field names within types</li>
 *   <li>Schema class names (e.g., must end with "Schema")</li>
 *   <li>DataFix class names (e.g., must end with "Fix")</li>
 * </ul>
 *
 * <h2>Issue Codes</h2>
 * <ul>
 *   <li>{@code CONVENTION_TYPE_NAME} - Type name violates conventions</li>
 *   <li>{@code CONVENTION_FIELD_NAME} - Field name violates conventions</li>
 *   <li>{@code CONVENTION_SCHEMA_CLASS} - Schema class name violates conventions</li>
 *   <li>{@code CONVENTION_FIX_CLASS} - Fix class name violates conventions</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>This class is used internally by {@link SchemaValidator}. Direct usage
 * is not recommended.</p>
 *
 * @author Erik Pfoertner
 * @see ConventionRules
 * @see SchemaValidator
 * @since 0.3.0
 */
public final class ConventionChecker {

    /**
     * Issue code for type name violations.
     */
    public static final String CONVENTION_TYPE_NAME = "CONVENTION_TYPE_NAME";

    /**
     * Issue code for field name violations.
     */
    public static final String CONVENTION_FIELD_NAME = "CONVENTION_FIELD_NAME";

    /**
     * Issue code for schema class name violations.
     */
    public static final String CONVENTION_SCHEMA_CLASS = "CONVENTION_SCHEMA_CLASS";

    /**
     * Issue code for fix class name violations.
     */
    public static final String CONVENTION_FIX_CLASS = "CONVENTION_FIX_CLASS";

    private ConventionChecker() {
        // Utility class
    }

    /**
     * Checks convention compliance for a schema.
     *
     * @param schema the schema to check, must not be {@code null}
     * @param rules  the convention rules to apply, must not be {@code null}
     * @return the validation result, never {@code null}
     */
    @NotNull
    public static ValidationResult checkSchema(
            @NotNull final Schema schema,
            @NotNull final ConventionRules rules
    ) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        Preconditions.checkNotNull(rules, "rules must not be null");

        if (!rules.isEnabled()) {
            return ValidationResult.empty();
        }

        final ValidationResult.Builder result = ValidationResult.builder();
        final String schemaLocation = "Schema@" + schema.version().getVersion();

        // Check schema class name
        final String schemaClassName = schema.getClass().getSimpleName();
        if (!rules.isValidSchemaClassName(schemaClassName)) {
            result.add(createIssue(
                    CONVENTION_SCHEMA_CLASS,
                    buildClassNameViolationMessage(schemaClassName, "Schema",
                            rules.schemaClassPrefix(), rules.schemaClassSuffix()),
                    schemaLocation,
                    rules
            ));
        }

        // Check type names and field names
        for (final TypeReference ref : schema.types().references()) {
            final String typeName = ref.getId();

            // Check type name
            if (!rules.isValidTypeName(typeName)) {
                result.add(createIssue(
                        CONVENTION_TYPE_NAME,
                        "Type name '" + typeName + "' violates naming convention"
                                + (rules.typeNamePattern() != null
                                ? " (expected pattern: " + rules.typeNamePattern().pattern() + ")"
                                : ""),
                        schemaLocation + "/" + typeName,
                        rules
                ).withContext("typeName", typeName));
            }

            // Check field names
            final Type<?> type = schema.types().get(ref);
            if (type != null) {
                checkFieldNames(type, typeName, schemaLocation, rules, result);
            }
        }

        return result.build();
    }

    /**
     * Checks convention compliance for a DataFix.
     *
     * @param fix   the fix to check, must not be {@code null}
     * @param rules the convention rules to apply, must not be {@code null}
     * @return the validation result, never {@code null}
     */
    @NotNull
    public static ValidationResult checkFix(
            @NotNull final DataFix<?> fix,
            @NotNull final ConventionRules rules
    ) {
        Preconditions.checkNotNull(fix, "fix must not be null");
        Preconditions.checkNotNull(rules, "rules must not be null");

        if (!rules.isEnabled()) {
            return ValidationResult.empty();
        }

        final ValidationResult.Builder result = ValidationResult.builder();

        // Check fix class name
        final String fixClassName = fix.getClass().getSimpleName();
        if (!rules.isValidFixClassName(fixClassName)) {
            result.add(createIssue(
                    CONVENTION_FIX_CLASS,
                    buildClassNameViolationMessage(fixClassName, "Fix",
                            rules.fixClassPrefix(), rules.fixClassSuffix()),
                    "Fix@" + fix.toVersion().getVersion(),
                    rules
            ));
        }

        return result.build();
    }

    /**
     * Checks field names within a type.
     */
    private static void checkFieldNames(
            @NotNull final Type<?> type,
            @NotNull final String typeName,
            @NotNull final String schemaLocation,
            @NotNull final ConventionRules rules,
            @NotNull final ValidationResult.Builder result
    ) {
        final List<FieldInfo> fields = TypeIntrospector.extractFields(type);

        for (final FieldInfo field : fields) {
            final String fieldName = field.name();

            if (!rules.isValidFieldName(fieldName)) {
                result.add(createIssue(
                        CONVENTION_FIELD_NAME,
                        "Field name '" + fieldName + "' in type '" + typeName
                                + "' violates naming convention"
                                + (rules.fieldNamePattern() != null
                                ? " (expected pattern: " + rules.fieldNamePattern().pattern() + ")"
                                : ""),
                        schemaLocation + "/" + typeName + "/" + fieldName,
                        rules
                ).withContext("fieldName", fieldName)
                        .withContext("typeName", typeName));
            }
        }
    }

    /**
     * Builds a descriptive message for class name convention violations.
     *
     * @param className the actual class name
     * @param type      the type of class (e.g., "Schema", "Fix")
     * @param prefix    the expected prefix, or {@code null}
     * @param suffix    the expected suffix, or {@code null}
     * @return the violation message
     */
    @NotNull
    private static String buildClassNameViolationMessage(
            @NotNull final String className,
            @NotNull final String type,
            final String prefix,
            final String suffix
    ) {
        final StringBuilder message = new StringBuilder();
        message.append(type).append(" class name '").append(className).append("' should ");

        if (prefix != null && suffix != null) {
            message.append("start with '").append(prefix)
                    .append("' and end with '").append(suffix).append("'");
        } else if (prefix != null) {
            message.append("start with '").append(prefix).append("'");
        } else if (suffix != null) {
            message.append("end with '").append(suffix).append("'");
        } else {
            message.append("follow naming conventions");
        }

        return message.toString();
    }

    /**
     * Creates a validation issue with the appropriate severity based on rules.
     */
    @NotNull
    private static ValidationIssue createIssue(
            @NotNull final String code,
            @NotNull final String message,
            @NotNull final String location,
            @NotNull final ConventionRules rules
    ) {
        final ValidationIssue issue = rules.treatViolationsAsErrors()
                ? ValidationIssue.error(code, message)
                : ValidationIssue.warning(code, message);
        return issue.at(location);
    }
}
