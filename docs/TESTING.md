# Verification Plan and Current Results

## What Was Actually Checked

During source authoring, `scripts/verify-structure.ps1` passed. It parses XML, checks normal-only manifest permissions, backup/cleartext settings, Gradle catalog references, PowerShell syntax, expected project files and test-suite presence. Editor diagnostics returned no reported errors, which does not establish Kotlin compiler support or correctness.

The local Gradle wrapper download failed because of Zscaler policy. No package/dependency was installed locally. The owner supplied a successful Codemagic build 3 screenshot at `d7bbf07`: shared/backend JVM tests, Android unit tests/lint and debug APK compilation finished, and the app launched in preview. Real PostgreSQL integration is skipped in the default workflow; instrumentation, container/image validation, live application-to-OCI connectivity, memory benchmarks and physical-device tests remain pending. A successful SSH connection is not an application connectivity test.

The subsequent milestone adds readiness/connector tests and Jib packaging and is not covered by that older green build. Six standard-library preflight tests passed locally, covering recognized security support, the existing VM's ESM/RAM failures, occupied/unknown ports, missing tooling/disk and support expiry. Run them with `python -B -m unittest discover -s tests -p test_preflight.py -v`. No real VM was queried or changed by those fixture tests.

## Codemagic: Primary Execution Path

The first Codemagic build of commit `25eb79b` stopped at SDK setup because `sdkmanager` was not on PATH. The owner's browser-edited SDK discovery and the explicit Ktor logging receiver fix were validated by the successful build at `d7bbf07`. That working SDK block is preserved. Additional SDK-helper fixture tests run without downloading packages; `bash scripts/test-configure-android-sdk.sh` passed locally. The current milestone still requires a new build because it changes Kotlin and image packaging after that success.

Connect the repository through GitHub in the browser, select `main` and manually start `android-foundation` from the root `codemagic.yaml`. Review machine entitlement/build minutes first. There are no signing, Oracle or SSH secrets required for this debug workflow. It runs shared/backend JVM tests and Android unit tests/lint, then builds an APK and backend ZIP. JUnit reports and test HTML reports are exposed as build results/artifacts. The separate manual `backend-image` workflow runs deployment guard tests and JVM tests, then builds a Linux/amd64 image with Jib and packages the deployment archive. It needs no Docker daemon on the Mac and has not run yet. Verify image load/start, `/ready`, persistence and actual resource use in an isolated staging environment before coexistence on the Telethon VM.

The default Codemagic workflow does not provision a disposable PostgreSQL instance; `PostgresIntegrationTest` is therefore skipped unless its test environment variables are supplied securely against a disposable database. Do not point it at the Telethon VM or any live database. The retained manual GitHub backend workflow runs PostgreSQL integration on its own disposable service. Device instrumentation, resource measurement and real HTTPS acceptance remain separate gates even after a green Codemagic build.

## Automated Test Sources

| Suite | Coverage |
| --- | --- |
| Shared `ContractsTest` | UUID, field, battery and connectivity validation |
| Backend `IdentityServiceTest` | Login, invitation, expiry, refresh replay, logout, owner isolation, scoped heartbeat, revocation, pairing binding/expiry/replay, deletion, token/password digests and rate limits |
| Backend `ApiTest` | HTTP registration/enrollment/heartbeat/deletion, unauthorized access, malformed/oversized bodies, throttling and the new database/service readiness contract |
| Backend `PostgresIntegrationTest` | Actual PostgreSQL persistence, session revocation and cleanup |
| Android `RemoteDeviceRepositoryTest` | MockWebServer refresh/retry, authorization headers, revoked refresh, logout cleanup, HTTPS-origin validation and new readiness/compatibility/error checks |
| Android `FoundationViewModelTest` | Busy guard, sign-in/sign-out state and recoverable network errors |
| Android `KeystoreSessionStoreTest` | On-device persistence, encrypted record, tamper rejection and clearing |

Run Android/backend JVM tests in the authorized build environment:

```powershell
.\gradlew.bat :shared:test :backend:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

For real PostgreSQL integration set `POSTGRES_TEST_URL`, `POSTGRES_TEST_USER` and `POSTGRES_TEST_PASSWORD` privately in the authorized test environment. Use a disposable test database, never the live database. The test creates a random test account and deletes it in a finally block; schema initialization still runs. Without the URL the integration test is intentionally skipped. CI supplies a disposable PostgreSQL 16 service and runs the test.

The Ktor HTTP suite uses H2 in PostgreSQL mode. That is not equivalent to PostgreSQL; keep the real-engine CI gate enabled.

On a connected Android 13+ test device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Use a test installation/account. The Keystore tests clear session preferences and should not run against valuable user sessions. Instrumentation has no secrets built in and is not run by the hosted CI workflow yet.

## Physical-Device Acceptance

Test Moto Edge 60 Pro and Vivo V27 on their installed Android/security-patch versions. Record model, OS/API, OEM build, app commit and server image. The nominal model does not establish its current OS version. Test an API 33 emulator as the minimum, API 34/35 and newer stable releases available in the test environment.

- Configure trusted HTTPS; reject HTTP, wrong-host, untrusted and expired certificates.
- Exercise invitation registration, bad credentials, duplicate accounts, logout, process death, screen rotation and relaunch.
- Register both installations; confirm metadata, nullable battery handling, connectivity changes and last synchronization times.
- Wait for access expiry, refresh successfully, then revoke and verify refresh/synchronization fail.
- Verify pairing mismatch, wrong account, duplicate redemption and expiry with local approval on the recipient.
- Disable network during requests and confirm no tokens/passwords appear in errors or logs.
- Delete the account, verify all active rows disappear, then test backup retention and deletion reapplication on restore.
- Confirm font scaling, small screens, dark/light themes, keyboard visibility and accessibility labels. Screen capture is intentionally disabled.
- Verify server restarts preserve data and actual memory/CPU/disk usage stays within the micro instance's measured limits.

## Version-Specific Future Gates

API 33: notifications require runtime authorization, granular media access applies. API 34: foreground-service type permissions and while-in-use checks are enforced; selected-photo access needs careful handling. API 35+: re-evaluate foreground-service/boot execution limits, user-initiated transfer options, edge-to-edge layout and target-SDK behavior for the actual release. Newer Android versions are not certified by targeting API 35.

Location phase must test approximate-only access, background grant, denial/revocation, inexact 15-minute work, Doze, force-stop and OEM battery policies. Camera/audio phase must test visible local approval, privacy indicators, permission revocation and pause/resume/stop. Media phase must test lost SAF grants, interrupted/multipart uploads, quota failure and deletion. None of those tests or implementations exists yet; do not report those features as tested.