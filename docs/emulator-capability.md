# Android emulator на ai-agent-hub

Дата перевірки: 2026-09-02 UTC

## Поточний стан

- Build container: Proxmox LXC `107 ai-agent-hub`, unprivileged, 3 CPU, 5 GiB RAM, 1 GiB swap.
- Proxmox host: Intel N100; `/dev/kvm` передано в контейнер як `dev0` з mode `0666` без restart.
- `emulator -accel-check`: `KVM (version 12) is installed and usable`.
- Android Emulator 37.1.11, platform-tools 37.0.1, API 35 Google APIs x86_64 image.
- AVD: `bubnilka_api35`, Pixel 6 profile, 1080×2400, density 420, headless SwiftShader.
- Перевірений serial: `emulator-5554`, Android 15; debug APK встановлюється, IME вмикається і
  керується через ADB.

Контейнер збільшено гаряче з 2 CPU/4 GiB до 3 CPU/5 GiB. Перезапуск LXC не виконувався. API 35
guest використовує близько 3.5–3.7 GiB RSS, тому одночасний clean Gradle build суттєво впливає на
його швидкодію.

## Запуск

```bash
./scripts/android-emulator.sh start
```

Команда навмисно тримає emulator у foreground. В іншому shell:

```bash
./scripts/android-emulator.sh wait
./scripts/android-emulator.sh status
./scripts/android-emulator.sh install
./scripts/android-emulator.sh stop
```

Foreground-процес надійніший у цьому LXC за `nohup`, оскільки тут немає user systemd/desktop
session. Codex запускає його в довгоживучій PTY-сесії.

## Текстовий screen inspector

`scripts/android-screen.sh` поєднує два джерела:

1. UIAutomator XML для активного Activity — він дає точні bounds, але не бачить окреме IME window.
2. Debug-only receiver Бубнілки — він обходить реальне дерево `keyboardView` і повертає bounds,
   center, size, visibility, enabled/clickable/selected, class, resource ID, text та description.

```bash
./scripts/android-screen.sh window --actionable
./scripts/android-screen.sh ime --actionable
./scripts/android-screen.sh tree
./scripts/android-screen.sh tap 979 2231
./scripts/android-screen.sh text abc
./scripts/android-screen.sh screenshot /tmp/bubnilka.png
```

Під час перевірки direct IME dump займав 65 ms, повне дерево з UIAutomator — 2.4–3.5 s, PNG capture
— 0.28–0.29 s без вартості подальшого image-аналізу. Тому для клавіатури основний feedback loop —
`ime`, а PNG лишається fallback для кольорів, шрифтів, clipping та системних overlays.

## Межі доказів

Емулятор придатний для layout, onboarding, state rendering, вибору стилю, Backspace, insertion,
accessibility-міток і базових performance smoke tests. SwiftShader на Intel N100 не репрезентує GPU
реального телефона: швидке перемикання чипів показало 50-й percentile 48 ms і 93.75% janky frames,
тому ці frame numbers є характеристикою headless host, а не Android UI Бубнілки.

Фізичний телефон досі потрібен для правдивої перевірки мікрофона, системного STT, App Check,
Gemini latency та поведінки `InputConnection` у конкретних сторонніх застосунках.

## Джерела

- [Android Emulator hardware acceleration](https://developer.android.com/studio/run/emulator-acceleration)
- [Command-line and headless emulator](https://developer.android.com/studio/run/emulator-commandline)
- [ADB and screencap](https://developer.android.com/tools/adb)
