# Verification Plan and Current Results

## What Was Actually Checked

During source authoring, `scripts/verify-structure.ps1` passed. It parses XML, checks the exact seven-permission allowlist and optional hardware, backup/cleartext settings, Gradle catalog references, PowerShell syntax, expected project files and eight test-suite files. Editor diagnostics returned no reported errors, which does not establish Kotlin compiler support or correctness.

The local Gradle wrapper download failed because of Zscaler policy. No package/dependency was installed locally. The owner supplied a successful Codemagic build 3 screenshot at `d7bbf07`, then a successful `backend-image` build at `6fac19c`, generated an Android APK and ran it in preview and on Vivo V27. The default CI still skips the disposable PostgreSQL integration suite; the live learning deployment is not a replacement for that isolated test.

On 2026-11-17, owner evidence confirmed the Linux/amd64 non-root image load, PostgreSQL health, private API readiness, public trusted HTTPS `/ready`, preview enrollment and real Vivo V27 synchronization. The owner confirmed session retention, both device listings and Telethon working. Manual revocation was deliberately not executed to preserve the preview session. Pairing, account deletion, backup recovery, minimum-API/OEM coverage and sustained load remain open; no broad device certification is claimed.

On 2026-11-20, GitHub [Foundation checks run 3](https://github.com/Venugopal116/AndroidPersonalAppLearning/actions/runs/44694393122) at `da9e499` passed `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug` and published `debug-apk`. This validates Kotlin compilation, Android JVM unit tests (including the five permission-policy methods covering all eight grant/history/rationale combinations), lint and APK generation for Android 0.2.0. The separate backend job passed shared/backend tests against its disposable PostgreSQL service and produced `backend-distribution`. Runtime permission acceptance and the device matrix in [PERMISSIONS.md](PERMISSIONS.md) remain open; no sensor or device behavior is claimed from these cloud checks.

Six standard-library preflight tests previously passed locally, covering security support, RAM, ports, tooling/disk and support expiry. Run them with `python -B -m unittest discover -s tests -p test_preflight.py -v`. They use synthetic host facts; the owner separately ran the real preflight and chose to proceed with the documented unsupported-OS learning risk. No thresholds were lowered.

## GitHub Actions: Current Build Path

The owner switched to GitHub Actions after a Codemagic trial/build restriction and created a free Appetize account for limited previews. Run the manual `Foundation checks` workflow on `main`: it uses standard Linux runners, builds the APK, runs Android unit tests/lint and separately tests the backend against a disposable PostgreSQL service. Standard public-repository runners are free under GitHub's policy; private repositories draw from included usage. Artifact/cache limits are separate. Do not enable billing, use larger runners or move builds onto the shared 1 GB VM.

The first GitHub run `44692208328` at `60b59d0` failed before Kotlin compilation: the backend failed in `pwsh -File scripts/bootstrap-wrapper.ps1`; Android failed in `android-actions/setup-android@v3`. The displayed Node/setup-java deprecation notices are warnings, not detailed failure causes. Public job metadata identified these steps; full log download required additional access, so no exact underlying exception is claimed.

The corrected Linux workflow uses the existing checksum-verifying `bootstrap-wrapper.sh` and SDK-local `configure-android-sdk.sh` helper, retaining JDK17, SDK35, wrapper validation and tests. Shell syntax and the fake-tool SDK regression suite passed locally without dependency downloads. Run `44693404746` confirmed both setup paths and the backend passed, but Android compilation exposed six missing icons because the catalog supplied only `material-icons-core`. The one-line catalog fix uses BOM-managed `material-icons-extended`; run `44694393122` at `da9e499` passed both jobs. Artifacts have three-day retention; download `debug-apk`, extract `app-debug.apk` and upload only that APK to Appetize. Appetize is a preview service, not a Kotlin build service. Its published free plan was 30 minutes/month with three-minute sessions when checked on 2026-11-20; confirm account limits before use.

Existing backend/Telethon services and valuable preview sessions need no restart or redeployment. A different CI debug signing key may prevent an in-place Vivo update; do not uninstall a valued installation merely to bypass a signing mismatch. Use a disposable preview first and sign in to the same app account only when account testing is needed. Do not provide signing secrets, invitations or Appetize tokens in chat.

## Codemagic: Optional Build Path

The first Codemagic build of commit `25eb79b` stopped at SDK setup because `sdkmanager` was not on PATH. The owner's browser-edited SDK discovery and the explicit Ktor logging receiver fix were validated by the successful build at `d7bbf07`. That working SDK block is preserved. Additional SDK-helper fixture tests run without downloading packages; `bash scripts/test-configure-android-sdk.sh` passed locally. Android 0.2.0 was subsequently validated in the GitHub run above; it does not change image packaging or the deployed backend.

Connect the repository through GitHub in the browser, select `main` and manually start `android-foundation` from the root `codemagic.yaml`. Review entitlement/build minutes; do not enable billing. There are no signing, Oracle or SSH secrets required for this debug workflow. It runs shared/backend JVM tests and Android unit tests/lint, then builds an APK and backend ZIP. Reports are build artifacts. `backend-image` is the separate server-image workflow, already exercised for the foundation; it is not needed for this Android-only milestone. Never deploy a replacement backend just to test permission UI.

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
| Android `PermissionPolicyTest` | Current grant wins, first request, rationale/retry, missing local history and conservative Settings fallback; JVM tests passed in GitHub run 3 at `da9e499` |
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