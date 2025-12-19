# Layout `KeyAction` defaults

This project uses the `a.kt`-style key schema (`actions` + `cases` + `doActions`).

## `default`

`KeyAction.default` is the default branch actions:

- When `cases` is empty, it is executed.
- When `cases` is non-empty but no case matches, it is executed.

## `fallback`

`KeyAction.fallback` is an optional built-in fallback used when `default` is empty.

Supported values:

- `PRIMARY_CODE_AS_TOKEN`: emits `key.primaryCode` as a literal token (code point -> string) via `PUSH_TOKEN`.

Supported values:

- `PRIMARY_CODE_AS_TOKEN`: emits `key.primaryCode` as a literal token (code point -> string) via `PUSH_TOKEN`.

### Example

```json
{
  "keyId": "key_q",
  "primaryCode": 113,
  "label": "q",
  "ui": { "styleId": "style_alpha_key", "gridPosition": { "startCol": 0, "startRow": 0, "spanCols": 1 } },
  "actions": {
    "TAP": {
      "actionType": "PUSH_TOKEN",
      "cases": [
        { "whenCondition": { "layer": "ALPHA" }, "doActions": [ { "type": "PUSH_TOKEN", "token": "q" } ] }
      ],
      "fallback": "PRIMARY_CODE_AS_TOKEN"
    }
  }
}
```
