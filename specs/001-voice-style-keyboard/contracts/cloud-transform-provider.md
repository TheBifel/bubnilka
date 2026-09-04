# Contract: CloudTransformProvider

## Purpose

Transform one final dictated transcript into the selected style and persistent output language
without adding facts or assistant commentary. By default, merge it with supplied whole-field context.

## Input

- Current session identifier
- Trimmed final transcript, 1..2,000 characters
- Known style identifier
- Auto-detect source marker and selected Ukrainian or English target language tag
- Complete field snapshot by default (maximum 6,000 characters) and additional transcript as
  separate untrusted data blocks; the user can disable field combination

The caller includes field content while the default «Combine with field» toggle is enabled. When
disabled, only the transcript is included. It never includes persisted history.

## Warm-up

- The provider exposes a content-free warm-up operation that may issue one minimal generation.
- Listening starts the warm-up immediately and in parallel with recognition.
- The first user transformation waits until the warm-up attempt finishes, so it is the next model
  call rather than a concurrent request.
- Warm-up failure is diagnostic only and must not suppress or retry the real transformation.
- Firebase limits warm-up to one attempt per application process; it consumes one additional
  provider request but never contains field or dictated content.

## Output

- Matching session identifier
- Matching style identifier
- Nonblank transformed text only

## Failure categories

- Not configured
- Offline or timeout
- Free-tier quota exhausted
- Provider rejected request
- Empty or invalid response
- Cancelled/stale request

The provider performs no automatic retries. Cancellation must stop delivery to the session even if
the underlying request completes later. Warm-up response text is ignored.
