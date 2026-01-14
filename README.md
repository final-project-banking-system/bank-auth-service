# Auth Service (Сервис авторизации)

Auth Service — это сервис аутентификации и авторизации в рамках микросервисной банковской системы.  
Он отвечает за регистрацию пользователей, логин, refresh JWT-токенов, управление пользовательскими сессиями, а также за 
публикацию доменных событий через Kafka (через Outbox-паттерн).

---

## Основные возможности

- Регистрация пользователей
- Аутентификация по логину и паролю
- Выпуск JWT access token
- Обновление access token с использованием refresh token
- Управление пользовательскими сессиями
- Proxy-доступ к другим сервисам
- Публикация событий в Kafka (USER_CREATED, USER_LOGIN, SYSTEM_ERROR)
- JWKS endpoint для валидации JWT другими сервисами

---

## Используемые технологии

- Java 17
- Spring Boot 3
- Spring Security (JWT, OAuth2 Resource Server)
- Spring Data JPA
- PostgreSQL
- Liquibase
- Apache Kafka
- Docker / Docker Compose

---

## Переменные окружения

Пример `.env` файла:

```env
DB_NAME=auth_db
DB_USER=auth_user
DB_PASSWORD=TO_CHANGE

SPRING_DATASOURCE_URL=jdbc:postgresql://auth_db:5432/auth_db
SPRING_DATASOURCE_USERNAME=auth_user
SPRING_DATASOURCE_PASSWORD=TO_CHANGE

JWT_ISSUER=bank-auth-service
JWT_ACCESS_TTL=900

JWT_KEYSTORE_PATH=classpath:keystore/auth-jwt.p12
JWT_KEYSTORE_PASSWORD=TO_CHANGE
JWT_KEY_ALIAS=auth-jwt
JWT_KEY_PASSWORD=TO_CHANGE
```

---

## Используемые Kafka топики

- `auth.users` — события регистрации пользователей (USER_CREATED)
- `auth.logins` — события логина пользователей (USER_LOGIN)
- `system.errors` — системные ошибки сервиса

---

## Как запустить локально

### Запуск через Docker Compose

1. Поднять инфраструктуру и сервисы:

```bash
docker compose up -d
```
2. Проверить логи Auth Service:

```bash
docker logs -f auth-service
```

---

## API Endpoints

### Регистрация пользователя

**POST** `/auth/register`

```json
{
  "login": "user1",
  "password": "password123",
  "email": "user1@mail.com"
}
```

### Логин пользователя

**POST** `/auth/login`

```json
{
  "login": "user1",
  "password": "password123"
}
```

### Обновление access token

**POST** `/auth/refresh`

```json
{
  "refreshToken": "refresh-token"
}
```

### Выход из системы

**POST** `/auth/logout`

```json
{
  "refreshToken": "refresh-token"
}
```

### Получение активных сессий пользователя

**GET** `/auth/sessions`

Требуется заголовок авторизации:

```http
Authorization: Bearer <access_token>
```

### Отзыв пользовательской сессии

**POST** `/auth/sessions/{sessionId}/revoke`

Заголовок авторизации:

```http
Authorization: Bearer <access_token>
```

### JWKS endpoint

**GET** `/auth/.well-known/jwks.json`

Используется другими сервисами для получения публичного ключа  
и проверки JWT access token.

### Proxy-доступ к другим сервисам

**ANY** `/proxy/{serviceKey}/**`

Используется для проксирования запросов к другим сервисам системы  
(Core Banking Service, Client Profile Service, Notification Service).

## Примечания

- Refresh token хранится в базе данных только в виде SHA-256 хэша
- Access token является stateless JWT
- Сессии удаляются физически, без soft delete
- Liquibase используется как единственный источник истины схемы БД
- Все события публикуются через Outbox-паттерн