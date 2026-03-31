# Hybrid Test Framework

A production-ready hybrid test automation framework combining **UI** (Selenium) and **API** (REST Assured) testing with **Allure** reporting, built on **Java 17 / Gradle / TestNG**.

---

## ✨ Features

| Category | Details |
|---|---|
| **UI Testing** | Selenium 4 + Page Object Model (SauceDemo) |
| **API Testing** | REST Assured with JSON Schema Validation (restful-booker) |
| **Test Runner** | TestNG with parallel execution & automatic retry |
| **Reporting** | Allure Reports with screenshots, video, environment info & trend history |
| **Video Recording** | Selenium-based MP4 capture — works in headless mode |
| **CI/CD** | GitHub Actions → Allure Report published to GitHub Pages |
| **Logging** | Log4j2 + SLF4J with console & rolling file output |
| **Config** | Multi-environment support (`-Denv=qa|staging|prod`) with layered config overrides |

---

## 📋 Prerequisites

- **Java 21+** (Gradle 9 requires JDK 21; compiled code targets Java 17)
- **Chrome** or **Firefox** browser installed
- **Allure CLI** (optional, for local report viewing)

```bash
# Install Allure CLI (macOS)
brew install allure
```

---

## 🚀 Quick Start

### Run all tests
```bash
./gradlew test
```

### Run with options
```bash
# Target a specific environment
./gradlew test -Denv=staging

# Firefox, non-headless, against prod
./gradlew test -Denv=prod -Dbrowser=firefox -Dheadless=false

# Override a single property (highest priority)
./gradlew test -Denv=qa -Dapi.base.url=https://your-api.com
```

### Using the shell script
```bash
./run-tests.sh                        # Default env + generate report
./run-tests.sh --env staging          # Run against staging
./run-tests.sh --env prod --serve     # Run against prod + open report
./run-tests.sh --help                 # Show all options
```

---

## 📊 Allure Report

### Generate locally
```bash
# After running tests:
allure generate build/allure-results -o build/allure-report --clean
allure open build/allure-report
```

### CI/CD (GitHub Pages)
Reports are automatically published on every push to `main`/`master`:
```
https://<username>.github.io/<repo-name>/
```

---

## 🏗️ Project Structure

```
src/
├── main/java/com/framework/
│   ├── config/         ConfigManager (singleton, env-aware)
│   ├── driver/         DriverFactory (thread-safe WebDriver)
│   ├── listeners/      RetryAnalyzer, RetryTransformer
│   ├── logging/        Log (Log4j2 + Allure steps)
│   ├── pages/          Page Objects (Login, Products, Cart, Checkout)
│   ├── recording/      VideoRecorder (Selenium screenshot → MP4)
│   └── reporting/      AllureAttachmentHelper
├── main/resources/
│   ├── config.properties       Base defaults
│   ├── config-qa.properties    QA environment overrides
│   ├── config-staging.properties  Staging overrides
│   ├── config-prod.properties  Production overrides
│   ├── log4j2.xml
│   └── allure.properties
└── test/
    ├── java/com/tests/
    │   ├── api/        BaseApiTest, BookingTests, POJOs
    │   └── ui/         BaseUiTest, SmokeFlowTest
    └── resources/
        ├── categories.json
        ├── schemas/    JSON schemas for API validation
        ├── suites/     testng-suite.xml
        └── testdata/   bookings.json
```

---

## ⚙️ Configuration

### Multi-Environment Support

The framework supports environment-specific configuration via `-Denv`:

```bash
./gradlew test -Denv=staging
```

**Config files loaded (in order — later wins):**

| Order | File | Purpose |
|:---:|---|---|
| 1 | `config.properties` | Base defaults (always loaded) |
| 2 | `config-{env}.properties` | Environment overlay (e.g. `config-staging.properties`) |
| 3 | System properties (`-Dkey=val`) | CLI overrides — highest priority |
| 4 | OS env variables (`KEY_NAME=val`) | CI/CD secret injection |

**Available environments:**

| Environment | File | Notes |
|---|---|---|
| `default` | `config.properties` only | No `-Denv` flag — base config |
| `qa` | `config-qa.properties` | QA environment |
| `staging` | `config-staging.properties` | Pre-production, longer timeouts |
| `prod` | `config-prod.properties` | Production, video disabled |

### Properties Reference

| Property | Default | Description |
|---|---|---|
| `browser` | `chrome` | Browser: `chrome` or `firefox` |
| `headless` | `true` | Run browser headless |
| `timeout.seconds` | `10` | WebDriverWait timeout |
| `video.enabled` | `true` | Record video (MP4) per test |
| `ui.base.url` | `https://www.saucedemo.com` | UI test base URL |
| `ui.username` | `standard_user` | UI login username |
| `ui.password` | `secret_sauce` | UI login password |
| `api.base.url` | `https://restful-booker.herokuapp.com` | API base URL |
| `api.auth.username` | `admin` | API auth username |
| `api.auth.password` | `password123` | API auth password |

> **Tip:** For local overrides, create a `config-local.properties` file (gitignored).

---

## 🔄 CI/CD — GitHub Actions

The workflow (`.github/workflows/test-and-report.yml`) runs on:
- Push to `main` / `master`
- Pull requests
- Daily schedule (6 AM UTC)
- Manual dispatch (choose environment, browser & headless mode)

### Enable GitHub Pages
1. Go to **Settings → Pages**
2. Set **Source** to **GitHub Actions**
3. Push to `main` — the workflow deploys the Allure report automatically

### Allure History & Trends
The workflow preserves Allure history across runs via the `gh-pages` branch, enabling trend charts in the report.

---

## 🔒 Security Notes

- Credentials in `config.properties` are for **public demo sites** only
- For real projects, use **GitHub Secrets** + environment variables:
  ```yaml
  env:
    API_AUTH_USERNAME: ${{ secrets.API_USERNAME }}
    API_AUTH_PASSWORD: ${{ secrets.API_PASSWORD }}
  ```
- The `.gitignore` excludes `.env` and `config-local.properties`

---

## 🧪 Test Retry

Failed tests are automatically retried once (configurable):

```bash
# Set max retries to 2
./gradlew test -Dtest.retry.max=2
```

---

## 📝 License

This project is for educational / practice purposes.

