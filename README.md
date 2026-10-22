# MAP-Mobile

MAP-Mobile to aplikacja na Androida do zapisywania podróży i odwiedzonych miejsc.

## Uruchomienie

1. Otwórz projekt w Android Studio i poczekaj na synchronizację Gradle.
2. Uruchom emulator albo podłącz odblokowane urządzenie z włączonym debugowaniem USB.
3. W Android Studio wybierz konfigurację `app` i kliknij **Run**.

## Uruchamianie testów

1. Jednostkowych:
~~~
.\gradlew.bat :app:testDebugUnitTest
~~~
2. Testów instrumentowane Androida:
~~~
.\gradlew.bat :app:connectedDebugAndroidTest
~~~
3. Obu jednocześnie:
~~~
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest
~~~