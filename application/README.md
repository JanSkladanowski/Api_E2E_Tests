# Application

Backend aplikacji `IssueHub` jest napisany w Spring Boot. Frontend znajduje się
w osobnym katalogu `frontend` i jest napisany w TypeScript.

`GET /api/v1/issues/by-title?title=...` wyszukuje zgłoszenie po dokładnym tytule.
Zwraca 200 z jednym zgłoszeniem, 404 przy braku wyniku i 409, gdy tytuł nie jest
unikalny. Endpoint nie korzysta z listy zgłoszeń, więc jej przyszła paginacja nie
zmieni tego kontraktu. Tytuł należy zakodować jako parametr zapytania.

## Uruchomienie lokalne

1. W katalogu `application` uruchom backend:

   ```powershell
   mvn spring-boot:run
   ```

2. W drugim terminalu, w katalogu `frontend`, uruchom frontend developerski:

   ```powershell
   npm install
   npm run dev
   ```

3. Otwórz `http://localhost:5173`. Vite przekazuje żądania `/api` do backendu
   działającego pod `http://localhost:8080`.

## Build frontendu dla Spring Boot

W katalogu `frontend` wykonaj `npm run build`. Vite najpierw sprawdza typy, a
następnie generuje pliki statyczne w `application/src/main/resources/static`.
Po buildzie Spring Boot udostępnia UI pod `http://localhost:8080`.
