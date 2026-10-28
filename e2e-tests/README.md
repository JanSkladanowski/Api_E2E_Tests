# E2E Tests

Testy interfejsu IssueHub używają Selenium WebDriver i JUnit Jupiter.

## Struktura

| Pakiet | Rola |
| --- | --- |
| `tests` | Scenariusze i cykl życia JUnit w `BaseUiTest` |
| `pages` | Page Objects, selektory UI i adnotacje `@Step` na istotnych akcjach |
| `driver` | `BrowserDriverProvider`, implementacje Chrome/Firefox/Edge i `DriverFactory` |
| `config` | Wczytanie i walidacja ustawień z `ui-test.properties` |
| `data` | Unikalne tytuły i reguły sprzątania issues (ID albo awaryjne wyszukanie po tytule) |
| `client` | Żądania API używane wyłącznie przy sprzątaniu |
| `support` | Ogólny rejestr akcji cleanupu i artefakty diagnostyczne przy nieudanym teście UI |

`BrowserDriverProvider` jest wewnętrznym kontraktem: każda implementacja deklaruje obsługiwaną przeglądarkę i tworzy `WebDriver`. `DriverFactory` wybiera ją według ustawienia `browser`. Testy i Page Objects pracują z interfejsem Selenium `WebDriver`.

## Konfiguracja

Wartości domyślne są w `src/test/resources/ui-test.properties`:

~~~properties
uiBaseUrl=http://localhost:5173
apiBaseUrl=http://localhost:8080
browser=chrome
headless=true
browserBinary=
windowWidth=1440
windowHeight=900
waitSeconds=10
cleanupTimeoutSeconds=10
~~~

Każdy klucz można nadpisać przez `-D` Mavena. Zachowane są także wcześniejsze aliasy `-DbaseUrl` dla UI i `-DchromeBinary` dla Chrome. Wartość `browserBinary` dotyczy wybranej przeglądarki. Obsługiwane wartości `browser`: `chrome`, `firefox`, `edge`. Wybrana przeglądarka musi być zainstalowana; Selenium Manager rozwiązuje odpowiedni sterownik.

Przykład uruchomienia przeciwko serwowanemu przez Spring Boot UI:

~~~powershell
mvn test "-DuiBaseUrl=http://localhost:8080" "-Dbrowser=firefox" "-Dheadless=false"
~~~

Można też uruchomić backend w katalogu `application` i Vite w katalogu `frontend` (`npm install`, `npm run dev`), a następnie wykonać `mvn test` z domyślnymi adresami. Vite przekazuje żądania przeglądarki `/api` do backendu. Cleanup zawsze wywołuje backend bezpośrednio przez `apiBaseUrl`.

Po zmianie frontendu uruchom `npm run build`. Jeśli testujesz statyczny UI serwowany przez Spring Boot na porcie 8080, przebuduj i uruchom aplikację ponownie, aby korzystała z nowych zasobów.

## Izolacja i diagnostyka

Każdy test dostaje nową sesję przeglądarki i własny `TestResourceRegistry`. Rejestr przechowuje opisane akcje cleanupu różnych typów zasobów i uruchamia je w odwrotnej kolejności. `BaseUiTest` udostępnia `registerResourceForCleanup(description, cleanup)` dla przyszłych zasobów. Reguły specyficzne dla issues pozostają w `IssueCleanupRegistry`: test zapisuje unikalny tytuł **przed** próbą utworzenia zgłoszenia przez UI, co od razu rejestruje akcję w ogólnym rejestrze. Gdy UI pokaże utworzony wiersz, test dopisuje ID. `@AfterEach` usuwa potwierdzone issues bezpośrednio po ID. Dla niepotwierdzonego tworzenia wykonuje awaryjne wyszukanie po tytule: usuwa dokładnie jedno trafienie, pomija brak trafienia i zgłasza błąd przy wielu trafieniach. Błąd jednej akcji nie zatrzymuje pozostałych; wyjątki są zbierane i zgłaszane po zakończeniu sprzątania.

Awaryjna ścieżka korzysta z `GET /api/v1/issues/by-title?title=...`, który wyszukuje dokładny tytuł niezależnie od listy zgłoszeń i jej przyszłej paginacji. API zwraca jedno zgłoszenie, 404 przy braku wyniku lub 409 przy duplikatach; w razie 409 cleanup zgłasza błąd i niczego nie usuwa. Klient dodatkowo sprawdza tytuł i dodatnie ID w odpowiedzi. Każde niepotwierdzone issue wywołuje osobne wyszukanie. Tytuł z losowym UUID zmniejsza ryzyko kolizji, ale nie dowodzi własności zasobu na współdzielonym środowisku. Odpowiedź `404` przy usuwaniu znanego ID pozostaje błędem.

`IssueCleanupFallbackE2eTest` sprawdza tę ścieżkę z prawdziwą przeglądarką: tworzy issue przez UI, potwierdza je w API, celowo **nie zapisuje ID w rejestrze cleanupu**, uruchamia akcję sprzątania używaną również przez `@AfterEach` i potwierdza brak issue w API. Uruchom go osobno po starcie backendu z endpointem `/by-title` i frontendu:

~~~powershell
mvn test "-Dtest=IssueCleanupFallbackE2eTest"
~~~

Logi konsolowe i plikowe (`target/logs/e2e-tests.log`) zawierają nazwę testu, wybraną przeglądarkę, działania UI, zarejestrowane tytuły i ID oraz żądania cleanupu. Gdy metoda testowa kończy się błędem i przeglądarka została uruchomiona, przed jej zamknięciem zapisywane są zrzut ekranu i HTML w `target/e2e-failures` oraz jako załączniki Allure. Asercje pozostają w metodach testowych; `try/catch` występuje tylko w kodzie zarządzającym zasobami i obsługą błędów.

Szybkie testy konfiguracji, wyboru dostawcy WebDriver i cleanupu można uruchomić bez działającej aplikacji:

~~~powershell
mvn test "-Dtest=ApiCleanupClientTest,IssueCleanupRegistryTest,TestResourceRegistryTest,UiTestConfigTest,DriverFactoryTest,AllureStepConfigurationTest"
~~~

## Raport Allure

Testy UI mają tag `e2e`, a testy samego frameworka tag `framework`. Bez filtra `mvn test` uruchamia obie grupy. Aby raport zawierał tylko trzy scenariusze UI, uruchom z katalogu `e2e-tests`:

~~~powershell
mvn clean test "-Dgroups=e2e"
mvn allure:serve
~~~

Wyniki po teście są w `target/allure-results`; raport zawiera kroki scenariusza, przeglądarkę, tryb headless i adres UI. Przy błędzie metody testowej otrzymasz także zrzut ekranu i kod HTML strony. Serwer raportu zatrzymasz przez `Ctrl+C`.

Kroki akcji są tworzone przez `@Step` na istotnych metodach Page Objectów w pakiecie `pages`. Weryfikacje pozostają w testach i mają krótkie, biznesowe nazwy dzięki `Allure.step(...)`. Nie używamy automatycznego raportowania każdej asercji JUnit, ponieważ generowało techniczne, trudne do czytania nazwy (szczególnie dla komunikatów typu `Supplier<String>`). Maven Surefire uruchamia testy z agentem AspectJ wymaganym przez adnotacje `@Step`.

Test `AllureStepConfigurationTest` sprawdza to bez uruchamiania przeglądarki.

Plugin Maven ma na stałe ustawiony katalog instalacji `.allure-fresh`, więc nie trzeba dopisywać `-Dallure.install.directory` do komend. Przed ponownym uruchomieniem `mvn allure:serve` zatrzymaj poprzedni serwer przez `Ctrl+C`: na Windows działający proces blokuje jego `node.exe`. Nie usuwaj `target/allure-results`, jeśli chcesz zachować wyniki testów.

Same testy frameworka uruchomisz komendą `mvn test "-Dgroups=framework"`. Jeśli serwer Allure nie uruchomi się przez plugin Maven, po `mvn allure:install` możesz otworzyć wyniki bezpośrednio przez `node .allure-fresh/allure-3.17.0/node_modules/allure/cli.js open target/allure-results`.
