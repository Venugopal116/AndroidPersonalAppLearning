# Contributing

Keep one narrowly testable capability per change. Preserve explicit device-user consent and Android security boundaries. Do not introduce placeholder production repositories, no-op APIs or screens that claim unavailable functionality.

Use Kotlin, the central version catalog, constructor injection and existing module ownership. Add unit/repository/API tests with behavior changes, and instrumented tests when Android Keystore, permissions or services are involved. Test PostgreSQL changes against PostgreSQL, not just H2. Add versioned migration handling before changing a deployed V001 schema.

Run the README's build/test/lint commands in an authorized development environment and record which checks actually ran. Do not install tools or dependencies on the restricted authoring computer. Update the delivery ledger and documentation with honest pending gates.

Never commit `.env`, private keys, signing keys, credentials, tokens, device exports or database backups. Use synthetic test values only. Enable private security reporting, dependency alerts and secret scanning where the repository plan permits them.

GitHub workflow tools and container base images need regular reviewed updates. Pin reviewed immutable references before production. Releases require two-device acceptance, a restored backup test, TLS/authorization review and measured server resource limits.