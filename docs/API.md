# Foundation API

Base URL: your HTTPS origin. All timestamps are UTC Unix milliseconds. JSON field names match [the shared contracts](../shared/src/main/kotlin/dev/personal/contracts/Contracts.kt). No endpoint returns password hashes or token digests. Successful session responses contain bearer secrets and must not be logged/cached.

## Endpoints

| Method | Path | Authorization | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/health` | None | None | 200 `Health` (process liveness only) |
| POST | `/v1/auth/register` | Registration invitation | `Credentials` | 201 `Tokens` |
| POST | `/v1/auth/login` | Email/password | `Credentials` | 200 `Tokens` |
| POST | `/v1/auth/refresh` | Refresh token in body | `RefreshRequest` | 200 rotated `Tokens` |
| POST | `/v1/auth/logout` | Bearer access token | None | 204 |
| GET | `/v1/devices` | Bearer access token | None | 200 array of `Device` owned by caller |
| POST | `/v1/devices` | Bearer access token | `DeviceInput` | 201 `Device`, binds caller's session |
| POST | `/v1/devices/{id}/heartbeat` | Matching bound device session | `DeviceInput` | 200 `Device` |
| POST | `/v1/devices/{id}/revoke` | Owner session | None | 204; all target bound sessions deleted |
| POST | `/v1/pairings` | Owner session | `PairingInput` | 201 `PairingCode` |
| POST | `/v1/pairings/redeem` | Same owner, enrolling session | `RedeemPairing` | 201 `Device`, binds caller's session |
| DELETE | `/v1/me` | Owner session | None | 204; active account data deleted |

Use `Authorization: Bearer <access-token>` on protected endpoints. Send bodies with `Content-Type: application/json`. Body limit: 16 KiB. Responses set `Cache-Control: no-store`. Cross-origin browser access is not enabled; a future same-origin dashboard avoids token exposure through permissive CORS.

## Contracts

`Credentials`: `email`, `password`, optional `registrationCode`. Registration requires the configured invitation, normalized unique email and a 12-128 character password. Login ignores `registrationCode`.

`Tokens`: `accessToken`, `refreshToken`, `expiresAt`. Access lifetime ten minutes; refresh seven days. Refresh atomically invalidates the previous access and refresh tokens. Clients serialize refresh calls and retry a protected request at most once. A lost refresh response can require signing in again; do not blindly retry refresh.

`DeviceInput`: canonical lowercase UUID `installationId`; 1-100 character nonblank `name`, `manufacturer`, `model`, `androidVersion`; nullable integer `batteryPercent` (0-100); `connectivity` in `wifi`, `cellular`, `ethernet`, `other`, `offline`.

`Device`: all input fields plus server UUID `id`, server observation `lastSeenAt` and nullable `revokedAt`. Metadata is client-reported, not attested. Re-enrollment of an active installation by the same owner is idempotent; another owner cannot claim it. Revoked installation IDs are not resettable through this API.

`PairingInput`: target `installationId`. `PairingCode`: random `code` and `expiresAt` (five minutes). `RedeemPairing`: `code` and `device` (`DeviceInput`). The recipient must already be signed in to the same owner account and approve enrollment locally. At most five outstanding pairings per account. A replacement code invalidates any previous code for that account/installation.

`ApiError`: `code`, `message`. Typical statuses: 400 invalid JSON/metadata/pairing; 401 missing/expired/revoked session; 403 wrong registration invitation or mismatched device scope; 404 device not owned/not found; 409 unique-account/installation conflict; 413 oversized body; 415 unsupported content type; 429 rate/session/pairing limit; 500 generic internal failure. Unmatched routes return framework 404 responses; clients must tolerate non-JSON proxy/framework errors.

## Operational Constraints

All callers share a conservative 120 requests/minute origin cap. Login, registration, refresh and pairing redemption additionally share 10 requests/minute. Behind the supplied proxy the backend sees the proxy origin; this intentionally becomes a small deployment-wide cap rather than trusting spoofable forwarded headers. It is not suitable for a public multi-tenant service. Limits reset on process restart; no distributed limiter is supplied.

At most 20 unexpired sessions per account. Expired sessions and expired/consumed pairings are pruned on relevant writes. Audit events record successful identity/device actions without request contents and are pruned to approximately 30 days when new events are written. No audit-query endpoint is provided yet.

Device lists are not paginated in this two-device milestone. Pagination, reauthentication for destructive operations, account session management, location/media APIs, consent-based remote requests and browser authentication are future gates, not silently implemented capabilities. Account deletion is destructive; require a confirmation UI as the Android app does. HTTP APIs must never be embedded as publicly usable destructive links.