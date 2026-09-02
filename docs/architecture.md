# Architecture

## Runtime flow

1. Android delivers `SMS_RECEIVED` to `SmsReceiver` after the user grants permission.
2. The receiver extracts multipart SMS content and starts `SmsForwardingService`.
3. The service must immediately publish a persistent notification with `startForeground`.
4. `TelegramClient` sends a short-lived HTTPS request and returns a typed result.
5. The service records only non-sensitive operational status and stops itself.

## Recommended boundaries

- **Flutter UI:** permission education, configuration health, enable/disable state.
- **Native receiver:** validate action and normalize SMS parts; do no network work.
- **Foreground service:** lifecycle, notification, cancellation, retry policy.
- **Transport:** HTTPS client, timeouts, response mapping, secret provider abstraction.
- **Backend (recommended):** stores the Telegram token outside the APK and forwards authenticated requests.

## Security checklist

- Never commit `.env`, `config/local.json`, `local.properties`, keystores, tokens, chat IDs, or captured SMS.
- Do not print secrets or message bodies to Logcat.
- Restrict any backend credential, rotate it after suspected exposure, and use HTTPS only.
- Explain SMS access before the runtime permission prompt and provide an off switch.
- Add bounded retries with backoff; avoid duplicate Telegram delivery with a local message fingerprint.

