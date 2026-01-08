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

/**
 * Fluent migration service API for performing data migrations in Spring applications.
 *
 * <p>This package provides a high-level, user-friendly abstraction for performing data
 * migrations with the Aether Datafixers framework. The service follows the builder pattern
 * to offer an intuitive, chainable API for configuring and executing migrations.</p>
 *
 * <h2>Package Contents</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.spring.service.MigrationService}
 *       - Main service interface with fluent builder API</li>
 *   <li>{@link de.splatgames.aether.datafixers.spring.service.MigrationResult}
 *       - Immutable result object containing migration outcome</li>
 *   <li>{@link de.splatgames.aether.datafixers.spring.service.DefaultMigrationService}
 *       - Default implementation with metrics and async support</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @Service
 * public class GameDataService {
 *
 *     private final MigrationService migrationService;
 *
 *     public GameDataService(MigrationService migrationService) {
 *         this.migrationService = migrationService;
 *     }
 *
 *     public TaggedDynamic<?> migrateGameSave(TaggedDynamic<?> data, int version) {
 *         MigrationResult result = migrationService
 *             .migrate(data)
 *             .from(version)
 *             .toLatest()
 *             .execute();
 *
 *         if (result.isSuccess()) {
 *             return result.getData();
 *         }
 *         throw new MigrationException(result.getError().orElse(null));
 *     }
 * }
 * }</pre>
 *
 * <h2>Multi-Domain Usage</h2>
 * <pre>{@code
 * MigrationResult result = migrationService
 *     .migrate(userData)
 *     .usingDomain("user")     // Select specific domain
 *     .from(100)
 *     .to(200)                 // Explicit target version
 *     .execute();
 * }</pre>
 *
 * <h2>Asynchronous Execution</h2>
 * <pre>{@code
 * CompletableFuture<MigrationResult> future = migrationService
 *     .migrate(largeDataSet)
 *     .from(100)
 *     .toLatest()
 *     .executeAsync();
 *
 * future.thenAccept(result -> {
 *     if (result.isSuccess()) {
 *         processData(result.getData());
 *     }
 * });
 * }</pre>
 *
 * <h2>Result Handling</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.service.MigrationResult} class
 * provides comprehensive information about the migration outcome:</p>
 * <ul>
 *   <li>{@code isSuccess()} / {@code isFailure()} - Check outcome</li>
 *   <li>{@code getData()} / {@code getDataOptional()} - Access migrated data</li>
 *   <li>{@code getError()} - Get failure details</li>
 *   <li>{@code getDuration()} - Migration timing</li>
 *   <li>{@code getVersionSpan()} - Version difference</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.service.DefaultMigrationService}
 * is thread-safe and can be safely used concurrently from multiple threads.</p>
 *
 * @author Erik Pfoertner
 * @see de.splatgames.aether.datafixers.spring.service.MigrationService
 * @see de.splatgames.aether.datafixers.spring.service.MigrationResult
 * @see de.splatgames.aether.datafixers.spring.service.DefaultMigrationService
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring.service;
