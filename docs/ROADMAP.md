# Delivery Ledger

Status terminology: **source present** means code/tests have been authored; **verified** requires the applicable checks to have run. A phase is not accepted merely because its files exist.

| Phase | Deliverable | Current status |
| --- | --- | --- |
| 1 | Architecture, Android limitations, OCI evaluation, API/schema plan | Documented; tenancy/security decisions still require operator confirmation |
| 2 | Fresh project, centralized versions, wrapper, CI and documentation | Source present; Codemagic browser-first workflow added; verified wrapper bootstraps on CI, cloud execution pending |
| 3 | Auth, devices, pairing, revocation, manual HTTPS metadata round trip | Source and tests present; execution, deployment and real round trip pending |
| 4 | Central permission states/rationales/denial/settings dashboard | Pending; only normal network permissions declared now |
| 5 | Opt-in location, encrypted history, start/pause/stop, configurable synchronization | Pending; inexact background scheduling and OS grants required |
| 6 | CameraX photos/video and explicit microphone recording | Pending; visible preview/local consent, approved upload only |
| 7 | Audio controls and supported call-state learning | Pending; no call-recording bypass or private communication capture |
| 8 | SAF/Photo Picker, resumable media transfers, contacts and safe SMS import | Pending; direct SMS access excluded unless independently justified/permitted |
| 9 | Private OCI Object Storage, transfers, cleanup, quotas and retention | Pending; backend identity stack deployment still needs validation |
| 10 | Expiring remote requests, audit, local sensor approval and optional push | Pending; no hidden operations, arbitrary commands or immediate-delivery promise |
| 11 | Dashboard API expansion and basic web admin | Pending; existing device APIs provide a starting contract, not a web interface |
| 12 | Full security review, tests and operational documentation | Per-slice tests/docs started; production review, execution and device acceptance pending |

## Next Acceptance Gate

1. Publish to the supplied GitHub repository and authorize only that repository in Codemagic.
2. Run the manual `android-foundation` workflow, which bootstraps the verified wrapper externally; fix any compiler/test/lint findings before deployment and vendor the verified JAR when available.
3. Retain the existing VM/Telethon workload; address absent ESM coverage, DNS, approved ingress, backups and measured resource headroom before any deployment or server change.
4. Deploy the backend and complete the two-phone HTTPS flow.
5. Record test evidence and commit the accepted foundation before adding sensor permissions.

Do not expand into later capabilities until this identity foundation is verified. Do not claim live functionality based on mock tests or static checks.