# Android emulator capability on ai-agent-hub

Дата перевірки: 2026-09-01 UTC

## Поточний стан

- Build container: Proxmox LXC `107 ai-agent-hub`, unprivileged, 2 CPU, 4 GiB RAM, 512 MiB swap.
- Host: Intel N100, VT-x доступний, `/dev/kvm` існує на Proxmox host.
- Container: CPU flag `vmx` видимий, але `/dev/kvm` не передано.
- Android SDK: `adb` 37.0.1 встановлений; emulator package, system images та AVD відсутні.
- Підключених ADB-пристроїв під час перевірки немає.

Без `/dev/kvm` x86_64 emulator працюватиме через повну програмну трансляцію і не підходить для
регулярного UI feedback loop на виділених контейнеру ресурсах.

## Робочий варіант

1. Передати `/dev/kvm` у CT 107 і надати `aiagent` доступ до пристрою.
2. Бажано виділити контейнеру щонайменше 3 CPU та 6–8 GiB RAM на час emulator-сесій.
3. Встановити Android Emulator і одну x86_64 Google APIs system image.
4. Створити невеликий phone AVD та запускати його headless із software GPU rendering.

Після цього Codex зможе:

- запускати/зупиняти AVD без графічного вікна;
- встановлювати APK через `adb install -r`;
- відкривати Activity, натискати координати, вводити текст і перемикати IME;
- читати UI hierarchy через UIAutomator;
- отримувати PNG через `adb exec-out screencap -p` і аналізувати скриншоти;
- збирати Logcat і повторювати screenshot-driven UX перевірки.

Емулятор придатний для layout, onboarding, станів клавіатури, backspace та базового insertion flow.
Реальний телефон усе одно потрібен для достовірної перевірки мікрофона, системного STT, App Check,
Gemini latency та поведінки IME у сторонніх застосунках.

## Джерела

- [Android Emulator hardware acceleration](https://developer.android.com/studio/run/emulator-acceleration)
- [Command-line and headless emulator](https://developer.android.com/studio/run/emulator-commandline)
- [ADB and screencap](https://developer.android.com/tools/adb)
