# Affine

An **Affine** optic focuses on zero or one value within a structure. It combines the properties of both Lens (product focus) and Prism (sum focus), making it ideal for accessing optional fields or nested optional structures.

## Definition

```java
public interface Affine<S, A> extends Optic<S, A> {
    /** Try to get the focused value */
    Optional<A> getOptional(S source);

    /** Set the focused value, returning a new source */
    S set(S source, A value);

    /** Modify the focused value if present */
    default S modify(S source, Function<A, A> fn) {
        return getOptional(source)
            .map(a -> set(source, fn.apply(a)))
            .orElse(source);
    }
}
```

**Type Parameters:**
- `S` — The source type (whole structure)
- `A` — The focus type (the optional part)

## Conceptual Model

An Affine is like a Lens that might not find its target:

```
┌─────────────────────────────────────────────────────────────┐
│                        Source S                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              May or may not contain                 │    │
│  │  ┌─────────────────────────────────────────────┐   │    │
│  │  │                 Focus A                      │   │    │
│  │  └─────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  getOptional(s) → Optional<A>                                │
│  set(s, a) → S                                               │
└─────────────────────────────────────────────────────────────┘
```

## Creating Affines

### For Optional Fields

```java
public record User(String name, Optional<String> email) {
    public User withEmail(Optional<String> email) {
        return new User(name, email);
    }
}

Affine<User, String> userEmail = Affine.of(
    user -> user.email(),
    (user, email) -> user.withEmail(Optional.of(email))
);
```

### For Nullable Fields

```java
public record Config(String name, @Nullable Integer timeout) {
    public Config withTimeout(Integer timeout) {
        return new Config(name, timeout);
    }
}

Affine<Config, Integer> configTimeout = Affine.of(
    config -> Optional.ofNullable(config.timeout()),
    Config::withTimeout
);
```

### From Lens and Prism Composition

When you compose a Prism with a Lens (or vice versa), you get an Affine:

```java
// Prism for Optional.of case
Prism<Optional<User>, User> someUser = ...;

// Lens for User.name
Lens<User, String> userName = ...;

// Composed: Optional<User> → String
Affine<Optional<User>, String> optionalUserName = someUser.compose(userName);
```

## Using Affines

### getOptional

Try to extract the focused value:

```java
User userWithEmail = new User("Steve", Optional.of("steve@example.com"));
User userNoEmail = new User("Alex", Optional.empty());

userEmail.getOptional(userWithEmail)  // Optional.of("steve@example.com")
userEmail.getOptional(userNoEmail)    // Optional.empty()
```

### set

Set the focused value (always succeeds):

```java
User user = new User("Steve", Optional.empty());
User updated = userEmail.set(user, "steve@example.com");
// User("Steve", Optional.of("steve@example.com"))
```

### modify

Transform the focused value if present:

```java
User user = new User("Steve", Optional.of("steve@example.com"));
User modified = userEmail.modify(user, String::toUpperCase);
// User("Steve", Optional.of("STEVE@EXAMPLE.COM"))

User noEmail = new User("Alex", Optional.empty());
User unchanged = userEmail.modify(noEmail, String::toUpperCase);
// User("Alex", Optional.empty()) - unchanged
```

### modifyOptional

Transform with an optional result:

```java
// Only modify if email contains "@"
User modified = userEmail.modifyOptional(user, email ->
    email.contains("@") ? Optional.of(email.toUpperCase()) : Optional.empty()
);
```

## Composition

### Affine with Lens

```java
Affine<User, Address> userAddress = ...;
Lens<Address, String> addressCity = ...;

// Composed Affine
Affine<User, String> userCity = userAddress.compose(addressCity);
```

### Affine with Prism

```java
Affine<User, Contact> userContact = ...;
Prism<Contact, Email> contactEmail = ...;

// Composed Affine
Affine<User, Email> userContactEmail = userContact.compose(contactEmail);
```

### Affine with Affine

```java
Affine<Config, Database> configDb = ...;
Affine<Database, Integer> dbPort = ...;

// Composed Affine
Affine<Config, Integer> configDbPort = configDb.compose(dbPort);
```

## Practical Examples

### Nested Optional Access

```java
public record Order(String id, Optional<Customer> customer) {}
public record Customer(String name, Optional<Address> address) {}
public record Address(String city, String country) {}

Affine<Order, Customer> orderCustomer = Affine.of(
    Order::customer,
    (order, customer) -> new Order(order.id(), Optional.of(customer))
);

Affine<Customer, Address> customerAddress = Affine.of(
    Customer::address,
    (customer, address) -> new Customer(customer.name(), Optional.of(address))
);

Lens<Address, String> addressCity = Lens.of(
    Address::city,
    (addr, city) -> new Address(city, addr.country())
);

// Deep optional access: Order → city
Affine<Order, String> orderCity = orderCustomer
    .compose(customerAddress)
    .compose(addressCity);

// Usage
Order order = new Order("123", Optional.of(
    new Customer("Steve", Optional.of(new Address("NYC", "USA")))
));

Optional<String> city = orderCity.getOptional(order);  // Optional.of("NYC")
```

### Safe Map Access

```java
public record UserData(Map<String, Object> properties) {}

// Affine for a specific key
Affine<UserData, Object> userName = Affine.of(
    data -> Optional.ofNullable(data.properties().get("name")),
    (data, value) -> {
        Map<String, Object> newProps = new HashMap<>(data.properties());
        newProps.put("name", value);
        return new UserData(newProps);
    }
);

// With type casting
Affine<UserData, String> userNameString = userName.compose(
    Affine.of(
        obj -> obj instanceof String s ? Optional.of(s) : Optional.empty(),
        (obj, str) -> str
    )
);
```

### Dynamic Field Access

```java
// Affine for accessing a dynamic field
public static <T> Affine<Dynamic<?>, T> dynamicField(String name, Function<Dynamic<?>, Optional<T>> extract) {
    return Affine.of(
        d -> d.get(name).result().flatMap(extract),
        (d, value) -> d.set(name, /* create dynamic from value */)
    );
}

// Usage
Affine<Dynamic<?>, String> playerName = dynamicField("name",
    d -> d.asString().result()
);
```

## Affine vs Other Optics

| Optic      | Focus     | get             | set             |
|------------|-----------|-----------------|-----------------|
| Lens       | Exactly 1 | Always succeeds | Always succeeds |
| Prism      | 0 or 1    | May fail        | Always succeeds |
| **Affine** | 0 or 1    | May fail        | Always succeeds |
| Traversal  | 0 to N    | Returns list    | Always succeeds |

### Difference from Prism

- **Prism**: Focuses on a *case* of a sum type. Can *construct* the source from the focus.
- **Affine**: Focuses on an *optional part*. Cannot necessarily construct from focus.

```java
// Prism: Can construct Optional from value
Prism<Optional<String>, String> some = Prism.of(
    opt -> opt,
    Optional::of  // Can construct
);

// Affine: Just accesses optional value
Affine<User, String> userEmail = Affine.of(
    user -> user.email(),
    User::withEmail  // Sets within existing User
);
```

### When to Use Affine

✅ Use Affine when:
- Accessing optional fields
- Navigating through optional nested structures
- Field might not exist in all cases
- You need to set the value regardless of current presence

❌ Don't use Affine when:
- Field always exists (use Lens)
- Working with sum types where you can construct (use Prism)
- Need to access multiple values (use Traversal)

## Affine Laws

### 1. Set-Get Law

If you set a value, getting it returns that value (if successful):

```java
// If getOptional succeeds after set, it returns the set value
User user = ...;
String email = "new@example.com";
Optional<String> result = userEmail.getOptional(userEmail.set(user, email));
assert result.equals(Optional.of(email));
```

### 2. Get-Set Law

If you get and then set, nothing changes:

```java
// If getOptional(s).isPresent(), then set(s, getOptional(s).get()) == s
User user = new User("Steve", Optional.of("steve@example.com"));
Optional<String> email = userEmail.getOptional(user);
assert email.isPresent();
assert userEmail.set(user, email.get()).equals(user);
```

## Affine Summary

| Operation          | Description          | Returns       |
|--------------------|----------------------|---------------|
| `getOptional(S)`   | Try to get the value | `Optional<A>` |
| `set(S, A)`        | Set the value        | `S`           |
| `modify(S, A → A)` | Transform if present | `S`           |
| `compose(Lens)`    | Compose with lens    | `Affine`      |
| `compose(Prism)`   | Compose with prism   | `Affine`      |
| `compose(Affine)`  | Compose with affine  | `Affine`      |

---

## Related

- [Lens](lens.md) — For required fields
- [Prism](prism.md) — For sum type cases
- [Finder](finder.md) — For Dynamic data navigation
- [Optics Overview](index.md) — Optic hierarchy

