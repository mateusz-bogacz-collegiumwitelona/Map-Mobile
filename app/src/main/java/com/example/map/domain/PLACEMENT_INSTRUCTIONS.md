# Domain

The domain layer contains business logic that is independent of Android and the user interface.

Add the following here:

- domain models,
- repository interfaces,
- use cases and business rules.

Rules:

- Do not import classes from the `data` package.
- Do not depend on the Android SDK, Compose, or storage implementations.
- Represent external dependencies through interfaces.