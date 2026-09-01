# Contract: TextInsertionPort

## Purpose

Isolate host-field interaction from shared keyboard workflow logic.

## Operations

- `insert(text)`: commit a nonblank final or explicitly approved raw string at the cursor.
- `matchesBeforeCursor(text)`: return true only when the complete text is immediately before the
  cursor in the current connection.
- `replace(previous, replacement)`: replace only when `matchesBeforeCursor(previous)` is true.
- `undo(previous)`: delete only when `matchesBeforeCursor(previous)` is true.

## Safety

No delete or replacement operation may proceed after a connection/field change or when the exact
suffix cannot be obtained. Failure is reported without modifying host text.
