# ToDoWidget

A resizable Android home-screen widget that is a complete to-do list on its own — tick tasks off,
add new ones, watch deadlines count down and change every colour without ever opening a normal app
screen. Everything is stored as plain Markdown, optionally in a note inside your Obsidian vault.

## What it does

- **Tick from the home screen.** Tap a task to check it off. It stays visible with a strikethrough
  for a few seconds (configurable), offers an **Undo**, and then slips into the archive.
- **See the archive without leaving the widget.** The bottom-left button flips the same surface to
  the completed list and back.
- **Deadlines that count themselves down.** Each task can carry a duration ("within 4h") or an
  explicit deadline. The remaining time sits behind the task text as `3d`, `4h`, `25m`, and turns
  red as `-2h` once it is overdue.
- **Add without a detour.** The add button opens a small sheet over your launcher — text, one of
  three priorities, and a duration or deadline — then closes straight back to the home screen.
- **Yours to colour.** Highlight/CTA, background (including fully transparent), text and one colour
  per priority level. Three presets are included.
- **Plain Markdown, wherever you want it.** By default the list lives in the app's own storage. Point
  it at an Obsidian vault folder or a single `.md` file and it writes there instead.

## The file format

Tasks use the [Obsidian Tasks](https://publish.obsidian.md/tasks/) emoji format, so a vault note
keeps working with the Tasks and Dataview plugins:

```markdown
- [ ] Buy milk ⏫ ➕ 2026-08-19 📅 2026-08-21
- [ ] Submit the form 🔼 📅 2026-08-20 [due_time:: 14:30] [duration:: 4h]
- [x] Call the landlord 🔽 ✅ 2026-08-18
```

| Field | Meaning |
|---|---|
| `⏫` `🔼` `🔽` | High, medium, low priority (`🔺`/`⏬` are read as high/low) |
| `➕ date` | Created |
| `📅 date` | Deadline — without a time it means end of that day |
| `[due_time:: HH:mm]` | Deadline time, written only when it is not end of day |
| `[duration:: 4h]` | The window you gave yourself, kept for reference |
| `✅ date` | Completed |

Anything else on the line — tags, recurrence rules, unknown fields — is left exactly as it was, and
lines that are not tasks (headings, prose, front matter) are never rewritten. You can keep your
todos in the middle of a real note.

The exact second a task was ticked is kept inside the app rather than in the file, so the auto-hide
timer works without filling your vault with timestamps.

## Getting the APK

Every push builds one. Open the latest **Build APK** run under
[Actions](../../actions/workflows/build.yml) and download the `todowidget-debug-apk` artifact.

Tagging a commit `v*` runs the release workflow, which attaches a release APK to a GitHub Release.
It signs the APK when these repository secrets exist, and ships it unsigned when they do not:
`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Using it

1. Install the APK and long-press your home screen to place **To-Do List**. Resize it freely.
2. Tap **+** to add your first task.
3. Tap the sliders icon for settings — colours, how long a ticked task lingers, and where the
   Markdown file lives.
4. For Obsidian: settings → **Vault folder**, pick your vault (or a folder in it), and set the file
   name. Your current list is copied across on the first switch. Choosing **Single file** instead
   points the widget at an existing note.

Access to the vault uses the system file picker, so the app never asks for broad storage
permissions and the grant survives reboots.

## Known limits

These are platform limits, not missing work:

- Widgets cannot host a text field, which is why adding opens a sheet over the launcher.
- Countdowns cannot tick every second. The widget wakes only at the moments a label actually
  changes, plus a periodic refresh that also picks up edits you made in Obsidian.
- Rounded widget corners need Android 12 or newer; on older versions the background is square.

## Building locally

```bash
./gradlew testDebugUnitTest   # markdown round-trip and countdown tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Needs JDK 17 and the Android SDK (compileSdk 35). Kotlin 2.0.21, AGP 8.7.3, Jetpack Glance.

## Layout

```
app/src/main/java/com/felicedesign/todowidget/
├── data/
│   ├── markdown/     parser and line-preserving writer
│   ├── storage/      app-private file, and SAF document/vault access
│   ├── TodoRepository.kt    the only place tasks are changed
│   └── SettingsRepository.kt
├── model/            Todo, Priority, Settings, TodoBoard
├── ui/
│   ├── add/          the sheet that floats over the launcher
│   ├── settings/     colours, behaviour, storage location
│   └── theme/
├── util/Countdown.kt formats time left, and says when it next changes
└── widget/           the Glance widget, its actions and the tick scheduler
```
