# Dependency Injection

This package contains Hilt modules that configure application dependencies.

Add the following here:
- providers and bindings,
- dependency scopes, such as `@Singleton`.

DI modules connect interfaces from `domain` to their implementations in `data`.
Do not put business logic or screen code here.