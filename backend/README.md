# Loyalty Web — Spring backend и веб-интерфейс

Самостоятельный модуль Java 21 / Spring Boot 3.5.16 / PostgreSQL 17. Существующее JavaFX/JDBC-приложение в корневом `src` сохранено. Новый модуль использует отдельную БД `loyalty_web`, отдельный Docker volume и не мигрирует старые таблицы автоматически. Главная сущность — клиент программы лояльности; справочник — сегмент. Сумма покупок задаётся менеджером: перенос покупок, карт и бонусных транзакций из старого приложения не выполняется.

## Запуск из корня репозитория

1. Если `.env` отсутствует, скопируйте `.env.example`. Если существует — сохраните его значения.
2. Заполните `POSTGRES_USER`, `DOCKER_POSTGRES_PASSWORD`, `JWT_SECRET_BASE64`, `BOOTSTRAP_ADMIN_PASSWORD`. JWT-секрет: `openssl rand -base64 64`. Пароль: 12–72 **байта**, строчная/заглавная латинские буквы и цифра. Не сохраняйте секреты в Git.
3. Выполните:

```sh
docker compose --env-file .env -f backend/docker-compose.yml up -d --build
docker compose --env-file .env -f backend/docker-compose.yml ps
```

Веб-интерфейс: http://localhost:8080 (или `APP_PORT`). Логин администратора — `BOOTSTRAP_ADMIN_EMAIL`, начальный пароль — `BOOTSTRAP_ADMIN_PASSWORD` из `.env`. При первом входе обязательна смена пароля. Bootstrap создаёт администратора только при пустой таблице пользователей; изменение `.env` не сбрасывает существующий пароль.

PostgreSQL доступен только на `127.0.0.1:55433`, backend — только на `127.0.0.1:8080`. Оба сервиса имеют healthcheck. Данные сохраняются в именованном volume `loyalty-web_loyalty_web_data` после обычного `docker compose ... down`. `down -v` удаляет том и данные — для обычной остановки его не используйте.

Для локального запуска без Docker задайте переменные среды (Maven сам не загружает `.env`), включая `LOYALTY_DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET_BASE64`, `BOOTSTRAP_ADMIN_PASSWORD`:

```sh
mvn -f backend/pom.xml spring-boot:run
```

Фронтенд находится в `frontend/`, упаковывается Maven в статические ресурсы backend. Отдельного Node runtime и dev proxy не требуется. После изменения фронтенда пересоберите JAR / контейнер.

## Роли и безопасность

- `VIEWER`: просмотр клиентов, заметок, справочника, аналитики и экспорт.
- `EDITOR`: дополнительно CRUD клиентов и заметок, архив/восстановление, импорт и массовые операции.
- `ADMIN`: всё перечисленное, справочник, пользователи, аудит, окончательное удаление архивной записи.

JWT по умолчанию живёт 15 минут. Refresh — случайный токен на 14 дней, в БД хранится только SHA-256, привязка к пользователю и срок. Refresh ротируется под блокировкой строки; повторное использование старого токена отклоняется. Logout отзывает текущую refresh-сессию; access JWT остаётся действительным до истечения. Смена/сброс пароля и изменение пользователя отзывают все refresh-сессии и инвалидируют JWT через версию пользователя. Отключение пользователя действует немедленно.

В браузере access хранится в памяти, refresh — в HttpOnly/SameSite=Strict cookie. Для `/refresh` и `/logout` обязателен заголовок `X-Refresh-Request: 1`; CORS разрешает только `CORS_ALLOWED_ORIGINS`. В HTTPS окружении установите `COOKIE_SECURE=true`. Для мобильного приложения используйте `mobile: true` при входе и JSON refresh-токен, как описано в `mobile/README.md`.

После пяти неудачных попыток за 15 минут вход блокируется на 15 минут по логину и IP; состояние хранится в БД. Реальный IP берётся из TCP-соединения, произвольный `X-Forwarded-For` не считается доверенным. При установке за reverse proxy отдельно настройте доверенные прокси.

Администратор назначает временный пароль при создании/сбросе, API его не возвращает. Пароли хешируются BCrypt (cost 12). API никогда не возвращает JPA-сущности или хеши паролей.

## REST API

Все бизнес-запросы: `Authorization: Bearer <accessToken>`. `items` из задания заменено на `customers`, `references` — на `segments`.

| Метод | Адрес | Назначение |
|---|---|---|
| POST | `/api/auth/login` | `{login,password,mobile:false}` |
| POST | `/api/auth/refresh` | Cookie либо `{refreshToken}`; заголовок X-Refresh-Request |
| POST | `/api/auth/logout` | Отзыв refresh-сессии; тот же заголовок |
| GET | `/api/auth/me` | Текущий пользователь |
| POST | `/api/auth/password` | `{currentPassword,newPassword}`; затем повторный вход |
| GET, POST | `/api/customers` | Страница / создание |
| GET, PUT | `/api/customers/{id}` | Карточка / изменение |
| DELETE | `/api/customers/{id}?version=N` | Архив |
| POST | `/api/customers/{id}/restore?version=N` | Восстановление |
| DELETE | `/api/customers/{id}/permanent?version=N` | Удаление архивного клиента, ADMIN |
| POST | `/api/customers/bulk` | Частичные результаты по каждому UUID |
| GET, POST | `/api/customers/{id}/notes` | История / создание заметки |
| PUT, DELETE | `/api/customers/{id}/notes/{noteId}` | Изменение / удаление (DELETE с `?version=N`) |
| POST | `/api/customers/import/preview` | Multipart `file`, `mode`: NEW_ONLY или UPSERT |
| POST | `/api/customers/import/apply` | `{previewId}` |
| GET | `/api/customers/export` | CSV с текущими фильтрами и сортировкой |
| GET, POST | `/api/segments` | Справочник / создание |
| PUT | `/api/segments/{id}` | `{name,active,version}`, ADMIN |
| GET | `/api/analytics/summary` | Сводка по всей базе, включая архив |
| GET, POST | `/api/admin/users` | Страница / создание пользователя |
| PUT | `/api/admin/users/{id}` | `{login,name,roles,active,version}` |
| POST | `/api/admin/users/{id}/reset-password` | `{temporaryPassword}` |
| GET | `/api/admin/audit` | `q`, `page`, `size`, `sort` |
| POST | `/api/admin/swagger-session` | Открыть 5-минутный доступ браузера к документации |

Создание / изменение клиента:

```json
{
  "code": "C-1001", "name": "Анна Иванова", "email": "anna@example.com",
  "phone": "+7 900 000-00-00", "description": "Постоянный клиент",
  "segmentId": "UUID из GET /api/segments", "status": "ACTIVE",
  "totalPurchases": 15000.00, "marketingConsent": true,
  "tags": ["vip"], "version": 0
}
```

Для изменения передавайте полученный `version`. При конфликте — HTTP 409; перечитайте карточку. Уровень вычисляется: BASE < 10000; SILVER от 10000; GOLD от 50000. Созданная запись содержит UUID, code, createdAt/updatedAt, version, archived. Статусы: ACTIVE, BLOCKED, INACTIVE. Для новых/переназначенных клиентов можно выбирать только включённый сегмент.

### Поиск, пагинация, массовые операции

`GET /api/customers?q=анна&segmentId=UUID&status=ACTIVE&marketingConsent=true&tier=GOLD&tag=vip&archived=false&amount=10000&amountOp=gte&sort=name,asc&page=0&size=20`

Все фильтры необязательны. Без `archived` возвращаются и активные, и архивные записи. Поиск по имени, коду, email и телефону, без учёта регистра; `%`/`_` ищутся как обычные символы. Операторы суммы: `eq`, `gt`, `gte`, `lt`, `lte`. Сортировка: name, code, totalPurchases, createdAt, updatedAt, status + asc/desc. Страницы от 0, размер 1–100, стабильный дополнительный порядок по UUID. Поиск строится JPA Criteria/Specifications, пользовательские строки не конкатенируются с SQL/JPQL.

Ответ страницы: `{content,totalElements,totalPages,number,size}`.

```json
{"ids":["UUID"],"action":"ADD_TAG","tag":"vip","versions":{"UUID":3}}
```

Массовые действия: ARCHIVE, RESTORE, STATUS (поле status), ADD_TAG, REMOVE_TAG (поле tag), до 100 UUID. Каждая запись обрабатывается в отдельной транзакции, ответ — `[{id,success,message}]`. Отсутствующая/устаревшая версия даёт ошибку конкретной записи.

### CSV

UTF-8 (BOM поддерживается), запятая, экранирование по RFC 4180. Строгий порядок колонок:

```csv
code,name,email,phone,description,segmentId,status,totalPurchases,marketingConsent,tags
C-001,Анна,anna@example.com,,,UUID,ACTIVE,10000.00,true,vip|retail
```

Максимум 2 МБ, 5000 строк, теги разделены `|`. Preview возвращает UUID, количество корректных/ошибочных строк и ошибки по номерам; сохраняется в БД на 30 минут с привязкой к пользователю. Apply разрешён только после исправления всех строк, однократно, атомарно. Конфликты между preview/apply откатывают весь импорт. NEW_ONLY отклоняет существующие коды, UPSERT обновляет по code с контролем сохранённой версии. Архивный признак существующего клиента сохраняется.

Экспорт экранирует формулы электронных таблиц префиксом `'`; импорт снимает этот защитный префикс, в том числе у телефонов `+7…`. Символ `|` и переводы строк внутри отдельного тега запрещены, поскольку `|` разделяет теги в CSV.

### Ошибки, аудит и Swagger

Единый формат: `{timestamp,status,message,correlationId,fields}`. Коды: 400 валидация, 401 нет/неверная авторизация, 403 права/обязательная смена пароля, 404 запись не найдена, 409 конфликт версии/уникальности, 429 блокировка входа. Stack trace клиенту не отправляется. Корреляция возвращается в `X-Correlation-ID` и записывается в аудит. Допустимый входной ID — буквы, цифры, дефис, до 80 символов; иначе сервер создаёт UUID.

Администратор открывает Swagger кнопкой **Swagger API ↗** в боковой панели. Адрес `/swagger-ui.html`, OpenAPI `/v3/api-docs`; оба доступны только ADMIN. Кнопка выдаёт короткую HttpOnly cookie только для чтения документации. Для `Try it out` нажмите Authorize и укажите обычный Bearer JWT; cookie документации бизнес-API не авторизует.

## Проверки и структура

```sh
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package
node --check frontend/app.js
```

H2 в режиме PostgreSQL запускает те же Flyway-миграции, затем Hibernate `ddl-auto: validate`. Интеграционные тесты проверяют Spring Context, обязательную смену пароля, роли, отключение, refresh/logout, ограничение входов, CRUD, фильтры, версии, заметки, массовые операции, CSV и аналитику. Отдельные тесты — границы уровней, политика паролей, защита CSV от формул.

- `src/main/java/com/loyalty`: DTO, JPA, репозитории, сервисы, контроллеры и Security.
- `src/main/resources/db/migration`: Flyway, FK, CHECK, индексы и таблицы.
- `../frontend`: адаптивный интерфейс без сборщика.
- `../mobile`: транспортный адаптер и инструкция для будущего мобильного клиента.

Для переноса старой клиентской базы подготовьте CSV с кодами и новым segmentId, выполните preview/apply. Перенос старых бонусных операций — отдельная миграция предметной области.
