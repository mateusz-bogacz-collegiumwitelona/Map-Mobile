# ViewModels

This package contains ViewModels that manage screen state and handle UI events.

ViewModels may call use cases from the domain layer and expose UI state to screens.
They should not access data sources or create classes such as `TokenManager` directly.

Keep screen-specific state in a ViewModel when it needs to survive configuration
changes or when handling it requires application logic. Simple, temporary visual
state can remain in a composable.