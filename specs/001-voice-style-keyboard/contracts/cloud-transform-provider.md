# Contract: CloudTransformProvider

## Purpose

Transform or translate one final dictated transcript into the selected style without adding facts
or assistant commentary. Optionally merge it with explicitly supplied whole-field context.

## Input

- Current session identifier
- Trimmed final transcript, 1..2,000 characters
- Known style identifier
- Source and target language tags
- Optional complete field snapshot (maximum 6,000 characters) and additional transcript as
  separate untrusted data blocks

The caller MUST NOT include field content unless the user selected Field + voice. It never includes
persisted history.

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
the underlying request completes later.
