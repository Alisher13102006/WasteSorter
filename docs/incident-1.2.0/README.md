# Инцидент 1.2.0: падение при запуске (задания 13–14)

- **Симптом:** после обновления до 1.2.0 приложение закрывается сразу при запуске.
- **Причина:** `EcoStats.percent()` делит на `total`, а у только что обновившихся пользователей статистика
  пустая (`total = 0`) → `ArithmeticException: divide by zero` при отрисовке карточки «Эко-статистика».
- **Почему не поймали:** unit-тест проверял только заполненную статистику.

## Стектрейс из release-сборки
`stacktrace-obfuscated.txt` — так его видно в Logcat и Crashlytics без mapping-файла: `ei.e`, `en.d`…

Расшифровка mapping-файлом той же версии 1.2.0 (R8 retrace из build-tools):
```
java -cp ~/Library/Android/sdk/build-tools/36.0.0/lib/d8.jar com.android.tools.r8.retrace.Retrace \
  app/build/outputs/mapping/release/mapping.txt docs/incident-1.2.0/stacktrace-obfuscated.txt
```
Результат: `stacktrace-retraced.txt`, первая строка — `EcoStats.percent(EcoStats.kt:18)`.
Crashlytics делает то же сам, если плагин загрузил mapping.txt при сборке.

## Исправление (1.2.1)
`if (total == 0) 0 else count * 100 / total` + регрессионный тест `EcoStatsEmptyTest`.
Хронология и время восстановления: `docs/RELEASE.md`, раздел «Hotfix и откат».
