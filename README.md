# MAP-Mobile

MAP-Mobile to aplikacja na Androida do zapisywania podróży i odwiedzonych miejsc.

## Uruchomienie aplikacji

1. Otwórz projekt w Android Studio i poczekaj na synchronizację Gradle.
2. Uruchom emulator albo podłącz odblokowane urządzenie z włączonym debugowaniem USB.
3. W Android Studio otwórz **Build > Select Build Variant** i wybierz `developmentDebug`.
4. Utwórz, lub dopisz do istniejącego local.properties:
```powershell
MAP_API_BASE_URL_DEVELOPMENT=http://10.0.2.2:8080/
MAP_API_BASE_URL_PRODUCTION=https://api.example.invalid/
```
5. Wybierz konfigurację `app` i kliknij **Run**.

## Aby zbudować wariant development i production:

```powershell
.\gradlew.bat :app:assembleDevelopmentDebug :app:assembleProductionRelease
```

## Uruchamianie testów

Testy jednostkowe dla wariantu development:

```powershell
.\gradlew.bat :app:testDevelopmentDebugUnitTest
```

Testy jednostkowe dla obu wariantów:

```powershell
.\gradlew.bat :app:testDevelopmentDebugUnitTest :app:testProductionDebugUnitTest
```

Testy instrumentowane dla wariantu development wymagają podłączonego urządzenia albo uruchomionego
emulatora:

```powershell
.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest
```

Testy instrumentowane dla obu wariantów wymagają podłączonego urządzenia albo uruchomionego
emulatora:

```powershell
.\gradlew.bat :app:connectedDevelopmentDebugAndroidTest :app:connectedProductionDebugAndroidTest
```

Aby uruchomić testy jednostkowe i instrumentowane dla obu wariantów:

```powershell
.\gradlew.bat :app:testDevelopmentDebugUnitTest :app:testProductionDebugUnitTest :app:connectedDevelopmentDebugAndroidTest :app:connectedProductionDebugAndroidTest
```