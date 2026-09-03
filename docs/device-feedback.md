# Device feedback: Samsung Galaxy S21

Статус: debug APK зібрано на host; очікує встановлення й перевірки користувачем на Galaxy S21.

Host artifact: `android-app/build/outputs/apk/debug/android-app-debug.apk` (generated, не комітиться).

## Як передати фідбек

Не вставляйте приватні диктовки в Git. Для кожного сценарію достатньо записати застосунок, тип
поля, приблизну довжину фрази, затримку та спостережувану категорію проблеми.

| Сценарій | Очікування | Статус | Нотатки |
|---|---|---|---|
| Установити APK | Package `com.thebifel.bubnilka` встановлюється | Не перевірено | Device-only |
| Увімкнути й вибрати Бубнілку | IME з'являється у системному списку | Не перевірено | Device-only |
| Диктовка українською | Видно partial, у поле йде лише styled final | Заблоковано → виправлено в коді | На пристрої немає on-device recognizer; додано system fallback |
| Автозавантаження STT-моделі | Після fallback Android отримує запит на `uk-UA` offline-модель | Не перевірено | Система може показати підтвердження або відкласти download |
| П'ять стилів | Стиль змінює форму, але не факти | Не перевірено | Device-only |
| Undo / Regenerate | Заміна лише при точному збігу суфікса | Не перевірено | Device-only |
| Backspace | Видаляє selection або попередній Unicode code point | Не перевірено | Додано після першого UX feedback |
| Немає мережі / квоти | Є Retry та Insert raw, без циклу retry | Не перевірено | Device-only |
| Password / PIN | Мікрофон і cloud-виклик недоступні | Не перевірено | Device-only |
| Зміна клавіатури | Системний picker відкривається | Не перевірено | Device-only |
| Автоматичне слухання | Після відкриття звичайного поля диктовка стартує без tap | Перевірено на emulator | На фізичному пристрої перевірити permission і реальне аудіо |
| UA / EN | Обрана мова визначає locale системного STT | Перевірено контрактами | Точність — device-only |
| UA → EN / EN → UA | Gemini перекладає без втрати смислів, імен, чисел і посилань | Перевірено prompt-контрактами | Якість перекладу — device-only |
| Preview scroll | Нові partials тримають низ; ручний scroll угору призупиняє follow | Перевірено кодом і layout | Довга жива диктовка — device-only |
| Поле + голос | Повне поле й нова диктовка об'єднуються та замінюються лише при exact match | Перевірено unit tests | Перевірити у Notes/Telegram/Gmail |
| Структуровано | Зберігає формулювання; додає лише пунктуацію, абзаци й доречну нумерацію | Перевірено prompt-контрактом | Оцінити на довгому повідомленні |

## Метрики першого проходу

- Час від завершення мовлення до вставки: —
- Firebase `gemini_generate` cold/warm timings: очікують вимірювання новою збіркою
- Помилки локального STT: `isOnDeviceRecognitionAvailable=false`; реалізовано системний fallback
- Несумісні застосунки або поля: —
- Побажання до розміру клавіш/панелі: —

## Firebase перед першим cloud-тестом

1. Створити Spark-проєкт без Cloud Billing і Android app `com.thebifel.bubnilka`.
2. Увімкнути Firebase AI Logic із Gemini Developer API.
3. Заповнити ignored `firebase.local.properties` і перебілдити APK.
4. Додати App Check debug token з Logcat у Firebase Console.
5. Переконатися, що модель `gemini-3.5-flash-lite` доступна без billing.
