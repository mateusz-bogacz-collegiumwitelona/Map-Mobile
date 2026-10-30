# Data

The data layer implements domain interfaces and works with data sources.

Add the following here:
- repository implementations,
- local and remote data sources,
- mappings from data models to domain models.

For example, `TokenManager` handles secure token storage and implements an interface defined in `domain`.

The `data` layer may depend on `domain`, but `domain` must not depend on `data`.