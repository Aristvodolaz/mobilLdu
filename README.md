# MobilLdu Project

Мобильное приложение и backend-сервер для фотографирования артикулов и отправки данных в базу данных MS SQL Server.

## Структура проекта

- `backendLduPhoto/` — Backend API на Node.js.
- `mobilLduPhoto/` — Мобильное приложение под Android (Kotlin / Jetpack Compose).

---

## 1. Backend (`backendLduPhoto`)

Backend-сервер принимает фотографии и артикулы от мобильного приложения, сохраняет изображения и записывает информацию в базу данных MS SQL.

### Установка и запуск

1. Перейдите в папку бэкенда:
   ```bash
   cd backendLduPhoto
   ```
2. Установите зависимости:
   ```bash
   npm install
   ```
3. Скопируйте файл настроек окружения `.env.example` в `.env` и настройте параметры подключения (файл **не в git**, на сервере создаётся вручную):
   ```bash
   cp .env.example .env
   nano .env   # или vi .env
   ```
   Пример для сервера (значения как в рабочем `test_db.js`):
   ```env
   PORT=3030
   DB_USER=sa
   DB_PASSWORD=***
   DB_SERVER=PRM-SRV-MSSQL-01.komus.net
   DB_PORT=59587
   DB_NAME=SPOe_rc
   DB_ENCRYPT=true
   ```
4. Запустите в режиме разработки:
   ```bash
   npm start
   ```

### Управление через PM2

В проекте настроен конфигурационный файл `ecosystem.config.js` для запуска через PM2:

- **Запуск**: `npm run pm2:start`
- **Остановка**: `npm run pm2:stop`
- **Перезапуск**: `npm run pm2:restart`
- **Статус**: `npm run pm2:status`
- **Логи**: `npm run pm2:logs`
- **Удалить процесс и запустить заново**: `npm run pm2:delete` → `npm run pm2:start`

#### Если PM2 не стартует или логи пустые

1. Обновите PM2 (устраняет предупреждение *In-memory PM2 is out-of-date*):
   ```bash
   pm2 update
   ```
2. Убедитесь, что на сервере есть `.env` в `backendLduPhoto/` (скопируйте из `.env.example` и заполните `DB_*`, `PORT`).
3. Проверьте БД вручную: `node test_db.js` (из папки `backendLduPhoto`).
4. Пересоздайте процесс:
   ```bash
   npm run pm2:delete
   npm run pm2:start
   npm run pm2:status
   ```
5. Если статус `errored` / много рестартов — смотрите `pm2 logs ldu-photo-backend --err --lines 200`.
6. В `pm2 describe` должно быть **`exec mode │ fork_mode`**, не `cluster_mode`. Cluster с Express даёт `errored` и пустые логи — в `ecosystem.config.js` задано `exec_mode: 'fork'`.
7. Проверка без PM2: `node server.js` — ошибка БД или `.env` будет видна сразу в терминале.

---

## 2. Мобильное приложение (`mobilLduPhoto`)

Нативное мобильное приложение на Kotlin для Android.

### Сборка и запуск

Для сборки APK файла:
```bash
cd mobilLduPhoto
./gradlew assembleDebug
```
Готовый файл APK будет находиться по пути:
`mobilLduPhoto/app/build/outputs/apk/debug/app-debug.apk`

---

## Лицензия

Собственность Комус.
