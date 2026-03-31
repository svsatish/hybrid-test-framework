#!/bin/bash

# ============================================================
# run-tests.sh - Run tests and generate Allure reports
#
# Usage:
#   ./run-tests.sh                        (default env, generate report)
#   ./run-tests.sh --env staging          (run against staging)
#   ./run-tests.sh --env prod --serve     (run against prod + open report)
#   ./run-tests.sh --help
# ============================================================

set -e

ALLURE_RESULTS_DIR="build/allure-results"
ALLURE_REPORT_DIR="build/allure-report"
ENV=""
SERVE=false

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ----------------------------------------------------------
# Parse arguments
# ----------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case $1 in
        --env)
            ENV="$2"
            shift 2
            ;;
        --serve)
            SERVE=true
            shift
            ;;
        --help|-h)
            echo "Usage: ./run-tests.sh [--env qa|staging|prod] [--serve]"
            echo ""
            echo "Options:"
            echo "  --env <name>   Target environment (loads config-<name>.properties)"
            echo "                 Available: qa, staging, prod"
            echo "  --serve        Open Allure report in browser after generation"
            echo "  --help         Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Run './run-tests.sh --help' for usage."
            exit 1
            ;;
    esac
done

# Build Gradle flags
GRADLE_PROPS=""
if [ -n "$ENV" ]; then
    GRADLE_PROPS="-Denv=${ENV}"
fi

echo -e "${YELLOW}======================================${NC}"
echo -e "${YELLOW}  Hybrid Test Framework - Test Runner  ${NC}"
echo -e "${YELLOW}======================================${NC}"
if [ -n "$ENV" ]; then
    echo -e "  Environment: ${GREEN}${ENV}${NC}"
else
    echo -e "  Environment: ${GREEN}default${NC}"
fi

# ----------------------------------------------------------
# Step 1: Clean previous results
# ----------------------------------------------------------
echo -e "\n${GREEN}[Step 1]${NC} Cleaning previous build and allure results..."
./gradlew clean

# ----------------------------------------------------------
# Step 2: Run tests
# ----------------------------------------------------------
echo -e "\n${GREEN}[Step 2]${NC} Running tests..."
./gradlew test --continue ${GRADLE_PROPS} || true
# --continue ensures all tests run even if some fail
# || true prevents the script from exiting on test failures

# ----------------------------------------------------------
# Step 3: Verify allure results exist
# ----------------------------------------------------------
if [ ! -d "$ALLURE_RESULTS_DIR" ] || [ -z "$(ls -A "$ALLURE_RESULTS_DIR" 2>/dev/null)" ]; then
    echo -e "\n${RED}[Error]${NC} No Allure results found in ${ALLURE_RESULTS_DIR}."
    echo "Tests may not have produced any results."
    exit 1
fi

echo -e "\n${GREEN}[Step 3]${NC} Allure results found in ${ALLURE_RESULTS_DIR}"

# ----------------------------------------------------------
# Step 4: Check if Allure CLI is installed
# ----------------------------------------------------------
if ! command -v allure &> /dev/null; then
    echo -e "\n${YELLOW}[Warning]${NC} Allure CLI is not installed."
    echo "Install it using one of:"
    echo "  brew install allure     (macOS)"
    echo "  npm install -g allure-commandline"
    echo ""
    echo "Allure results are available at: ${ALLURE_RESULTS_DIR}"
    exit 0
fi

# ----------------------------------------------------------
# Step 5: Generate Allure report
# ----------------------------------------------------------
echo -e "\n${GREEN}[Step 4]${NC} Generating Allure report..."
allure generate "$ALLURE_RESULTS_DIR" -o "$ALLURE_REPORT_DIR" --clean

echo -e "\n${GREEN}[Step 5]${NC} Allure report generated at: ${ALLURE_REPORT_DIR}"

# ----------------------------------------------------------
# Step 6: Open report in browser (optional)
# ----------------------------------------------------------
if [ "$SERVE" = true ]; then
    echo -e "\n${GREEN}[Step 6]${NC} Opening Allure report in browser..."
    allure open "$ALLURE_REPORT_DIR"
else
    echo -e "\nTo view the report in browser, run:"
    echo "  allure open ${ALLURE_REPORT_DIR}"
    echo "  OR re-run this script with: ./run-tests.sh --serve"
fi

echo -e "\n${GREEN}Done!${NC}"

