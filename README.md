# Bank Cards REST

## Описание

**Bank Cards REST** — REST‑API для управления пользователями и банковскими картами.

**Возможности:**
- Регистрация пользователя и аутентификация по JWT.
- Получение сведений о текущем пользователе.
- Управление пользователями (администратор): список/поиск.
- Карты: выпуск карты пользователю, список собственных карт (с пагинацией), просмотр карты.
- Операции с картами: пополнение баланса, запрос блокировки.
- История переводов: по карте и по пользователю.

**Технологии и архитектура:**
- Spring Boot (Java 17), Spring Web, Spring Security (JWT), JPA/Hibernate, Liquibase, PostgreSQL 16.
- OpenAPI/Swagger для документации (`/swagger-ui.html`, `/v3/api-docs`).
- Профили конфигурации `local` и `docker`; параметры через переменные окружения (`.env`).

Стартовый пример запросов — в разделе **«Мини‑сценарий»**, полная спецификация — в `docs/openapi.yaml`.

## Требования и установка инструментов

> Минимальный набор зависит от того, как вы запускаете проект.

### Вариант A — запуск **через Docker** (рекомендуется)
Нужно установить:
- **Docker Desktop** (Windows/macOS) или **Docker Engine** (Linux) с **Compose v2**  
  Проверка: `docker --version` и `docker compose version`

Опционально (для удобства): `curl`, `jq`, `psql` (клиент PostgreSQL).

### Вариант B — локальный запуск **без Docker**
Нужно установить:
- **Java 17 (JDK)**
  Проверка: `java -version`
- **Maven 3.9+** — для сборки Spring Boot.  
  Проверка: `mvn -v`
- **PostgreSQL 16+** (сервер) — локальная БД на `localhost:5432` или измените порт в `application.yml`.  
  Проверка: `psql --version` и что сервер запущен.
- (Опционально) `curl`, `jq` — для примеров запросов в README.

### Быстрые команды установки

**Ubuntu/Debian**
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven docker.io docker-compose-plugin postgresql-client curl jq
# добавьте своего пользователя в группу docker (перелогиньтесь после команды)
sudo usermod -aG docker "$USER"
```

**macOS (Homebrew)**
```bash
brew install openjdk@17 maven docker postgresql curl jq
# Docker Desktop скачайте из App Store/сайта и запустите (Compose входит в состав)
```

**Windows (PowerShell + Chocolatey)**
```powershell
choco install -y temurin17-jdk maven git curl jq
# Docker Desktop установите через официальный установщик и включите WSL2 backend
```

### Проверка окружения
```bash
java -version
mvn -v
docker --version
docker compose version
psql --version 
```

## Быстрый старт (Docker)

```bash
# 1) Создай .env из шаблона и заполни секреты
cp .env.example .env

# 2) Подними сервисы
docker compose up -d --build
```

Откроется:
- API: `http://localhost:${APP_PORT_HOST:-8080}`
- Swagger UI: `http://localhost:${APP_PORT_HOST:-8080}/swagger-ui.html`
- PostgreSQL: `localhost:${DB_PORT_HOST:-5434}`

> Данные БД хранятся в docker volume `bankcards-postgres` (см. compose).

## Переменные окружения

Используются из `.env`:

```dotenv
POSTGRES_DB=bankcards
POSTGRES_USER=postgres
POSTGRES_PASSWORD=*****

ENCRYPT_SECRET=*****   # для шифрования PAN
JWT_SECRET=*****       # для подписи JWT

DB_PORT_HOST=5432
APP_PORT_HOST=8080
```


## Профили приложения

`application.yml` содержит профили:
- **local** — подключение к Postgres на `localhost:5434`
- **docker** — подключение к сервису `db` внутри сети Compose

В `docker-compose.yml` активирован профиль `docker` и заданы `SPRING_DATASOURCE_*`.

## Миграции

Схема и стартовые данные применяются Liquibase автоматически при старте приложения (`spring.liquibase.change-log=classpath:db/migration`).

## Авторизация

JWT Bearer. Открытые эндпоинты: `/api/auth/**`, `POST /api/users/register`, `POST /api/users/register-admin`, Swagger.

### Мини‑сценарий

```bash
# Регистрация
curl -X POST http://localhost:8080/api/users/register   -H "Content-Type: application/json"   -d '{"username":"alice","password":"p@ssw0rd"}'

# Логин (получим token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login   -H "Content-Type: application/json"   -d '{"username":"alice","password":"p@ssw0rd"}' | jq -r .token)

# Защищённый запрос
curl http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"
```

## OpenAPI/Swagger

Актуальная спецификация: [`docs/openapi.yaml`](docs/openapi.yaml)

Swagger UI: `/swagger-ui.html`, JSON схемы: `/v3/api-docs`.

## Локальный запуск без Docker

Нужен JDK 17 и Postgres на `localhost:5432`:

```bash
mvn spring-boot:run
```
