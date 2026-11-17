# Delivery Ledger

Status terminology: **source present** means code/tests have been authored; **verified** requires the applicable checks to have run. A phase is not accepted merely because its files exist.

| Phase | Deliverable | Current status |
| --- | --- | --- |
| 1 | Architecture, Android limitations, OCI evaluation, API/schema plan | Documented; tenancy/security decisions still require operator confirmation |
| 2 | Fresh project, centralized versions, wrapper, CI and documentation | Codemagic build 3 passed at d7bbf07 and produced a previewable debug APK; wrapper is bootstrapped on CI, vendoring still pending |
| 3 | Auth, devices, pairing, revocation, manual HTTPS metadata round trip | Live PostgreSQL/API/trusted TLS, Appetize preview and physical Vivo V27 synchronization verified by owner. Session retention and both listings passed; Telethon working. Manual revocation deferred by owner to retain preview; pairing/destructive acceptance still open |
| 4 | Central permission states/rationales/denial/settings dashboard | Android 0.2.0 source and five policy tests present; exact permission/security structure check passed. Fresh Codemagic compile/lint/tests and permission device matrix pending |
| 5 | Opt-in location, encrypted history, start/pause/stop, configurable synchronization | Pending; inexact background scheduling and OS grants required |
| 6 | CameraX photos/video and explicit microphone recording | Pending; visible preview/local consent, approved upload only |
| 7 | Audio controls and supported call-state learning | Pending; no call-recording bypass or private communication capture |
| 8 | SAF/Photo Picker, resumable media transfers, contacts and safe SMS import | Pending; direct SMS access excluded unless independently justified/permitted |
| 9 | Private OCI Object Storage, transfers, cleanup, quotas and retention | Pending; backend identity stack deployment still needs validation |
| 10 | Expiring remote requests, audit, local sensor approval and optional push | Pending; no hidden operations, arbitrary commands or immediate-delivery promise |
| 11 | Dashboard API expansion and basic web admin | Pending; existing device APIs provide a starting contract, not a web interface |
| 12 | Full security review, tests and operational documentation | Per-slice tests/docs started; production review, execution and device acceptance pending |

## Next Acceptance Gate

1. Run `android-foundation` for Android 0.2.0, download the debug APK and complete [PERMISSIONS.md](PERMISSIONS.md). No backend rebuild/redeploy or new VM software is required.
2. Verify no startup permission prompts, explicit explanations, allow/deny, repeated denial/settings, approximate-only location and refreshed grants after Settings/rotation/relaunch.
3. Keep the existing account, backend and preview session active. Do not revoke valuable installations merely to finish a test; use a disposable installation for the deferred revocation gate later.
4. Record cloud results and physical-device permission results before implementing location capture or background work. Granting a permission is not proof of working sensor capture.

The existing VM was retained at the owner's request. They explicitly accepted the Ubuntu 20.04/no-ESM learning risk after account creation was unavailable; standard support has still ended. The 400 MiB profile passed staged readiness and light manual use, not peak-load or production security certification. The six preflight policy tests and their conservative thresholds remain unchanged. Actual server commands and ingress changes were performed by the owner, not through the restricted authoring computer.