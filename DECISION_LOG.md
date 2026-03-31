# Decision Log — Hybrid Test Framework

> A record of the key technical decisions, trade-offs, and rationale behind this hybrid (UI + API) test automation framework.

---

## 1. Scope Interpretation & Timeboxing

| Decision | Rationale |
|---|---|
| **Hybrid framework (UI + API in one repo)** | A single repository with a shared Gradle build keeps the CI pipeline simple, avoids dependency synchronisation across repos, and lets both test types share the same config, logging, and reporting infrastructure. |
| **Two public demo apps as targets** | [SauceDemo](https://www.saucedemo.com) (UI) and [restful-booker](https://restful-booker.herokuapp.com) (API) were chosen because they are freely available, stable, require no sign-up, and have realistic enough surface area to demonstrate a production-grade framework without spending time on app setup. |
| **Framework code in `src/main`, tests in `src/test`** | Separating reusable framework utilities (driver factory, config, page objects, video recorder, allure helpers) from test logic follows the standard Gradle convention and ensures framework code can be published as a library JAR if needed. |
| **Timebox trade-offs** | Video recording, Allure history trends, CI/CD with GitHub Pages, retry logic, CVE remediation, and multi-environment config were all scoped in. Items deferred: full cross-browser matrix in CI, Docker-based execution, and database-backed test data. |

---

## 2. Test Selection & Coverage Rationale

### UI Tests — `SmokeFlowTest` (3 tests)

| Test | Type | Why |
|---|---|---|
| `testCompletePurchaseFlow` | Happy-path E2E | Covers the **critical business flow**: Login → Products → Add to Cart → Checkout → Order Complete. A single test that touches every page object proves the framework wiring end-to-end. |
| `testLoginWithInvalidCredentials` | Negative | Validates error handling for the most common user mistake — wrong credentials. |
| `testLoginWithLockedOutUser` | Negative / Edge | Tests a server-enforced lockout — a scenario the happy path would never exercise. |

> **Why only 3 UI tests?** UI tests are slow and flaky by nature. The goal was to validate the framework (driver lifecycle, page objects, video recording, screenshot-on-failure, Allure integration) rather than exhaustively test SauceDemo. More UI tests would add execution time without proving additional framework capability.

### API Tests — `BookingTests` (17 tests)

| Category | Count | Examples |
|---|---|---|
| CRUD — Positive | 4 | Create, Read, List, Update a booking |
| CRUD — Negative / Auth | 4 | Create with empty body, Update without auth, Get non-existent ID |
| Boundary | 4 | Negative price, MAX_VALUE price, zero price, very long names |
| Schema Validation | 1 | JSON Schema validation on GET response |
| Parameterized (DataProvider) | 5 | Data-driven from `bookings.json` — valid, boundary, and missing-field cases |
| Authentication | 3 | Valid creds, invalid creds, empty creds |

> **Why heavier API coverage?** API tests are fast (no browser), deterministic, and cheap to maintain. They give the best ROI for coverage. The test mix demonstrates: positive/negative paths, boundary values, schema contracts, data-driven parameterization, and authentication flows — all patterns that a production framework needs.

### Coverage Strategy

```
                 ┌─────────────────────────────┐
                 │        API Tests (17)        │  ← Fast, high coverage
                 │   CRUD · Boundary · Schema   │
                 │   Auth · Data-Driven (JSON)  │
                 ├─────────────────────────────┤
                 │       UI Tests (3)           │  ← Slow, critical paths only
                 │   E2E Smoke · Login Negative │
                 └─────────────────────────────┘
```

This follows the **test pyramid** — many fast API tests at the base, few slow UI tests at the top.

---

## 3. Stability & Data Strategies

### Test Isolation

| Area | Strategy |
|---|---|
| **WebDriver** | `ThreadLocal<WebDriver>` in `DriverFactory` — each test method gets its own browser instance via `@BeforeMethod`/`@AfterMethod`, enabling safe parallel execution. |
| **API State** | Each API test creates its own data (`POST /booking`) before operating on it. The `createdBookingId` field chains dependent tests via `dependsOnMethods` to guarantee ordering within the API test class. |
| **Config** | `ConfigManager` is a thread-safe singleton (`synchronized getInstance()`). System properties and env vars override file values, so CI can inject config without touching source. |
| **Multi-environment** | `ConfigManager` loads `config.properties` (base defaults), then overlays `config-{env}.properties` when `-Denv=qa|staging|prod` is passed. System properties (`-Dkey=val`) and OS env vars take highest priority. This layered approach avoids duplication — only values that differ per environment need to be in the overlay file. |

### Flaky Test Mitigation

| Technique | Implementation |
|---|---|
| **Automatic retry** | `RetryAnalyzer` retries failed tests once (configurable via `-Dtest.retry.max=N`). Applied globally via `RetryTransformer` listener registered in `testng-suite.xml`. |
| **Explicit waits** | All page objects use `WebDriverWait` with a configurable timeout (`timeout.seconds` in config) — no `Thread.sleep` anywhere. |
| **Headless by default** | `headless=true` eliminates flakiness from window focus, rendering, and display issues in CI. |
| **`--continue` flag** | Gradle's `--continue` ensures all tests run even if earlier ones fail — prevents partial results. |

### Test Data

| Source | Usage |
|---|---|
| `config.properties` | Credentials and URLs — overridable via `-D` flags or env vars for different environments. |
| `bookings.json` | Parameterized API test data — 5 entries covering valid, boundary, and negative cases. Loaded via Jackson `ObjectMapper` in a `@DataProvider`. |
| `booking-schema.json` | JSON Schema for API response contract validation. |
| Hardcoded helpers | `createDefaultBooking()` provides a clean baseline object for non-parameterized API tests. |

> **Dates in test data** use future dates (2026) to avoid API validation failures from past-date rejection.

---

## 4. Project Structure Decisions

```
hybrid-test-framework/
├── src/main/java/com/framework/     ← Reusable framework (could be a separate lib)
│   ├── config/ConfigManager          Singleton, env-aware, multi-environment layered loading
│   ├── driver/DriverFactory          ThreadLocal, Chrome/Firefox, headless support
│   ├── logging/Log                   Dual-output: Log4j2 console/file + Allure steps
│   ├── pages/                        Page Object Model (Login, Products, Cart, Checkout)
│   ├── recording/VideoRecorder       Selenium screenshot → MP4 via JCodec
│   └── reporting/AllureAttachmentHelper  Screenshot, video, text, HTML source
├── src/main/resources/
│   ├── config.properties             Base defaults (always loaded)
│   ├── config-qa.properties          QA environment overlay
│   ├── config-staging.properties     Staging environment overlay
│   ├── config-prod.properties        Production environment overlay
│   ├── log4j2.xml                    Console + rolling file appender
│   └── allure.properties             Allure results directory
├── src/test/java/
│   ├── com/framework/listeners/      RetryAnalyzer + RetryTransformer
│   └── com/tests/
│       ├── api/                      BaseApiTest, BookingTests, POJOs
│       └── ui/                       BaseUiTest, SmokeFlowTest
├── src/test/resources/
│   ├── categories.json               Allure report categories
│   ├── schemas/                      JSON Schema files
│   ├── suites/testng-suite.xml       Parallel tests, retry listener
│   └── testdata/bookings.json        Data-driven test input
├── .github/workflows/
│   └── test-and-report.yml           CI/CD: test → Allure → GitHub Pages
├── build.gradle                      Dependency management, test config, Allure env
├── run-tests.sh                      Local convenience runner
└── README.md                         Setup, usage, config reference
```

### Key Structural Decisions

| Decision | Why |
|---|---|
| **Page Object Model in `src/main`** | Page objects are framework code, not tests. Keeping them in `main` allows packaging as a library. |
| **Listeners in `src/test`** | `RetryAnalyzer` and `RetryTransformer` depend on TestNG (`testImplementation` scope) — they cannot compile under `src/main`. |
| **No Allure Gradle plugin** | The official `io.qameta.allure` plugin (v3.0.2) uses the deprecated `Convention` API removed in Gradle 9. Instead, AspectJ weaver is configured manually via a custom `agent` configuration. |
| **JCodec for video (not Monte)** | Monte Screen Recorder captures the OS display — invisible in headless mode. JCodec encodes Selenium screenshots into H.264 MP4 purely in Java, works everywhere, and plays inline in Allure reports. |
| **Log4j2 + Allure dual logging** | `Log` utility writes every message to both Log4j2 (for console/file debugging) and Allure steps (for report traceability). Developers see logs in the terminal; stakeholders see them in the report. |
| **CVE remediation** | WebDriverManager upgraded `5.7.0 → 6.1.0` (CVE-2025-4641 CRITICAL: XXE). Log4j upgraded `2.23.1 → 2.25.3` (CVE-2025-68161 MEDIUM: TLS hostname bypass). |
| **Gradle 9 + JDK 21 for build, Java 17 target** | `sourceCompatibility = 17` ensures compiled code runs on Java 17+. Gradle 9 requires JDK 21+ to execute. CI uses JDK 21 for the build tool, not for the application target. |

---

## 5. Next Steps if Given More Time

### High Priority

| Item | Description |
|---|---|
| **Cross-browser CI matrix** | Add a GitHub Actions matrix strategy for `[chrome, firefox]` × `[headless, headed]` to catch browser-specific regressions. |
| **Docker-based execution** | Create a `docker-compose.yml` with Selenium Grid (Chrome/Firefox nodes) for consistent, reproducible execution independent of the host OS. |
| **Independent API tests** | Remove `dependsOnMethods` and `createdBookingId` coupling in `BookingTests`. Each test should create and clean up its own data for full isolation. |
| **Separate Allure Gradle plugin** | Fork or contribute to `allure-gradle` to add Gradle 9 compatibility, then replace the manual AspectJ agent wiring. |

### Medium Priority

| Item | Description |
|---|---|
| **TestNG groups** | Tag tests with `groups = {"smoke", "regression", "api", "ui"}` and add Gradle tasks like `./gradlew smokeTest`. |
| **Dependency vulnerability scanning** | Integrate the OWASP `dependency-check` Gradle plugin or GitHub Dependabot for continuous CVE monitoring. |
| **Gradle version catalog** | Migrate dependency versions from `build.gradle` to `gradle/libs.versions.toml` for centralized, IDE-friendly version management. |
| **Parallel UI tests** | Switch from `parallel="tests"` to `parallel="methods"` in testng-suite.xml once all UI tests are fully isolated. |

### Nice to Have

| Item | Description |
|---|---|
| **Code quality gates** | Add Checkstyle, SpotBugs, or SonarQube analysis to the CI pipeline. |
| **Visual regression testing** | Integrate a tool like Ashot or Percy to compare screenshots across runs. |
| **Slack/Teams notifications** | Post test results summary and Allure report link to a chat channel from CI. |
| **Performance tests** | Add a Gatling or k6 module to the same Gradle project for load testing the API. |
| **Allure TestOps integration** | Connect to Allure TestOps for test case management, launch tracking, and defect linking. |

---

*Last updated: March 31, 2026*

