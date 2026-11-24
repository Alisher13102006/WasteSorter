# Мониторинг и аналитика (задание 13)

## Подключение Firebase
1. Firebase Console → Добавить проект → Android-приложение `kz.wastesorter.app`
   (и при необходимости `.staging`), SHA-1 upload-ключа: `keytool -list -v -keystore ~/.android-keys/wastesorter-upload.jks`.
2. Скачать `google-services.json` в `app/` (файл в .gitignore). Gradle сам подключит плагины
   google-services и Crashlytics, в коде станет `BuildConfig.FIREBASE_ENABLED = true`.
3. В CI: содержимое файла → Secret `GOOGLE_SERVICES_JSON`.

## Тестовый крэш
Release или staging → долгое нажатие на строку «Версия …» внизу экрана → приложение падает → открыть его снова (отчёт уходит
при следующем старте) → Crashlytics → Issues → `RuntimeException: Test Crash`.

Расшифровка стектрейса:
- Crashlytics: плагин при `assembleRelease` загружает mapping.txt сам (`uploadCrashlyticsMappingFileRelease`),
  стектрейс в консоли сразу читаемый;
- вручную: `~/Library/Android/sdk/cmdline-tools/latest/bin/retrace app/build/outputs/mapping/release/mapping.txt stacktrace.txt`.

## Оповещения

| Оповещение | Где | Порог |
|------------|-----|-------|
| Доля ответов «не уверен» > 40 % (модель плохо работает на реальных фото) | Analytics → Custom alert по событию `classify`, `sure=false` | > 40 % за сутки |
| Новая fatal-ошибка (new issue) | Crashlytics → Alerts → email + Slack | любая новая |
| Регрессия (закрытая ошибка вернулась) | Crashlytics → Alerts | любая |
| Crash-free users ниже порога | Crashlytics → Alerts → Velocity / Firebase Alerts | < 99,5 % за 1 ч |
| ANR rate / плохое поведение | Play Console → Android Vitals → Оповещения | > 0,47 % |

## Analytics
События: `classify` (параметры `label` = plastic/glass/paper, `sure` = уверена ли модель), `share_result` (1.1.0+), а также
автоматические `first_open`, `session_start`, `app_update`. DebugView: `adb shell setprop debug.firebase.analytics.app kz.wastesorter.app`.

## Android Vitals (Play Console → Качество)
- ANR rate: порог 0,47 %; распознавание идёт в `Dispatchers.Default`, а не в UI-потоке. Модель грузится в `onCreate`, это кандидат на оптимизацию.
- Сбои: порог 1,09 %.
- Холодный старт: < 5 с (у release на эмуляторе 3,45 с из-за загрузки модели TFLite: `am start -W`); тёплый < 2 с.
