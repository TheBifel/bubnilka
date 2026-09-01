# Contract: CloudTransformProvider

## Purpose

Transform one final dictated transcript into the selected style without adding facts or assistant
commentary.

## Input

- Current session identifier
- Trimmed final transcript, 1..2,000 characters
- Known style identifier
- Source language tag

The caller MUST NOT include surrounding field content or persisted history.

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
