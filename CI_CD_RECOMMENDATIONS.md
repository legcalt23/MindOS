# CI_CD_RECOMMENDATIONS.md

**Date:** 2026-05-27

---

## 1. Current CI

### Existing workflows

| Workflow | Scope | Trigger |
|---|---|---|
| `ci.yml` | Functions: scan, typecheck, test, build + npm audit + secret scan | push/PR to main |
| `security.yml` | npm audit + secret scan | Weekly Monday + manual |

### Gap

**No CI for Kotlin/Android/iOS builds or tests.** Only the Node.js backend (Cloud Functions) is covered.

---

## 2. Recommended CI additions

### Job 1: Shared tests

```yaml
kotlin-tests:
  name: Kotlin shared tests
  runs-on: ubuntu-latest
  timeout-minutes: 20
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
    - uses: gradle/actions/setup-gradle@v4
    - name: Run shared tests
      run: ./gradlew :shared:desktopTest --no-daemon
```

### Job 2: Android build

```yaml
android-build:
  name: Android debug build
  runs-on: ubuntu-latest
  timeout-minutes: 30
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
    - uses: gradle/actions/setup-gradle@v4
    - name: Build Android debug
      run: ./gradlew :androidApp:assembleDebug --no-daemon
```

### Job 3: Detekt

```yaml
detekt:
  name: Detekt static analysis
  runs-on: ubuntu-latest
  timeout-minutes: 15
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
    - uses: gradle/actions/setup-gradle@v4
    - name: Run detekt
      run: ./gradlew :shared:detekt --no-daemon
```

### Job 4: iOS build (macOS runner, optional)

```yaml
ios-framework:
  name: iOS framework build
  runs-on: macos-latest
  timeout-minutes: 30
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
    - uses: gradle/actions/setup-gradle@v4
    - name: Build iOS framework
      run: ./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64 --no-daemon
```

---

## 3. Priority

| Job | Priority | Cost |
|---|---|---|
| Shared tests | P1 | Free (ubuntu) |
| Android build | P1 | Free (ubuntu) |
| Detekt | P2 | Free (ubuntu) |
| iOS framework | P3 | macOS runner cost |
