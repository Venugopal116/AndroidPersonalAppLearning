# Local Permission Center

Android 0.2.0 adds a Permissions screen, opened with the shield action in the existing toolbar. It is local to this app installation and works independently of the signed-in account. No server API/schema or VM change is needed. There are no startup permission prompts.

## Scope

Rows cover camera, microphone, approximate foreground location, precise foreground location and notifications. Each request first displays an explanation and requires Continue before the Android system dialog. Not now leaves the grant unchanged. Approximate-only access is a valid outcome; precise access is requested together with coarse access as Android requires. Hardware requirements are optional.

No camera, recorder, location provider, background worker, foreground service, notification sender or permission-status upload is implemented here. Granting permission starts none of those operations. Background location, contacts, SMS, Bluetooth and broad storage access are not requested. Future features must check current grants again at use time and obtain any additional local consent.

## State and Denial

Android's current grant is the source of truth. A private preference records only completed request results, not credentials or permission data for the server. Current grants are reread after results and whenever the screen resumes, including a return from Settings. System dialogs and Android Settings, not the app, decide access.

| Android/local facts | Display/action |
| --- | --- |
| Granted, regardless of history | Allowed; Settings can manage/revoke it |
| Not granted, platform requests rationale | Denied; explanation and another explicit request |
| Not granted, no known completed request | Not requested; explanation and first request |
| Not granted, prior result and no platform rationale | Review Settings rather than repeatedly prompting |

The last row is a conservative fallback, not proof of permanent denial: device policy, one-time grants, automatic resets and OEM behavior can differ. The app does not label such a state irreversibly denied or attempt a bypass. No grant is required to continue account/device-metadata workflows.

## Build and Acceptance

1. Run `android-foundation` on the published commit. Require `:app:testDebugUnitTest`, lint and APK compilation to pass. Five `PermissionPolicyTest` methods cover all eight grant/history/rationale combinations; local structure/editor checks do not execute them.
2. Install the APK on an authorized Android 13+ device or fresh preview. Clean Codemagic debug signing keys can differ; an update may be rejected for signing mismatch. Do not uninstall a valued installation merely to test. Prefer a disposable preview first. Uninstall clears the session, permission history and installation identity; signing in again does not restore the old installation UUID.
3. Confirm no permission dialog on launch/login/synchronization. Open Permissions with the shield action; use TalkBack and large fonts, rotate and return with Back without losing the account session.
4. For camera/microphone/notifications: cancel the explanation, allow, deny, deny repeatedly, open Settings and grant/revoke there, then return. States must match Android; there must be no request loop. Camera/microphone privacy indicators must stay off because there is no capture. Do not reset the retained preview's account merely for denial tests; use a disposable test installation if needed.
5. Grant only approximate location; verify approximate Allowed and precise not Allowed. Separately test a precise grant, one-time grants/expiration and revocation from Settings. No background permission may appear and no tracking may start.
6. Confirm device status still synchronizes with all optional grants denied. Verify Vivo V27 and API 33 behavior separately; preview Android 16 is not evidence for every supported API/OEM.

The backend and existing Codemagic preview sessions are not revoked by this source update. Current device evidence covers the foundation only; permission UI, process recreation and OEM denial/reset behavior remain unverified until this matrix is run. Existing no-ESM host risk, backup gaps and deferred identity destructive tests are unchanged.