# User Profile Example

Migrating user profile data with nested objects and optional fields.

## Scenario

A web application stores user profiles that evolve over time:

| Version | Changes                                               |
|---------|-------------------------------------------------------|
| v1      | Basic profile: username, email                        |
| v2      | Added address as nested object                        |
| v3      | Split name into firstName/lastName, added preferences |

## Data Evolution

### Version 1

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe"
}
```

### Version 2

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "country": "USA"
  }
}
```

### Version 3

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "country": "USA"
  },
  "preferences": {
    "theme": "light",
    "notifications": true
  }
}
```

## TypeReferences

```java
public final class TypeReferences {
    public static final TypeReference USER = TypeReference.of("user");
    public static final TypeReference ADDRESS = TypeReference.of("address");
    public static final TypeReference PREFERENCES = TypeReference.of("preferences");

    private TypeReferences() {}
}
```

## Schemas

### Schema V1

```java
public class SchemaV1 extends Schema {

    public SchemaV1(Schema parent) {
        super(new DataVersion(1), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.USER, DSL.and(
            DSL.field("username", DSL.string()),
            DSL.field("email", DSL.string()),
            DSL.field("fullName", DSL.string()),
            DSL.remainder()
        ));
    }
}
```

### Schema V2

```java
public class SchemaV2 extends Schema {

    public SchemaV2(Schema parent) {
        super(new DataVersion(2), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.USER, DSL.and(
            DSL.field("username", DSL.string()),
            DSL.field("email", DSL.string()),
            DSL.field("fullName", DSL.string()),
            DSL.optional("address", DSL.and(
                DSL.field("street", DSL.string()),
                DSL.field("city", DSL.string()),
                DSL.field("country", DSL.string()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));
    }
}
```

### Schema V3

```java
public class SchemaV3 extends Schema {

    public SchemaV3(Schema parent) {
        super(new DataVersion(3), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.USER, DSL.and(
            DSL.field("username", DSL.string()),
            DSL.field("email", DSL.string()),
            DSL.field("firstName", DSL.string()),
            DSL.field("lastName", DSL.string()),
            DSL.optional("address", DSL.and(
                DSL.field("street", DSL.string()),
                DSL.field("city", DSL.string()),
                DSL.field("country", DSL.string()),
                DSL.remainder()
            )),
            DSL.field("preferences", DSL.and(
                DSL.field("theme", DSL.string()),
                DSL.field("notifications", DSL.bool()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));
    }
}
```

## Fixes

### V1 to V2 Fix

```java
public class UserV1ToV2Fix extends SchemaDataFix {

    public UserV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "user-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Add optional address with default empty object
        return Rules.transform(TypeReferences.USER, user -> {
            if (user.get("address").result().isEmpty()) {
                // Add empty address object
                Dynamic<?> emptyAddress = user.emptyMap()
                    .set("street", user.createString(""))
                    .set("city", user.createString(""))
                    .set("country", user.createString(""));
                return user.set("address", emptyAddress);
            }
            return user;
        });
    }
}
```

### V2 to V3 Fix

```java
public class UserV2ToV3Fix extends SchemaDataFix {

    public UserV2ToV3Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(2), new DataVersion(3), "user-v2-to-v3");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Split fullName into firstName and lastName
            Rules.transform(TypeReferences.USER, this::splitName),
            // Add default preferences
            Rules.transform(TypeReferences.USER, this::addDefaultPreferences)
        );
    }

    private Dynamic<?> splitName(Dynamic<?> user) {
        String fullName = user.get("fullName").asString().orElse("");
        String[] parts = fullName.split(" ", 2);

        String firstName = parts.length > 0 ? parts[0] : "";
        String lastName = parts.length > 1 ? parts[1] : "";

        return user
            .set("firstName", user.createString(firstName))
            .set("lastName", user.createString(lastName))
            .remove("fullName");
    }

    private Dynamic<?> addDefaultPreferences(Dynamic<?> user) {
        if (user.get("preferences").result().isEmpty()) {
            Dynamic<?> preferences = user.emptyMap()
                .set("theme", user.createString("light"))
                .set("notifications", user.createBoolean(true));
            return user.set("preferences", preferences);
        }
        return user;
    }
}
```

## Bootstrap

```java
public class UserDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(3);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;
        schemas.register(new DataVersion(1), SchemaV1::new);
        schemas.register(new DataVersion(2), SchemaV2::new);
        schemas.register(new DataVersion(3), SchemaV3::new);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.USER, new UserV1ToV2Fix(schemas));
        fixes.register(TypeReferences.USER, new UserV2ToV3Fix(schemas));
    }
}
```

## Usage

```java
public class UserProfileMigrator {

    private final AetherDataFixer fixer;
    private final Gson gson;

    public UserProfileMigrator() {
        this.fixer = new DataFixerRuntimeFactory()
            .create(UserDataBootstrap.CURRENT_VERSION, new UserDataBootstrap());
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public JsonObject migrateProfile(JsonObject profile, int fromVersion) {
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, profile);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.USER, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(fromVersion),
            UserDataBootstrap.CURRENT_VERSION
        );

        return (JsonObject) migrated.value().value();
    }

    public static void main(String[] args) {
        UserProfileMigrator migrator = new UserProfileMigrator();

        // Old v1 profile
        JsonObject v1Profile = new JsonObject();
        v1Profile.addProperty("username", "john_doe");
        v1Profile.addProperty("email", "john@example.com");
        v1Profile.addProperty("fullName", "John Doe");

        System.out.println("V1 Profile:");
        System.out.println(migrator.gson.toJson(v1Profile));

        // Migrate to v3
        JsonObject v3Profile = migrator.migrateProfile(v1Profile, 1);

        System.out.println("\nMigrated to V3:");
        System.out.println(migrator.gson.toJson(v3Profile));
    }
}
```

## Output

```
V1 Profile:
{
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe"
}

Migrated to V3:
{
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "address": {
    "street": "",
    "city": "",
    "country": ""
  },
  "preferences": {
    "theme": "light",
    "notifications": true
  }
}
```

## Key Patterns Demonstrated

1. **Optional nested objects** — Address may not exist
2. **Field splitting** — fullName → firstName + lastName
3. **Default values** — Preferences added with defaults
4. **Chained migrations** — v1 → v2 → v3 automatic chain

## Related

- [Handle Optional Fields](../how-to/handle-optional-fields.md)
- [Restructure Data](../how-to/restructure-data.md)
- [Game Data Example](game-data-example/index.md)

