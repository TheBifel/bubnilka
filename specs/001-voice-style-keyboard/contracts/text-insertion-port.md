# Contract: TextInsertionPort

## Purpose

Isolate host-field interaction from shared keyboard workflow logic.

## Operations

- `insert(text)`: commit a nonblank final or explicitly approved raw string at the cursor.
- `matchesBeforeCursor(text)`: return true only when the complete text is immediately before the
  cursor in the current connection.
- `replace(previous, replacement)`: replace only when `matchesBeforeCursor(previous)` is true.
- `undo(previous)`: delete only when `matchesBeforeCursor(previous)` is true.
- `readAllText()`: return a bounded snapshot only when the editor exposes its complete text.
- `replaceAll(replacement)`: select the entire current editor and replace it with the unified cloud
  result. Whole-field mode deliberately does not compare against the earlier snapshot.

## Safety

Suffix replacement used when field combination is disabled still requires an exact suffix match.
Whole-field mode is intentionally unconditional: edits made while the cloud request is pending can
be overwritten. A failed editor operation returns `false`; Undo in whole-field mode restores the
captured pre-dictation snapshot by replacing the whole current editor.
