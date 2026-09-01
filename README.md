# Бубнілка

Бубнілка — Android-клавіатура, що перетворює локально розпізнану диктовку на короткий або
стилізований текст і вставляє готовий результат у поточне поле.

## Межа MVP

- Android 12+; перший тестовий пристрій — Samsung Galaxy S21 з Android 15.
- Розпізнавання голосу виконує тільки системний on-device `SpeechRecognizer`.
- До Firebase AI Logic надсилається лише фінальний текст диктовки, без вмісту поля чи буфера
  обміну.
- Транскрипти, відповіді та аудіо не зберігаються і не потрапляють у логи.
- KMP-модуль містить спільні правила й контракти; iOS-таргет зараз не створюється.
- Proxmox потрібен лише як build-host і не є runtime-залежністю застосунку.

## Перевірка

```bash
./scripts/bootstrap-android.sh
./scripts/verify.sh
```

Чистий checkout збирається без Firebase-конфігурації. Для реального cloud-перетворення скопіюйте
`firebase.local.properties.example` у `firebase.local.properties` та заповніть ідентифікатори
власного Firebase Spark-проєкту.

У Firebase Console потрібно зареєструвати Android app `com.thebifel.bubnilka`, увімкнути AI Logic
з Gemini Developer API, лишити проєкт на Spark без Cloud Billing і зареєструвати debug-токен App
Check із першого запуску. MVP використовує стабільну безкоштовну модель
`gemini-3.5-flash-lite`; release-variant налаштований на Play Integrity.

Локальні quality gates можна встановити командою:

```bash
./scripts/install-git-hooks.sh
```

Специфікація, план і список задач лежать у `specs/001-voice-style-keyboard/`.
