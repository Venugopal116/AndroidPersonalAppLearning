# androidPersonalAppLearning

Personal Device Lab is an Android 13+ learning application for explicitly authorized personal-device management. This is a fresh, independent project; no code or configuration from the parent Android project is reused.
notes:   https://129-225-75-12.nip.io/ready

https://129-225-75-12.nip.io/ready
## Status: foundation source, not a verified release

Source and corresponding tests exist for account registration/login, expiring sessions, refresh rotation, device registration, device-scoped metadata synchronization, installation-bound one-time pairing, device revocation and account deletion. The Android app includes endpoint setup, authentication and device management screens with Material 3 light/dark themes. Native session storage uses Android Keystore AES-GCM.

**Foundation commit `d7bbf07` completed Codemagic build 3:** shared/backend JVM tests, Android unit tests/lint and a debug APK build passed, and the owner launched the app in preview. That is not live Oracle acceptance, physical-device certification or real PostgreSQL integration (skipped in the default workflow). No dependencies were installed on the restricted authoring computer.

The next source milestone adds `/ready` database checks, Android server compatibility verification, a read-only VM preflight, an optional micro profile and a daemonless backend-image workflow. Its new Kotlin code and image packaging require a fresh Codemagic build. Six synthetic preflight policy tests passed locally. No backend has been deployed; there is still no live address to enter. Do not treat this as production-ready.

The official Gradle wrapper JAR download was blocked by the local Zscaler policy. Launchers, wrapper properties, a pinned distribution checksum and official checksum-verifying bootstrap scripts are included. Codemagic runs the shell bootstrap on its build machine automatically; no local installation is required. The repository is not fully self-contained until the verified wrapper JAR is added. Do not bypass workplace network policy.

Camera, microphone, location, permission-management UI, WorkManager, Room history, contacts, SMS imports, media transfers, remote requests, Oracle Object Storage and web administration are **not implemented**. They have no placeholder screens pretending to work. See [docs/ROADMAP.md](docs/ROADMAP.md).

## Architecture

```mermaid
flowchart TD
    UI[Compose + ViewModel + StateFlow] --> Repository[Retrofit repository]
    Repository --> Vault[Keystore-encrypted session]
    Repository -->|HTTPS| Proxy[Caddy on OCI]
    Proxy --> API[Kotlin Ktor API]
    API --> Database[PostgreSQL]
    API -. future .-> Objects[Private OCI Object Storage]
    Web[Future web dashboard] -. authenticated API .-> Proxy
```

- `app`: Kotlin/Compose UI, ViewModel, repository, device metadata and security.
- `shared`: serialized API contracts and validation shared by Android/backend.
- `backend`: Ktor, bounded JSON HTTP APIs, JDBC transactions and SQL schema.
- `deploy`: container deployment configuration for an existing OCI VM.
- `docs`: API, architecture, deployment and testing guides.
- `codemagic.yaml`: primary browser-operated Android/backend workflow and downloadable build artifacts.
- `.github/workflows`: optional manual Android and backend checks, including real PostgreSQL integration.

Pinned compatible baseline: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, JDK 17, compile/target SDK 35, minimum SDK 33. These are deliberately pinned, not a claim to be the newest versions. Dependencies live in [gradle/libs.versions.toml](gradle/libs.versions.toml). Review current target-SDK distribution requirements and dependency security updates before publishing.

## Codemagic Browser Builds

Android Studio is optional. The primary workflow now builds on Codemagic rather than on the restricted computer or the Oracle VM.

1. In Codemagic's Connect repository dialog, select GitHub, then **Next: Authorize integration**. Complete GitHub's authentication directly in your browser. Grant access only to `Venugopal116/AndroidPersonalAppLearning` where the integration permits repository selection.
2. Select that repository and the native Android/project-with-YAML option if asked. Finish adding the application. No Flutter conversion is needed.
3. Select branch `main`, check for the root `codemagic.yaml`, then choose **Start new build** and workflow `android-foundation`.
4. Review the build machine allowance and remaining minutes before starting. The workflow uses `mac_mini_m2`; Codemagic's current docs list Linux runners as billing-enabled. This does not guarantee free entitlement for your account. Do not enable billing or purchase anything merely to run a build.
5. Codemagic verifies/downloads the official Gradle wrapper, configures SDK 35, runs shared/backend JVM tests, runs Android unit tests/lint and builds a debug APK. It also creates a backend distribution ZIP and exposes the wrapper JAR as an artifact. No Oracle keys, SSH keys, registration invitations or release signing keys are needed.
6. When the workflow succeeds, download `app-debug.apk` from the build's Artifacts section to your own phone through an authorized download path. Android may ask you to allow installation from the browser/file manager you use; enable it only for that trusted source and disable it afterwards. Do not disable device protection or install an APK from an unverified source.

The APK uses a generated debug signing key, not a production release key. Different clean CI builds may have different debug signing keys; Android may refuse an in-place update. Uninstalling the old test app deletes local credentials and its installation identity, so use disposable test data. Stable release signing will be a separate secured workflow.

Builds are manual to control usage. There is no automatic cloud deployment, no paid-service provisioning and no store publication. GitHub Actions remains manual-only to avoid duplicate automatic builds. Codemagic's default workflow does not provision PostgreSQL, so its real-engine integration test is skipped unless a disposable test database is configured separately. The optional GitHub backend workflow supplies that database. A successful cloud build is not proof of two-device operation or Oracle connectivity. See [docs/TESTING.md](docs/TESTING.md).

### Backend Deployment Artifact

Start the separate manual `backend-image` workflow when ready to validate backend packaging. It uses Jib to build a Linux/amd64 image for the existing x86_64 VM without Docker on the Mac builder. It requires network access to Gradle/base-image repositories but no Docker installation, registry login, Oracle credentials or signing keys. Review build minutes before starting.

The resulting `backend-deployment.tar.gz` contains `backend/build/jib-image.tar`, reviewed Compose configurations, Caddy configuration, an example environment file and the read-only preflight script. It does not contain real credentials. Do not build the image on the 1 GB VM. Follow [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) before loading or starting anything.

## Optional Android Studio Setup

1. Publish **this directory**, not the parent workspace, to the supplied GitHub destination `https://github.com/Venugopal116/AndroidPersonalAppLearning.git`. Confirm its visibility is Private and keep secrets out of Git. Configuring a local remote does not publish any files.
2. After publication, clone `https://github.com/Venugopal116/AndroidPersonalAppLearning.git` through Android Studio's Version Control dialog.
3. Use an Android Studio version supporting AGP 8.9, configure JDK 17 for Gradle, and install SDK Platform 35 through that authorized development environment.
4. On an authorized Windows machine, run `powershell -File scripts/bootstrap-wrapper.ps1`. It downloads only the official wrapper JAR and verifies both wrapper and pinned-distribution checksums. On Linux/macOS, PowerShell 7 runs the same script; alternatively generate the official 8.11.1 wrapper using an already authorized Gradle installation and verify it against Gradle's published checksum.
5. Commit the verified wrapper JAR to make subsequent clones self-contained. Do not commit SDKs, Gradle distributions, build directories, tokens, private keys or local configuration.
6. Open this project's root and allow Android Studio to synchronize Gradle. It resolves declared dependencies; nothing was installed on the restricted authoring computer.

No OCI credential is required to build the APK. The app asks for the public HTTPS API origin when launched. It never accepts a cloud private key.

### Current Deployment Readiness

The owner has successfully connected from Oracle Cloud Shell to the existing VM and wants to retain it. Read-only results reported on 2026-11-12: Ubuntu 20.04.6, no Ubuntu Pro attachment/ESM coverage, 952 MiB total RAM with about 504 MiB available, no swap, and approximately 42 GB free disk. A Telethon script is already running and must not be stopped, replaced or modified by this project.

There is no domain yet. A free DNS subdomain may suit this learning deployment, subject to provider availability and terms. Reusing the VM is conditional on addressing its security coverage and measuring enough resource headroom. The supplied container memory limits total 736 MiB and have not been proven safe alongside the existing workload. Do not run that stack unchanged on the VM, restart it, upgrade it or add swap without an approved plan. Nothing in Codemagic deploys there. Follow [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) only after these gates are resolved.

An optional `deploy/compose.micro.yml` lowers total caps to 400 MiB using `JAVA_TOOL_OPTIONS` for the Jib runtime. It is an unbenchmarked staging candidate, not proof the VM can run the system. Run `python3 deploy/preflight.py --json` inside the Ubuntu VM, not Cloud Shell, after transferring/reviewing the script through an authorized path. It reads OS/security status, available RAM/disk, listening ports and tool availability without printing secrets, changing settings, installing software or touching Telethon. Exit 2 lists blockers; exit 0 only allows further staging review. The previous 504 MiB available/no-ESM report fails the new 600 MiB minimum headroom/security checks.

## Build and Install

From this directory, in the authorized Android Studio environment:

```powershell
.\gradlew.bat :shared:test :backend:test :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Install via Android Studio Run or, when Android platform tools are available:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Linux/macOS: use `bash gradlew` in place of `.\gradlew.bat`. A release APK requires your own signing configuration and secure key management; no signing key is supplied. Do not distribute debug builds as production releases.

The JVM backend can build without Android SDK configuration:

```powershell
.\gradlew.bat -PbackendOnly :shared:test :backend:test :backend:installDist
```

## First End-to-End Test

1. Deploy the backend following [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md), with valid HTTPS and a strong server-only registration invitation.
2. Deploy the updated backend and launch the updated Android app. Enter your HTTPS origin, for example `https://devices.example.com`. No paths, embedded credentials or query parameters are accepted. Connect now calls `/ready` without account credentials and saves the address only if the expected service/API version and database are ready. A URL alone or an older backend without this endpoint is not sufficient.
3. Create an account with an email, a 12-128 character password and the invitation supplied privately by the server administrator. Email verification/password recovery are not implemented.
4. Enter a device name and explicitly approve registration. Synchronize status and inspect the server's device list.
5. Sign into the same account on the other phone. Its dashboard shows its installation UUID before enrollment.
6. On the first phone, enter the second installation UUID and create a pairing code. Transfer this short-lived secret privately. On the second phone, enter the code and approve registration. The code is valid once for five minutes and only for that account/installation. QR scanning is not implemented.
7. Revoke a device and confirm it can no longer synchronize or refresh its bound session. Existing registered installation IDs cannot re-enroll after revocation. A fresh install creates a new UUID; an authenticated account can intentionally register new installations. This is not hardware attestation or an enterprise enrollment lock.

Registration without a pairing code is allowed for authenticated owners on their own installations. Pairing is an additional authorized onboarding flow, not a mandatory account security factor.

Metadata synchronization is manual in this milestone. "Recently seen" means a heartbeat within five minutes at the time the screen renders, not a live connectivity guarantee. Remote permission status is not collected yet.

## Oracle Cloud

The supplied screenshots show an Ubuntu 20.04 `VM.Standard.E2.1.Micro` VM in Hyderabad, 1 GB RAM, a 47 GB boot volume and no extra block volumes. The subsequent SSH session confirms OS, available memory/disk and lack of ESM coverage. Public HTTPS readiness, free-tier billing eligibility and safe capacity for the new backend remain unverified.

Use this VM only as a constrained two-device learning deployment after security/support and capacity checks. Ubuntu 20.04 standard support has ended; use approved extended security coverage or provision a supported LTS replacement. Do not perform an unattended in-place upgrade or destroy existing workloads.

The proposed deployment uses a single bounded JVM process, PostgreSQL and Caddy, pending the existing-VM compatibility gates above. Builds happen on Codemagic, not on the micro VM. The DB and API ports are not published. Private Object Storage and instance-principal IAM will be added in the media phase; there is no reason to collect OCI keys now.

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for costs, tradeoffs and security boundaries. Free-tier limits and region capacity must be confirmed in your own console before provisioning; zero cost and uptime cannot be guaranteed.

## Permissions and Privacy

This foundation declares only `INTERNET` and `ACCESS_NETWORK_STATE`, both normal permissions. It accesses no location, camera, microphone, contacts, SMS, Bluetooth or user files. Passwords and pairing invitations are not saved. Tokens are encrypted under a non-exportable Keystore key; backups/transfers are excluded and screen capture is disabled for this app window.

Device names, installation UUIDs, model/OS information, battery and connectivity metadata are sent only to the configured server after enrollment/synchronization. Account deletion removes active database records via cascading foreign keys; backup expiry is an operator responsibility. Server logs must never include authorization headers, secrets or request bodies.

See [SECURITY.md](SECURITY.md) for threat boundaries and reporting. No administrator privilege bypass, hidden monitoring, other-app data access or silent sensor activation is planned.

## Testing

Run the local, dependency-free structure check:

```powershell
.\scripts\verify-structure.ps1
```

It checks XML syntax, manifest privacy settings, catalog references, script syntax and test-suite presence. It **does not** validate Kotlin behavior. See [docs/TESTING.md](docs/TESTING.md) for unit, repository, HTTP, real PostgreSQL and device tests and [docs/API.md](docs/API.md) for endpoint contracts.

CI installs/resolves its tools on external Codemagic runners (or optional manual GitHub-hosted runners), not this computer. Foundation build 3 passed at `d7bbf07`; later source changes require their own cloud build. Run `python -B -m unittest discover -s tests -p test_preflight.py -v` for the standard-library deployment policy tests. Enable GitHub secret scanning/push protection where available; CI is not a security certification.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Missing wrapper JAR | Bootstrap in an authorized environment, verify checksum, then commit the official JAR. |
| Gradle sync fails | JDK 17, SDK 35, compatible Android Studio, proxy policy and repository access. Do not disable TLS validation. |
| HTTPS connection fails | DNS, certificate hostname/trust, public port 443, OCI route/NSG/security list and host firewall. HTTP/self-signed certificates are intentionally rejected. |
| HTTP 401 | Expired/revoked session; sign in again. A revoked installation cannot silently restore enrollment. |
| HTTP 403 at registration | Incorrect server invitation; never paste it into a public issue. |
| HTTP 429 | Conservative per-minute rate limit or session/pairing cap. Behind Caddy, requests share a proxy-origin limit. Wait or deliberately review the server limits. |
| Pairing rejected | Check account, exact target UUID, five-minute expiry and whether the code was already consumed. |
| Container exits/OOM | Inspect memory/disk/CPU usage without dumping secrets. The 1 GB VM has no measured capacity guarantee. |
| Device absent after reinstall | UUIDs/Keystore keys are installation-specific, not hardware identifiers. Re-register intentionally. |

The GitHub destination is configured as `origin`; confirm the published commit on `main` before starting each cloud build. Official GitHub HTTPS API publication is used when this environment's Git upload fails; no network controls are disabled. No Oracle resource or remote configuration is created automatically. Remaining external gates are the new cloud checks, a DNS hostname, supported server security coverage and measured resource headroom. Keep passwords, SSH keys and OCI tokens out of chat and source control.
