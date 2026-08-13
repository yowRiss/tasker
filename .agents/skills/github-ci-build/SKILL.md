---
name: github-ci-build
description: Guidelines and instructions regarding build execution. Reminds agents that building/compiling binaries (including Android ./gradlew and Go binary builds) is done in GitHub Actions CI/CD, not locally on the host machine.
---

# GitHub CI/CD Build Skill & Instruction

## Rule & Guideline
1. **Local Machine Is Development-Only**: Do NOT attempt to run full application builds, Android Gradle compilations (`./gradlew assemble`), or production binary builds on the local user machine.
2. **GitHub Actions CI/CD Ownership**: All compilation, APK/AAB generation, and production packaging are executed automatically in GitHub CI/CD pipelines.
3. **Agent Scope**:
   - Write clean, type-safe, well-structured code.
   - Perform static checks (such as `go vet`) or inspect code structure when tools are present.
   - Do not fail or block a turn trying to execute `./gradlew` or missing local SDK build tools.
   - Trust GitHub Actions CI/CD to handle the build and report pipeline status.
