#!/bin/bash

# Script to check for dependency updates by parsing Android Studio's lint results
# This leverages Android Studio's own dependency checking logic

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Checking for dependency updates using Android Studio's lint results...${NC}"
echo ""

# Function to find project root
find_project_root() {
    local current_dir="$(pwd)"
    local search_dir="$current_dir"
    
    # Search up the directory tree for gradle/libs.versions.toml
    while [ "$search_dir" != "/" ]; do
        if [ -f "$search_dir/gradle/libs.versions.toml" ] && [ -f "$search_dir/gradlew" ]; then
            echo "$search_dir"
            return 0
        fi
        search_dir="$(dirname "$search_dir")"
    done
    
    return 1
}

# Find the project root
PROJECT_ROOT=$(find_project_root)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error: Could not find project root with gradle/libs.versions.toml${NC}"
    echo "Make sure you're running this script from within the project directory tree"
    exit 1
fi

echo -e "${GREEN}✅ Found project root: $PROJECT_ROOT${NC}"
echo ""

# Change to project root for all operations
cd "$PROJECT_ROOT"

# Function to run lint and parse results
check_dependency_updates() {
    echo -e "${BLUE}🔄 Running Android lint to check for dependency updates...${NC}"
    echo -e "${CYAN}   (This may take a moment as it checks with remote repositories)${NC}"
    echo ""
    
    # Run lint (suppress most output, but allow errors)
    ./gradlew lint --quiet 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Lint completed with some issues (this is normal)${NC}"
        echo ""
    }
    
    # Find lint result files
    local lint_files=$(find . -name "lint-results*.xml" | grep -v build/intermediates | head -5)
    
    if [ -z "$lint_files" ]; then
        echo -e "${RED}❌ No lint results found. Try running './gradlew lint' manually first.${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Lint completed. Analyzing results...${NC}"
    echo ""
    
    local updates_found=false
    local gradle_updates_found=false
    
    # Parse lint results for dependency updates
    for lint_file in $lint_files; do
        # Check for GradleDependency issues (Android Studio's dependency checker)
        local gradle_deps=$(grep -A 5 'id="GradleDependency"' "$lint_file" | grep 'message=' | sed 's/.*message="\([^"]*\)".*/\1/')
        
        if [ -n "$gradle_deps" ]; then
            if [ "$updates_found" = false ]; then
                echo -e "${YELLOW}📦 Dependency Updates Available (detected by Android Studio's linter):${NC}"
                echo ""
                updates_found=true
            fi
            
            echo "$gradle_deps" | while IFS= read -r message; do
                if [ -n "$message" ]; then
                    echo -e "  ${YELLOW}📦 $message${NC}"
                fi
            done
        fi
        
        # Check for AndroidGradlePluginVersion issues
        local gradle_plugin_deps=$(grep -A 5 'id="AndroidGradlePluginVersion"' "$lint_file" | grep 'message=' | sed 's/.*message="\([^"]*\)".*/\1/')
        
        if [ -n "$gradle_plugin_deps" ]; then
            if [ "$gradle_updates_found" = false ]; then
                echo ""
                echo -e "${YELLOW}🔧 Gradle Updates Available:${NC}"
                echo ""
                gradle_updates_found=true
            fi
            
            echo "$gradle_plugin_deps" | while IFS= read -r message; do
                if [ -n "$message" ]; then
                    echo -e "  ${YELLOW}🔧 $message${NC}"
                fi
            done
        fi
        
        # Check for NewerVersionAvailable issues (more comprehensive but slower)
        local newer_versions=$(grep -A 5 'id="NewerVersionAvailable"' "$lint_file" | grep 'message=' | sed 's/.*message="\([^"]*\)".*/\1/' | sort -u)
        
        if [ -n "$newer_versions" ]; then
            if [ "$updates_found" = false ]; then
                echo -e "${YELLOW}📦 Additional Updates Available (comprehensive check):${NC}"
                echo ""
                updates_found=true
            fi
            
            echo "$newer_versions" | while IFS= read -r message; do
                if [ -n "$message" ] && [ "$message" != "A newer version of Gradle than"* ]; then
                    echo -e "  ${CYAN}🔍 $message${NC}"
                fi
            done
        fi
    done
    
    if [ "$updates_found" = false ] && [ "$gradle_updates_found" = false ]; then
        echo -e "${GREEN}✅ No dependency updates found by Android Studio's linter!${NC}"
        echo -e "${GREEN}   All dependencies appear to be up to date.${NC}"
    fi
    
    echo ""
    
    # Check for "latest.release" versions (not recommended for production)
    local latest_releases=$(grep -n "latest.release" gradle/libs.versions.toml || true)
    if [ -n "$latest_releases" ]; then
        echo -e "${YELLOW}⚠️  Found 'latest.release' versions (consider pinning to specific versions):${NC}"
        echo "$latest_releases" | sed 's/^/  /'
        echo ""
    fi
}

# Main execution
check_dependency_updates

echo -e "${GREEN}✅ Dependency check completed${NC}"
echo ""
echo -e "${BLUE}📝 How to Update Dependencies:${NC}"
echo "1. Review the updates listed above"
echo "2. Update specific versions in gradle/libs.versions.toml"
echo "3. Test your application thoroughly after updates"
echo "4. Run './gradlew build' to ensure everything compiles"
echo ""
echo -e "${BLUE}💡 About This Script:${NC}"
echo "• This script uses Android Studio's own lint dependency checker"
echo "• It parses the same results that Android Studio shows in the IDE"
echo "• Updates are detected by Android's official dependency analysis"
echo "• Run './gradlew lint' manually to refresh the analysis"
echo ""
echo -e "${BLUE}📚 Useful resources:${NC}"
echo "• Android Jetpack releases: https://developer.android.com/jetpack/androidx/versions"
echo "• Kotlin releases: https://github.com/JetBrains/kotlin/releases"
echo "• Maven Central: https://search.maven.org/"
