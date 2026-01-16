# CLI Installation

This guide covers how to build, install, and run the Aether Datafixers CLI.

## Building from Source

### Prerequisites

- Java 17 or later
- Maven 3.9.5 or later

### Build the Fat JAR

The CLI module produces a fat JAR containing all dependencies:

```bash
# Clone the repository
git clone https://github.com/aether-framework/aether-datafixers.git
cd aether-datafixers

# Build all modules (including CLI)
mvn clean install

# The fat JAR is located at:
# aether-datafixers-cli/target/aether-datafixers-cli-0.5.0-jar-with-dependencies.jar
```

### Verify Installation

```bash
java -jar aether-datafixers-cli/target/aether-datafixers-cli-0.5.0-jar-with-dependencies.jar --version
```

Expected output:
```
Aether Datafixers CLI 0.5.0
```

---

## Running the CLI

### Direct Execution

Run the CLI directly with `java -jar`:

```bash
java -jar aether-datafixers-cli-0.5.0-jar-with-dependencies.jar migrate \
    --to 200 \
    --type player \
    --bootstrap com.example.MyBootstrap \
    input.json
```

### Create an Alias (Unix/Linux/macOS)

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
alias aether-cli='java -jar /path/to/aether-datafixers-cli-0.5.0-jar-with-dependencies.jar'
```

Then use:

```bash
aether-cli migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json
```

### Create a Batch File (Windows)

Create `aether-cli.bat`:

```batch
@echo off
java -jar "C:\path\to\aether-datafixers-cli-0.5.0-jar-with-dependencies.jar" %*
```

Add the directory to your PATH, then use:

```cmd
aether-cli migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json
```

### Running via Maven

For development, run directly via Maven:

```bash
mvn exec:java -pl aether-datafixers-cli -Dexec.args="--help"
```

```bash
mvn exec:java -pl aether-datafixers-cli \
    -Dexec.args="migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json"
```

---

## Adding Your Bootstrap to the Classpath

The CLI needs access to your `DataFixerBootstrap` implementation. There are several ways to achieve this:

### Option 1: Include Bootstrap JAR

Add your bootstrap JAR to the classpath:

```bash
java -cp "aether-datafixers-cli-0.5.0-jar-with-dependencies.jar:my-bootstrap.jar" \
    de.splatgames.aether.datafixers.cli.AetherCli \
    migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json
```

Note: On Windows, use `;` instead of `:` as the path separator.

### Option 2: Fat JAR with Bootstrap

Create a custom fat JAR that includes both the CLI and your bootstrap:

```xml
<!-- In your project's pom.xml -->
<dependencies>
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-cli</artifactId>
        <version>0.5.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>de.splatgames.aether.datafixers.cli.AetherCli</mainClass>
                    </manifest>
                </archive>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Option 3: Classes Directory

During development, point to compiled classes:

```bash
java -cp "aether-datafixers-cli-0.5.0-jar-with-dependencies.jar:target/classes" \
    de.splatgames.aether.datafixers.cli.AetherCli \
    migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json
```

---

## Dependency on Your Project

To use the CLI as a dependency in your project (for programmatic access):

### Maven

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-cli</artifactId>
    <version>0.5.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'de.splatgames.aether.datafixers:aether-datafixers-cli:0.5.0'
```

---

## Troubleshooting

### ClassNotFoundException for Bootstrap

**Problem:** `Error loading bootstrap: com.example.MyBootstrap`

**Solution:** Ensure your bootstrap class is on the classpath. See [Adding Your Bootstrap](#adding-your-bootstrap-to-the-classpath).

### NoClassDefFoundError for Gson/Jackson

**Problem:** `NoClassDefFoundError: com/google/gson/Gson`

**Solution:** The fat JAR includes Gson by default. If using a custom classpath setup, add:

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

### UnsupportedClassVersionError

**Problem:** `UnsupportedClassVersionError: ... class file version 61.0`

**Solution:** You need Java 17 or later. Check your version with:

```bash
java -version
```

---

## Next Steps

→ [Command Reference](commands.md) — Learn all available options
