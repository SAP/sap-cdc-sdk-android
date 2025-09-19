#!/bin/bash

# Validation script for JReleaser configuration
# This script validates the release configuration without performing an actual release

set -e

echo "🔍 Validating JReleaser configuration for SAP CDC Android SDK..."
echo "=================================================="

# Function to find project root by looking for specific files
find_project_root() {
    local current_dir="$(pwd)"
    local search_dir="$current_dir"
    
    # Look for the project root by finding library/jreleaser.yml
    while [ "$search_dir" != "/" ]; do
        if [ -f "$search_dir/library/jreleaser.yml" ] && [ -f "$search_dir/gradlew" ]; then
            echo "$search_dir"
            return 0
        fi
        search_dir="$(dirname "$search_dir")"
    done
    
    return 1
}

# Find and navigate to project root
echo "🔍 Detecting project root..."
if PROJECT_ROOT=$(find_project_root); then
    echo "✅ Found project root at: $PROJECT_ROOT"
    cd "$PROJECT_ROOT"
    echo "📂 Changed to project root directory"
else
    echo "❌ Error: Could not find project root. Looking for library/jreleaser.yml and gradlew files."
    echo "   Make sure you're running this script within the SAP CDC Android SDK project."
    exit 1
fi

echo "✅ Found JReleaser configuration file"

# Check if required environment variables are set (for actual release)
echo ""
echo "🔐 Checking environment variables (for actual release):"
echo "Note: These are not required for validation, but will be needed for actual release"

check_env_var() {
    local var_name=$1
    if [ -z "${!var_name}" ]; then
        echo "⚠️  $var_name is not set"
    else
        echo "✅ $var_name is set"
    fi
}

check_env_var "SONATYPE_USERNAME"
check_env_var "SONATYPE_PASSWORD"
check_env_var "GITHUB_TOKEN"

# Validate JReleaser configuration
echo ""
echo "🔧 Validating JReleaser configuration..."

cd library

# Check JReleaser configuration syntax
echo "Checking JReleaser configuration syntax..."
if command -v ./gradlew &> /dev/null; then
    ../gradlew jreleaserConfig --dry-run || echo "⚠️  JReleaser config validation failed (this might be expected without proper credentials)"
else
    echo "⚠️  Gradle wrapper not found, skipping JReleaser config validation"
fi

# Validate that required files exist
echo ""
echo "📁 Checking required files..."

required_files=(
    "build.gradle.kts"
    "jreleaser.yml"
    "src/main/AndroidManifest.xml"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file is missing"
    fi
done

# Check Gradle build
echo ""
echo "🏗️  Testing Gradle build..."
cd ..
if ./gradlew :library:assembleRelease --dry-run; then
    echo "✅ Gradle build configuration is valid"
else
    echo "❌ Gradle build configuration has issues"
fi

# Test documentation generation
echo ""
echo "📚 Testing documentation generation..."
# Try the correct Dokka task name for newer versions
if ./gradlew :library:dokkaGeneratePublicationHtml --dry-run; then
    echo "✅ Documentation generation configuration is valid"
elif ./gradlew :library:dokkaHtml --dry-run; then
    echo "✅ Documentation generation configuration is valid"
else
    echo "❌ Documentation generation configuration has issues"
fi

# Run code quality checks and all tests
echo ""
echo "🔍 Running code quality checks..."

echo "Running lint checks for all modules..."
if ./gradlew lint; then
    echo "✅ Lint checks passed"
else
    echo "⚠️  Lint checks failed - please review and fix lint issues"
    echo "   Note: Continuing with other validations, but lint issues should be addressed before release"
fi

echo ""
echo "🧪 Running all tests..."

echo "Running unit tests for library module..."
if ./gradlew :library:test; then
    echo "✅ Library unit tests passed"
else
    echo "❌ Library unit tests failed"
    exit 1
fi

echo "Running unit tests for app module..."
if ./gradlew :app:test; then
    echo "✅ App unit tests passed"
else
    echo "❌ App unit tests failed"
    exit 1
fi

echo "Running instrumented tests for library module..."
if ./gradlew :library:connectedAndroidTest; then
    echo "✅ Library instrumented tests passed"
else
    echo "⚠️  Library instrumented tests failed or no device/emulator available"
    echo "   Note: Instrumented tests require a connected Android device or emulator"
fi

echo "Running instrumented tests for app module..."
if ./gradlew :app:connectedAndroidTest; then
    echo "✅ App instrumented tests passed"
else
    echo "⚠️  App instrumented tests failed or no device/emulator available"
    echo "   Note: Instrumented tests require a connected Android device or emulator"
fi

echo "Running comprehensive verification checks..."
if ./gradlew check; then
    echo "✅ All verification checks passed"
else
    echo "⚠️  Some verification checks failed (may include lint issues mentioned above)"
    echo "   Note: Please review and address any failing checks before release"
fi

# Test JReleaser tasks
echo ""
echo "📦 Testing JReleaser tasks..."
if ./gradlew :library:jreleaserConfig --dry-run; then
    echo "✅ JReleaser configuration is valid"
else
    echo "⚠️  JReleaser configuration validation failed (expected without credentials)"
fi

echo ""
echo "=================================================="
echo "🎉 Validation complete!"
echo ""
echo "📋 Summary:"
echo "- Group ID: com.sap.oss.cdc-android-sdk"
echo "- Artifact ID: cdc-android-sdk"
echo "- Version: 0.3.0"
echo "- Repository: https://github.com/SAP/sap-cdc-sdk-android"
echo ""
echo "🚀 To perform an actual release:"
echo "1. Set up required environment variables (SONATYPE_USERNAME, SONATYPE_PASSWORD, GITHUB_TOKEN)"
echo "2. Configure GPG signing"
echo "3. Run: ./gradlew :library:jreleaserFullRelease"
echo ""
echo "⚠️  Remember: This validation doesn't guarantee a successful release."
echo "   Always test in a staging environment first!"
