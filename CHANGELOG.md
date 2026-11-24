# Changelog

Все заметные изменения проекта. Формат — [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии — [Semantic Versioning](https://semver.org/lang/ru/). versionCode = MAJOR·10000 + MINOR·100 + PATCH.

## [Unreleased]

## [1.2.1] - 2026-11-24 (versionCode 10201)
### Fixed
- Критично: приложение закрывалось при запуске, если эко-статистика пустая (ArithmeticException, 1.2.0). Добавлен регрессионный тест.

## [1.2.0] - 2026-11-24 (versionCode 10200)
### Added
- «Эко-статистика»: сколько предметов каждого вида вы отсортировали (флаг `eco_stats_enabled`). Данные хранятся только на устройстве.

## [1.1.0] - 2026-11-24 (versionCode 10100)
### Added
- Кнопка «Поделиться» в карточке результата: контейнер и совет по сдаче отправляются в мессенджер (флаг `share_result_enabled`).
### Fixed
- Фото и результат пропадали при повороте экрана и смене темы. Состояние перенесено в ViewModel, модель больше не загружается заново при каждом повороте.

## [1.0.0] - 2026-11-24 (versionCode 10000)
### Added
- Варианты сборки debug / staging / release: свой applicationId, адрес API, имя и цвет иконки.
- Подпись release upload-ключом; ключ и пароли вне репозитория (keystore.properties / CI Secrets).
- R8: минификация, обфускация, сжатие ресурсов (классы TensorFlow Lite сохранены); mapping.txt для расшифровки крэшей.
- Firebase Crashlytics и Analytics (включаются при наличии google-services.json); долгое нажатие на версию вызывает тестовый крэш.
- Remote Config: минимальная поддерживаемая версия; Play In-App Updates.
- Строка с версией и типом сборки внизу экрана.
- CI (сборка, ktlint, Lint, unit-тесты, APK-артефакт) и CD по тегу v* (AAB -> internal -> production).
### Changed
- applicationId `com.example.wastesorter` -> `kz.wastesorter.app` (Google Play не принимает com.example).
- Версия по SemVer: 1.0 -> 1.0.0.

## [0.1.0] - 2026-11-18
### Added
- Распознавание пластика, стекла и бумаги моделью TensorFlow Lite (вход 224×224).
- Фото с камеры и из галереи, карточка результата с контейнером и советами по сдаче.

[Unreleased]: https://github.com/Alisher13102006/WasteSorter/compare/v1.2.1...HEAD
[1.2.1]: https://github.com/Alisher13102006/WasteSorter/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/Alisher13102006/WasteSorter/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Alisher13102006/WasteSorter/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Alisher13102006/WasteSorter/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/Alisher13102006/WasteSorter/releases/tag/v0.1.0
