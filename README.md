# Бубнілка

Бубнілка — Android-клавіатура, що перетворює диктовку на короткий або стилізований текст і
вставляє готовий результат у поточне поле.

## Межа MVP

- Android 12+; перший тестовий пристрій — Samsung Galaxy S21 з Android 15.
- Розпізнавання спочатку використовує системний on-device `SpeechRecognizer`; якщо він або
  українська модель недоступні, застосунок переходить на системний recognizer із доступом до
  мережі та просить службу завантажити offline-модель для наступних сесій.
- До Firebase AI Logic надсилається лише фінальний текст диктовки, без вмісту поля чи буфера
  обміну.
- Транскрипти, відповіді та аудіо не зберігаються і не потрапляють у логи. При fallback системна
  служба розпізнавання може передавати аудіо своєму cloud-провайдеру згідно з її налаштуваннями.
- KMP-модуль містить спільні правила й контракти; iOS-таргет зараз не створюється.
- Proxmox потрібен лише як build-host і не є runtime-залежністю застосунку.
- Клавіатура має власний Backspace; хмарна генерація пише в Logcat лише тривалість запиту без
  транскрипту чи відповіді.

## Перевірка

```bash
./scripts/bootstrap-android.sh
./scripts/verify.sh
```

### Headless emulator і текстовий огляд UI

На `ai-agent-hub` налаштовано KVM-прискорений AVD `bubnilka_api35`. Запуск є foreground-командою,
щоб життєвий цикл emulator не залежав від прихованого desktop-сеансу:

```bash
./scripts/android-emulator.sh start
# в іншому shell
./scripts/android-emulator.sh wait
./scripts/android-emulator.sh install
```

Замість скриншота UI можна читати як дерево з точними bounds, центрами та станами:

```bash
./scripts/android-screen.sh window --actionable # активне Activity
./scripts/android-screen.sh ime --actionable    # швидкий direct-dump клавіатури
./scripts/android-screen.sh tree                # обидва дерева повністю
./scripts/android-screen.sh tap 540 2232
./scripts/android-screen.sh screenshot          # fallback у /tmp
./scripts/android-benchmark.sh
```

Direct IME dump доступний лише у debug APK. Release manifest не експортує діагностичний receiver.

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
