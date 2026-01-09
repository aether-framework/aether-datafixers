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

package de.splatgames.aether.datafixers.examples.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

/**
 * Complete example demonstrating the Aether Datafixers workflow.
 *
 * <p>This example shows the proper way to use the framework in a DFU-style pattern:</p>
 * <ol>
 *   <li>Create a {@link GameDataBootstrap} with schemas and fixes</li>
 *   <li>Build an {@link AetherDataFixer} using {@link DataFixerRuntimeFactory}</li>
 *   <li>Load old save data and migrate it to the current version</li>
 * </ol>
 *
 * <h2>Versioning (SemVer-based IDs)</h2>
 * <ul>
 *   <li>100 = Version 1.0.0 - Initial flat structure</li>
 *   <li>110 = Version 1.1.0 - Restructured with nested position</li>
 *   <li>200 = Version 2.0.0 - Extended with health and level</li>
 * </ul>
 *
 * @see Schema100
 * @see Schema110
 * @see Schema200
 * @see PlayerV1ToV2Fix
 * @see PlayerV2ToV3Fix
 */
public final class GameExample {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Main entry point demonstrating the complete migration workflow.
     *
     * @param args command line arguments (unused)
     */
    public static void main(final String[] args) {
        printHeader("Aether Datafixers - Game Data Migration Example");

        // =====================================================================
        // STEP 1: Create the DataFixer
        // =====================================================================
        printSection("Step 1: Create DataFixer");
        System.out.println("Using DataFixerRuntimeFactory with GameDataBootstrap:");
        System.out.println();
        System.out.println("  AetherDataFixer fixer = new DataFixerRuntimeFactory()");
        System.out.println("      .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());");
        System.out.println();

        final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        System.out.println("DataFixer created successfully.");
        System.out.println("Current version: " + fixer.currentVersion() + " (2.0.0)");
        System.out.println();

        // =====================================================================
        // STEP 2: Simulate loading old V1.0.0 save data
        // =====================================================================
        printSection("Step 2: Load Old Save Data (V1.0.0)");
        System.out.println("Simulating loading a save file from version 1.0.0 (ID: 100)...");
        System.out.println();

        final JsonObject v1SaveData = createV1SaveData();
        System.out.println("V1.0.0 Data Structure:");
        System.out.println("  - playerName: string");
        System.out.println("  - xp: int");
        System.out.println("  - x, y, z: double (flat coordinates)");
        System.out.println("  - gameMode: int (0=survival, 1=creative, ...)");
        System.out.println();
        System.out.println("V1.0.0 Data Content:");
        printJson(v1SaveData);

        // =====================================================================
        // STEP 3: Wrap in TaggedDynamic
        // =====================================================================
        printSection("Step 3: Wrap Data for Migration");
        System.out.println("Convert JSON to TaggedDynamic with type reference:");
        System.out.println();
        System.out.println("  Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, saveData);");
        System.out.println("  TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);");
        System.out.println();

        final Dynamic<JsonElement> dynamicV1 = new Dynamic<>(GsonOps.INSTANCE, v1SaveData);
        final TaggedDynamic taggedV1 = new TaggedDynamic(TypeReferences.PLAYER, dynamicV1);

        System.out.println("Tagged dynamic created with type: " + taggedV1.type().getId());
        System.out.println();

        // =====================================================================
        // STEP 4: Migrate V1.0.0 -> V2.0.0
        // =====================================================================
        printSection("Step 4: Migrate Data V1.0.0 (100) -> V2.0.0 (200)");
        System.out.println("Apply migration using fixer.update():");
        System.out.println();
        System.out.println("  TaggedDynamic migrated = fixer.update(");
        System.out.println("      taggedV1,");
        System.out.println("      new DataVersion(100),  // from V1.0.0");
        System.out.println("      fixer.currentVersion()  // to V2.0.0 (200)");
        System.out.println("  );");
        System.out.println();

        final TaggedDynamic migrated = fixer.update(
                taggedV1,
                new DataVersion(100),
                fixer.currentVersion()
        );

        System.out.println("Migration chain:");
        System.out.println("  V1.0.0 (100) -> V1.1.0 (110): PlayerV1ToV2Fix");
        System.out.println("    - Rename playerName -> name");
        System.out.println("    - Rename xp -> experience");
        System.out.println("    - Convert gameMode int -> string");
        System.out.println("    - Group x/y/z -> position object");
        System.out.println();
        System.out.println("  V1.1.0 (110) -> V2.0.0 (200): PlayerV2ToV3Fix");
        System.out.println("    - Add health field (default 20.0)");
        System.out.println("    - Add maxHealth field (default 20.0)");
        System.out.println("    - Compute level from experience");
        System.out.println();

        // =====================================================================
        // STEP 5: Show result
        // =====================================================================
        printSection("Step 5: Migration Result (V2.0.0)");
        System.out.println("V2.0.0 Data Structure:");
        System.out.println("  - name: string");
        System.out.println("  - experience: int");
        System.out.println("  - level: int (computed)");
        System.out.println("  - health: float");
        System.out.println("  - maxHealth: float");
        System.out.println("  - position: { x, y, z }");
        System.out.println("  - gameMode: string");
        System.out.println();
        System.out.println("V2.0.0 Data Content:");

        @SuppressWarnings("unchecked")
        final Dynamic<JsonElement> resultDynamic = (Dynamic<JsonElement>) migrated.value();
        printJson(resultDynamic.value());

        // =====================================================================
        // SUMMARY
        // =====================================================================
        printHeader("Summary");
        System.out.println("Key Components:");
        System.out.println();
        System.out.println("1. TypeReferences.java");
        System.out.println("   - Defines type IDs (PLAYER, WORLD, etc.)");
        System.out.println("   - Used to route data to correct fixes");
        System.out.println();
        System.out.println("2. Schema Classes (Schema100, Schema110, Schema200)");
        System.out.println("   - Each extends Schema");
        System.out.println("   - Overrides registerTypes() to define types");
        System.out.println("   - SemVer-based: 100=1.0.0, 110=1.1.0, 200=2.0.0");
        System.out.println();
        System.out.println("3. Fix Classes (PlayerV1ToV2Fix, PlayerV2ToV3Fix)");
        System.out.println("   - One class per migration");
        System.out.println("   - Extend SchemaDataFix, implement makeRule()");
        System.out.println("   - Use Rules combinators (renameField, transformField, etc.)");
        System.out.println();
        System.out.println("4. GameDataBootstrap.java");
        System.out.println("   - Implements DataFixerBootstrap");
        System.out.println("   - registerSchemas() - registers Schema100, Schema110, Schema200");
        System.out.println("   - registerFixes() - registers all fix classes");
        System.out.println();
        System.out.println("5. DataFixerRuntimeFactory");
        System.out.println("   - Creates AetherDataFixer from bootstrap");
        System.out.println("   - Handles all wiring automatically");
        System.out.println();
        printHeader("Done");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Creates sample V1.0.0 save data representing an old save file.
     *
     * @return V1.0.0 format player data
     */
    private static JsonObject createV1SaveData() {
        final JsonObject data = new JsonObject();
        data.addProperty("playerName", "Steve");
        data.addProperty("xp", 2500);
        data.addProperty("x", 100.5);
        data.addProperty("y", 64.0);
        data.addProperty("z", -200.25);
        data.addProperty("gameMode", 0);
        return data;
    }

    /**
     * Prints a section header.
     */
    private static void printSection(final String title) {
        System.out.println("--------------------------------------------------");
        System.out.println(title);
        System.out.println("--------------------------------------------------");
        System.out.println();
    }

    /**
     * Prints a main header.
     */
    private static void printHeader(final String title) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("  " + title);
        System.out.println("==================================================");
        System.out.println();
    }

    /**
     * Prints JSON with indentation.
     */
    private static void printJson(final JsonElement json) {
        final String formatted = GSON.toJson(json);
        for (final String line : formatted.split("\n")) {
            System.out.println("  " + line);
        }
        System.out.println();
    }
}
