# Orthodox Charity Donations POS

Android POS-приложение на Jetpack Compose для приёма пожертвований с фиксированными и произвольной суммами.

## Требования

- JDK 17
- Android SDK Platform 34
- Локальная платежная библиотека `SmartSkyPosLib_v1.9.17.aar`
## Подготовка

1. Положите AAR вручную в:
   - `app/libs/SmartSkyPosLib_v1.9.17.aar`

## Сборка

```bash
./gradlew assembleDebug
```

Debug APK будет в `app/build/outputs/apk/debug/`.

## Troubleshooting

### 1) `android.jar not found`
Проверьте, что установлен `platforms;android-34` и корректно настроены `ANDROID_HOME`/`ANDROID_SDK_ROOT`.

### 2) `Missing SmartSkyPosLib_v1.9.17.aar`
Положите файл `SmartSkyPosLib_v1.9.17.aar` в `app/libs/`.

### 3) `zxing_scanner_layout not found`
Убедитесь, что зависимость `com.journeyapps:zxing-android-embedded` не удалена из `app/build.gradle`.

## Технологии

- Jetpack Compose UI
- Интеграция с POS-платежным SDK через локальный AAR
- Встроенное аудио сопровождение для фонового и result-сценариев
