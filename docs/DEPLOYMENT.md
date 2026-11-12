# Deploying the Foundation to OCI

These are operator instructions, not commands executed by this assistant on your server. The owner has connected through Oracle Cloud Shell and supplied read-only results. Never provide private keys, passwords, invites, access tokens or OCI configuration files in chat or Git.

## Existing VM: Preserve the Current Workload

The selected host is the existing Hyderabad E2 micro VM. The user wants to reuse it, not recreate it. It runs a Telethon script that this project must not stop, inspect for credentials, update or otherwise alter. SSH results from 2026-11-12 confirm Ubuntu 20.04.6, no active Ubuntu Pro subscription/ESM, 952 MiB RAM total, about 504 MiB available, no swap and about 42 GB free on `/`.

**Do not run the deployment commands below yet.** The current container caps total 736 MiB, exceeding that observed available RAM, and are not reserved memory or a safe-capacity estimate. Collect peak resource use and inventory already installed Java, PostgreSQL, proxy and container tooling without changing anything. Then design and validate a smaller coexistence plan, or use approved separate infrastructure if the VM cannot safely support it. Never silently replace PostgreSQL with an in-memory production store to make it fit.

The existing OS can be retained only with an approved security-maintenance plan; Ubuntu Pro eligibility/ESM activation and applying fixes require operator review. Do not run package upgrades, reboot, provision another VM, enable paid services or add swap without explicit approval and a recovery plan for the existing workload. Codemagic only produces artifacts and has no SSH/deployment credentials.

## Prerequisites and Approval Gates

1. Confirm the VM's existing workloads, free disk space, current memory/CPU load, tenancy billing eligibility and region quotas. The screenshots show only 1 GB RAM and a 47 GB attached boot volume.
2. Ubuntu 20.04 needs approved extended security coverage or replacement with a supported LTS. Prefer a separately provisioned supported VM when existing data must be preserved. Backup before any OS change. An operator must install required server software through approved processes; do not attempt elevation from the restricted authoring computer.
3. Supply a DNS name you control, pointing to the VM's public IP. Confirm the IP assignment remains valid. Do not hardcode the screenshot's address into Android source.
4. Verify a public-subnet route to an Internet Gateway and the required OCI NSG/security-list and host-firewall rules. The console's "Connect public subnet" quick action does not prove routing is already configured. Inbound TCP 443 is required. TCP 80 supports redirects/certificate issuance. Restrict SSH 22 to an approved administrator IP or use an approved bastion.
5. Never expose PostgreSQL 5432 or the backend 8080 to the internet. Prefer a dedicated compartment, least-privilege administration and no unrelated public services.
6. Docker Engine and Compose must already be installed/approved on the server. They are not installed by this repository. Access to the Docker socket is effectively privileged; do not grant it casually.

## Build Away From the Micro VM

### Codemagic Image Archive

Select the manual `backend-image` workflow on `main` after the updated foundation tests pass. Jib packages the application against the Temurin 17 base for Linux/amd64, even though the runner is an ARM Mac. No Docker daemon or registry login is needed on that runner. Download `backend-deployment.tar.gz` from Codemagic Artifacts and transfer it through an approved method to a new dedicated directory on the VM. Do not put keys/secrets in the archive or replace existing application directories.

After reviewing and extracting the archive, run only the read-only check first:

```sh
python3 deploy/preflight.py --json
```

It prints no process arguments, account/subscription details or private keys. It runs no privileged commands and makes no service, firewall, package or OS changes. It fails closed for unrecognized/missing Ubuntu security support, less than 600 MiB current available RAM, insufficient disk, unavailable Docker/Compose clients or occupied/unknown public ports. The 600 MiB threshold allows the 400 MiB profile plus a 200 MiB provisional coexistence margin. An attached/enabled ESM report is not proof all patches have been applied. Passing is only eligibility for staging review; peak workload, backups and recovery still require operator verification.

The previous VM report (504 MiB available, Ubuntu 20.04 without ESM) does not pass. Do not work around the report by lowering its thresholds, killing Telethon or silently adding swap. Resolve the specific blockers with an approved plan.

Only after all gates, trusted DNS/TLS configuration, reviewed staging tests and explicit operator approval, a Docker-authorized operator can load the prebuilt image and validate the smaller configuration:

```sh
docker load -i backend/build/jib-image.tar
docker compose --env-file deploy/.env -f deploy/compose.yml -f deploy/compose.micro.yml config --quiet
```

The micro override caps PostgreSQL/API/Caddy at 96/256/48 MiB. `JAVA_TOOL_OPTIONS` applies the runtime limits to both Jib and distribution-based images; do not put secrets in it because the JVM can print those options. These caps have not been benchmarked and may cause OOM/restarts under load. Validate a staging launch and recovery before using the VM's shared resources. The normal startup command below is not approval to launch this smaller profile untested.

No domain purchase is required: choose an available subdomain with a DNS provider whose terms permit your use, and set its A record to the current public VM IP through the provider's authorized interface. DNS registration/account authorization must be performed by the owner; do not put provider tokens in Android or source. Test trusted HTTPS before entering the origin into the app. No hostname has been registered by this project, and the app still has no live server address until deployment is complete.

Use the Codemagic `android-foundation` workflow for the backend distribution ZIP and Android debug APK. Do not compile Kotlin or build Gradle dependencies on the 1 GB server. The following local commands are optional for a separate, authorized build host, not prerequisites on the restricted computer:

```powershell
.\gradlew.bat -PbackendOnly :shared:test :backend:test :backend:installDist
docker build -f deploy/Dockerfile -t personal-device-backend:0.1.0 .
```

The Docker command requires an approved build host with Docker; it is not a requirement for building the Android app. Transfer the resulting image through an approved private registry or image archive. The `android-foundation` workflow provides a distribution ZIP; the optional GitHub workflow uploads the JVM distribution directory. Use `backend-image` for the ready-to-load image archive instead. No workflow deploys automatically or publishes to a registry, and no registry credentials are supplied. A non-container service deployment would require a separately reviewed resource/permissions configuration; none has been validated yet.

Review current security updates and pin reviewed image digests before a production deployment. The readable major-version tags in Compose are a learning baseline, not immutable supply-chain locks.

## Server Configuration

Place the reviewed deployment files on the server. Create a private `deploy/.env` from the field names in [deploy/.env.example](../deploy/.env.example). Replace all example values with operator-generated secrets, and protect the file with owner-only permissions through your normal administration process. Example values must never be used as live credentials.

- `API_DOMAIN`: DNS hostname only, without scheme or path.
- `BACKEND_IMAGE`: the reviewed image available to the server.
- `DATABASE_PASSWORD`: independent random database secret of at least 24 characters.
- `REGISTRATION_CODE`: independent random invitation of at least 24 characters. Share privately for account creation and rotate after onboarding.

The backend refuses the supplied `replace-` example values and secrets shorter than 24 characters. The app receives only the HTTPS origin, user credentials and scoped application tokens. OCI API keys are unnecessary for this phase. Environment secrets are visible to privileged container administrators; use OCI Vault/approved secret injection before broader deployment.

From the reviewed deployment directory on the authorized server:

```sh
docker compose --env-file .env -f compose.yml config --quiet
docker compose --env-file .env -f compose.yml up -d
```

Do not print fully expanded Compose configuration or container environment values into tickets/logs. Caddy obtains a publicly trusted certificate when DNS, routing and certificate validation work. Android does not accept a fallback HTTP connection or a disabled certificate check.

The backend listens inside its container on 8080, with no host port mapping. Outside containers its default bind is loopback-only. PostgreSQL uses a private Compose network with no published port. The backend runs non-root with a read-only filesystem; Caddy/PostgreSQL retain only their required volumes.

Memory caps: PostgreSQL 192 MiB, API 448 MiB and Caddy 96 MiB. This leaves limited space for Ubuntu/Docker and is **not a benchmark or capacity guarantee**. The API heap is capped at 192 MiB, with bounded native/metaspace settings. Monitor actual memory, CPU, disk, restarts and latency; downsize workload or move to verified available capacity if the VM cannot sustain it. Do not add media processing, antivirus scanning or Gradle builds to this VM.

## Acceptance Checks

From an authorized client, request `https://YOUR_DOMAIN/health`; expect `{"status":"ok"}` and a valid certificate. Then request `/ready`; expect `{"status":"ready","service":"personal-device-lab","apiVersion":1}` after a database connection check. A database outage must not pass the app's connection check. These probes are not proof of authenticated database writes. Complete the README's two-device flow and verify metadata through the authenticated device API. Confirm unauthorized requests receive 401 and an unowned device operation cannot succeed.

Verify the DB/API ports are not public using approved cloud configuration review and authorized diagnostics, not unsolicited scanning. Test restart persistence and session revocation. Do not mark the connection complete until both real phones pass against the actual deployment.

## Backups, Deletion and Recovery

The supplied stack does not automate backups. Before storing real data, schedule encrypted PostgreSQL backups through your approved tooling, store copies separately from the VM, enforce a documented retention window (recommended seven days for this learning environment), and perform a restore test into an isolated database. Boot-volume backups alone are not a tested database recovery procedure.

Account deletion removes live relational data, not backup copies already created. Those copies expire through the operator's retention policy. Record account deletions outside retained snapshots and reapply them after any restore; document how that ledger is protected. Disclose retention to users. No cloud-object data exists in this phase.

Named database/Caddy volumes persist across normal container replacement. Never use volume-removal flags casually. Before schema changes, add tested, versioned migrations and verify rollback/recovery. The first schema initializer is not a full migration platform.

Do not assume free-tier resources are backed up, always available or permanently retained. Keep encrypted off-instance backups and deployment documentation. Oracle can reclaim idle free instances; do not manufacture load to evade that policy.

## Object Storage Later

When authorized-media transfers are implemented: create a private bucket in the eligible region; define a narrowly scoped dynamic group/instance principal; enforce quotas, file validation, multipart cleanup and deletion; review current storage/request/egress costs. Nothing in this milestone creates a bucket or grants access to it.