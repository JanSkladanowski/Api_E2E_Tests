# QA Learning Lab

Projekt szkoleniowy do nauki automatyzacji testów w Javie.

## Moduły

- `application` — aplikacja typu bug tracker z REST API i UI.
- `api-tests-lombok` — testy API oparte o JUnit Jupiter, REST Assured i Lombok.
- `e2e-tests` — testy UI oparte o Selenium WebDriver i JUnit Jupiter.
- `frontend` — interfejs napisany w TypeScript i budowany przez Vite.

## Uruchomienie lokalne

Uruchom backend w katalogu `application`:

```text
mvn spring-boot:run
```

Uruchom frontend w katalogu `frontend`:

```text
npm ci
npm run dev
```

Testy API uruchom w katalogu `api-tests-lombok`, a testy UI w katalogu `e2e-tests`:

```text
mvn test
```

Testy API używają domyślnie `http://localhost:8080`. Testy E2E otwierają frontend pod `http://localhost:5173` i używają API pod `http://localhost:8080`.

## CI

[Workflow CI](.github/workflows/ci.yml) uruchamia się po pushu na `main`, dla pull requestów do `main` oraz ręcznie. Buduje frontend i aplikację, uruchamia testy API oraz E2E w Chrome headless. Raporty Surefire, wyniki Allure i log aplikacji są dostępne w artefakcie `test-diagnostics`, również gdy testy zakończą się błędem.

## Zasady projektowe

- Java 17 i Maven,
- SLF4J oraz Logback,
- osobne dane i cleanup dla każdego testu,
- testy niezależne od kolejności uruchamiania,
- jawne czekanie w testach UI,
- brak danych wrażliwych w logach.
