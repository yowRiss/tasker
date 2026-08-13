---
trigger: always_on
---

# CI/CD Build Environment Rule

- Do NOT run full application builds, Android Gradle compilations (`./gradlew assemble`), or production binary builds on the local machine.
- All compilation, APK/AAB generation, and production packaging are executed automatically in **GitHub Actions CI/CD**.
- Agents should write clean code, update contracts/documentation, run static checks if available (like `go vet`), and let GitHub Actions CI perform the build.
