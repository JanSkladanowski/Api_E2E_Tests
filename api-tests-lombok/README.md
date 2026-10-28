# API Tests — JUnit + Lombok

Testy API oparte bezpośrednio na JUnit Jupiter, REST Assured i Lombok.

`IssueApiTest` zawiera testy health, tworzenia, odczytu, usuwania, zmiany statusu, walidacji i filtrowania zgłoszeń. `CreateIssueRequest` wykorzystuje Lombokowe `@Builder`, a `TestDataFactory` centralizuje tworzenie poprawnych danych.

Każdy test dostaje własny `TestResourceRegistry`. Po odpowiedzi HTTP 201 helper `IssueApiTest.createIssue` rejestruje akcję usunięcia zgłoszenia po ID. Metoda `@AfterEach` uruchamia zarejestrowane akcje w odwrotnej kolejności, również gdy asercja zakończy test niepowodzeniem. Nieudana akcja nie blokuje kolejnych; błędy są zbierane i zgłaszane po zakończeniu sprzątania. Rejestr może w przyszłości obsłużyć inne typy zasobów, ale akcję cleanupu trzeba zarejestrować po ich utworzeniu.

`register(...)` zwraca `CleanupHandle`. Test DELETE po wywołaniu API sprawdza przez GET, że issue nie istnieje; dopiero wtedy oznacza uchwyt jako zakończony, aby `@AfterEach` nie wysłało drugiego DELETE. Jeżeli zasób nadal istnieje, akcja pozostaje zaplanowana do sprzątania. `ApiClient.deleteIssue(...)` zwraca surową `Response` dla asercji scenariusza, a `deleteIssueForCleanup(...)` wymaga HTTP 204; 404 podczas niezaplanowanego usuwania jest błędem cleanupu.

Mechanizm rejestru można sprawdzić bez uruchomionej aplikacji:

```text
mvn test "-Dtest=TestResourceRegistryTest"
```

Uruchomienie z tego katalogu:

```text
mvn test
```

Sam scenariusz usuwania (wymaga działającego backendu):

```text
mvn test "-Dtest=IssueApiTest#shouldDeleteCreatedIssue"
```

Domyślny adres aplikacji jest ustawiony w `src/test/resources/api-test.properties` pod kluczem `baseUrl`. Można go nadpisać parametrem JVM `-DbaseUrl=http://localhost:9090` lub zmienną środowiskową `API_BASE_URL`. Pierwszeństwo ma parametr JVM, potem zmienna środowiskowa, a na końcu wartość z pliku. Pusty lub niepoprawny URL powoduje błąd zamiast cichego przełączenia na inne środowisko. Akceptowane są absolutne adresy HTTP(S), a końcowy ukośnik jest usuwany. `mvn test "-Dtest=ApiTestConfigTest"` sprawdza tę logikę bez backendu.

Raport Allure można wyświetlić przez:

```text
mvn allure:serve
```
