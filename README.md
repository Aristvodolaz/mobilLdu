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
3. Скопируйте файл настроек окружения `.env.example` в `.env` и настройте параметры подключения:
   ```bash
   cp .env.example .env
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
