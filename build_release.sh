#!/usr/bin/env bash
set -euo pipefail

# Project Root Directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure JAVA_HOME is configured
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    fi
fi

# Ensure ANDROID_HOME is configured
if [ -z "${ANDROID_HOME:-}" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        export PATH="$ANDROID_HOME/platform-tools:$PATH"
    fi
fi

VERSION_FILE="version.properties"
BUMP=false
INSTALL=false

for arg in "$@"; do
    case $arg in
        --bump)
            BUMP=true
            ;;
        --install)
            INSTALL=true
            ;;
        --help|-h)
            echo "Usage: ./build_release.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --bump      Increment versionCode and patch version before building"
            echo "  --install   Install release APK on connected device/emulator after build"
            echo "  --help, -h  Show this help message"
            exit 0
            ;;
    esac
done

if [ "$BUMP" = true ]; then
    if [ -f "$VERSION_FILE" ]; then
        CURRENT_CODE=$(grep 'VERSION_CODE=' "$VERSION_FILE" | cut -d'=' -f2)
        CURRENT_NAME=$(grep 'VERSION_NAME=' "$VERSION_FILE" | cut -d'=' -f2)
        
        NEW_CODE=$((CURRENT_CODE + 1))
        
        # Increment patch version (e.g. 2.0 -> 2.1 or 2.0.1)
        MAJOR_MINOR=$(echo "$CURRENT_NAME" | cut -d'.' -f1-2)
        PATCH=$(echo "$CURRENT_NAME" | cut -s -d'.' -f3)
        if [ -n "$PATCH" ]; then
            NEW_NAME="${MAJOR_MINOR}.$((PATCH + 1))"
        else
            NEW_NAME="${CURRENT_NAME}.1"
        fi
        
        echo "VERSION_CODE=$NEW_CODE" > "$VERSION_FILE"
        echo "VERSION_NAME=$NEW_NAME" >> "$VERSION_FILE"
        echo "🔄 Bumped version: $CURRENT_NAME (code: $CURRENT_CODE) -> $NEW_NAME (code: $NEW_CODE)"
    fi
fi

VERSION_CODE=$(grep 'VERSION_CODE=' "$VERSION_FILE" | cut -d'=' -f2)
VERSION_NAME=$(grep 'VERSION_NAME=' "$VERSION_FILE" | cut -d'=' -f2)

echo "=================================================="
echo "🚀 Building Naughty Release APK v${VERSION_NAME} (code: ${VERSION_CODE})"
echo "=================================================="

# Run unit tests
echo "🧪 Running unit tests..."
./gradlew testDebugUnitTest --quiet

# Assemble signed release APK
echo "📦 Assembling signed release APK..."
./gradlew assembleRelease --quiet

OUTPUT_APK="Naughty-v${VERSION_NAME}-release.apk"
cp "app/build/outputs/apk/release/app-release.apk" "$OUTPUT_APK"

if [ -f "$OUTPUT_APK" ]; then
    APK_SIZE=$(ls -lh "$OUTPUT_APK" | awk '{print $5}')
    echo ""
    echo "=================================================="
    echo "✅ Release build successful!"
    echo "📁 Output APK : $SCRIPT_DIR/$OUTPUT_APK"
    echo "📊 File Size  : $APK_SIZE"
    echo "🏷️ Version    : $VERSION_NAME (Code: $VERSION_CODE)"
    echo "=================================================="

    if [ "$INSTALL" = true ]; then
        echo "📲 Installing on connected device..."
        adb install -r "$OUTPUT_APK"
        echo "✅ Installed successfully!"
    fi
else
    echo "❌ Error: Output APK $OUTPUT_APK not found."
    exit 1
fi
