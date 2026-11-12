# Security and Privacy

This is unverified foundation source for a personal-device learning project, not a production-certified management agent. Never deploy with example secrets or place sensitive real data in an unvalidated environment.

## Reporting

Do not publish vulnerabilities, credentials, private device metadata or database dumps in public issues. Until a private reporting channel is configured, contact the repository owner privately through an existing trusted channel. Enable GitHub private vulnerability reporting when publishing the repository.

## Implemented Source Controls

- Explicit local device enrollment and ownership checks on every protected API.
- Device-bound heartbeat authorization and immediate database-backed session revocation.
- Expiring random bearer tokens stored hashed; atomic refresh rotation.
- Installation/account-bound, single-use, short-lived pairing codes.
- Salted PBKDF2-HMAC-SHA256 passwords and invitation-gated account creation.
- Bounded JSON bodies, rate limits, SQL parameters and generic internal errors.
- Android Keystore AES-GCM token storage, backup/transfer exclusions, HTTP/redirect rejection and screen-capture protection.
- Private DB/API container ports, non-root/read-only API runtime and public TLS proxy.
- Active-database account deletion and bounded successful-action audit records.

Source presence does not mean these controls have passed runtime or penetration tests. Consult the testing ledger.

## Threat Boundaries and Gaps

Trust the configured server, its administrator, system TLS roots and device OS. Rooted/compromised phones, a compromised account or server, malicious keyboards and physical access to unlocked devices are not neutralized by Keystore encryption. Installation UUIDs and metadata are not hardware-attested. A signed-in owner can enroll a new installation; device revocation does not revoke the owner's account access.

Tokens are bearer credentials. No hardware proof-of-possession, MFA/passkey, verified-email recovery, refresh-family compromise detection, device attestation, per-operation reauthentication or public-service abuse defense is supplied. Rate limits are single-process/in-memory and shared behind the proxy. The service is intended for a few personal devices, not public multi-tenancy.

Audit records cover successful identity/device actions, not a complete forensic trail. Database/VM/object-storage encryption and operational secret injection require OCI/operator configuration; server fields are not application-layer end-to-end encrypted. Do not log request/response bodies or authorization headers. Environment secrets are readable by privileged operators.

Account deletion removes live rows. Separate encrypted backups must expire on the disclosed retention schedule and deletions must be re-applied after restoration. Automated backups, deletion-ledger management and object deletion are not yet implemented.

## Mandatory Deployment Gates

Use a supported/patched OS, trusted TLS, reviewed dependencies/images, least-privilege administration, private storage, tested backup restoration and verified ownership/revocation tests. Keep the 1 GB VM's actual resource consumption under review. Ubuntu 20.04 without approved extended security coverage is not an acceptable production baseline.

Never install a network/certificate bypass to make a build succeed. The local wrapper download was policy-blocked; obtain it only through approved development/CI environments and verify official checksums.

## Future Capabilities

No root/accessibility abuse, silent camera/microphone activation, private-app data extraction or call-recording protection bypass is permitted. Later sensor features must request contextual permissions, respect denial/revocation, show required foreground indications and require local approval for remote requests. Device Administrator access is not unrestricted control.