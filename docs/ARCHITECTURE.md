# Foundation Architecture and Decisions

## Scope and Ownership

The first delivery is account and device identity, not a collection of unimplemented sensor modules. Compose talks to a ViewModel through actions and StateFlow; a repository owns API calls, refresh serialization and local session management. Shared serialized DTOs avoid Android/backend contract drift. Explicit constructor injection avoids a DI framework for this small foundation.

Three Gradle modules are sufficient initially. Feature packages can become modules when build isolation or ownership justifies it. Room and WorkManager will be introduced with durable history/scheduled synchronization, not declared unused in phase 3. No fake production implementation is used; test doubles exist only under test sources.

## Backend

Ktor serves `/v1` JSON APIs with PostgreSQL JDBC transactions. The service uses one serialized DB transaction at a time and no pool, appropriate only for a small personal deployment. Hash computation is outside DB transactions. Requests are capped at 16 KiB, access is allowlisted by route, and request-origin rate limits constrain expensive operations. Rate limiting is in memory, resets on restart and is not distributed.

The first schema uses idempotent initial DDL at startup. There is no general migration runner yet. Before the next schema change, introduce versioned migrations with checksums and test an upgrade from V001; do not silently edit a deployed schema. Keep one backend replica until transactional locking/migration ownership and distributed rate limiting are addressed.

PostgreSQL was selected instead of Autonomous Database to keep portable SQL, simple local integration tests and a single deployment. This increases operator responsibility for patching, backup/restore and disk capacity. H2 tests are fast but do not replace the real PostgreSQL CI gate.

## Credentials and Authorization

Passwords use unique 128-bit salts and PBKDF2-HMAC-SHA256 with 600,000 iterations and a 256-bit result. This replaces the proposal's Argon2id choice to keep the 1 GB backend free of native bindings and memory-hard concurrency spikes. Passwords are bounded to 128 characters and registration is invitation-gated. Benchmark on the actual VM before exposing it; resource bounds are not proof of capacity.

Access and refresh tokens are independent 256-bit random bearer secrets. Only SHA-256 digests are stored server-side. Access expires after ten minutes; refresh after seven days. Refresh replaces the old session atomically, making old access/refresh tokens invalid. There is no JWT acceptance window after revocation. There is no refresh-token family compromise detection or passkey/MFA support yet.

Account sessions are initially unbound and can list/manage the owner's devices. Enrollment binds that session to one device. Heartbeat requires the authenticated session to match the target device and installation UUID. Revocation deletes all bound sessions, rejects re-enrollment of that installation and preserves metadata. Every HTTP operation re-authenticates. Installation UUIDs are not hardware secrets, attestation or proof of physical possession.

Pairing codes are 256-bit, stored hashed, bound to account/installation, expire in five minutes and are consumed transactionally. Both devices sign into the same account. Codes cannot override an existing enrollment or restore a revoked installation. The Android registration button is the local approval action. Camera/audio request approval will be a separate feature, never implied by pairing.

Android tokens are AES-GCM encrypted with fresh IVs, authenticated context and a non-exportable Android Keystore key. Corrupt/inaccessible records fail closed. Backup/transfer exclusions prevent restoring installation identifiers or unusable encrypted tokens. The app trusts normal system CAs, disallows HTTP, rejects endpoint credentials/query/path and disables redirects on authenticated requests. No certificate-verification bypass or hardcoded cloud credential exists.

## Cloud Limits and Costs

Reference: [OCI Always Free resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm), consulted during planning on 2026-11-12. Verify the console, current pricing and tenancy eligibility before applying anything.

- The published A1 allowance is 1,500 OCPU-hours / 9,000 GB-hours monthly, equivalent to 2 OCPUs / 12 GB RAM for an Always Free tenancy. Do not copy older 4-OCPU/24-GB tutorials blindly.
- Up to two eligible AMD E2 micro instances are listed. The existing micro VM is a shared/burst-limited small instance, not evidence of dedicated CPU capacity or a free bill.
- Boot and block volumes share a 200 GB allowance; existing resources consume it. The attached 47 GB boot volume does not establish available filesystem space.
- Free-only Object Storage is listed as 20 GB combined and 50,000 API requests monthly. Trial/paid account tier allocations differ. Multipart uploads and abandoned parts consume operations/storage.
- Two Always Free Autonomous databases are documented, each with fixed resource/session limits. Availability depends on region; they are not required here.
- Always Free VM capacity may be unavailable; idle instances can be reclaimed. There is no production availability promise.
- No paid API gateway, managed database, paid load balancer or NAT gateway is needed for this proposal. Traffic, logs, backups, certificate/DNS arrangements and services outside free limits still need cost review.

Budget alerts notify; they do not stop spending. Use appropriate compartment quotas, object quotas/retention and operator review. Do not create traffic simply to evade idle reclamation.

## Future Data and APIs

Planned tables: permission snapshots; encrypted local location/contact payloads; server location history; media metadata; multipart upload sessions; expiring, allowlisted remote requests; deletion jobs. These are design items, not existing schema. Private OCI storage will use server-side instance-principal authorization; Android will never receive OCI credentials. Start with authenticated API streaming, then evaluate narrowly scoped short-lived transfer grants if needed.

Without FCM or another supported push transport, background request delivery relies on inexact polling and may be delayed indefinitely by force-stop/offline conditions. OCI Notifications is not an Android wake-up transport. Sensor operations always require visible local approval, permission checks and supported foreground-service lifecycle.

## Release Gates

Pending: official wrapper vendoring, Kotlin compilation, all tests, actual HTTPS round trip, backup restoration, dependency vulnerability review, server resource measurements and Moto/Vivo testing. Registration/email recovery, MFA, encrypted local history, media deletion jobs and a general migration system are outside this first milestone. This source is not a production security certification.