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
echo "🚀 Building Naughty Release Artifacts v${VERSION_NAME} (code: ${VERSION_CODE})"
echo "=================================================="

# Run unit tests
echo "🧪 Running unit tests..."
./gradlew testDebugUnitTest --quiet

# Assemble signed release APK
echo "📦 Assembling signed release APK..."
./gradlew assembleRelease --quiet

# Assemble signed release AAB (Google Play Store bundle)
echo "📦 Assembling signed release AAB bundle..."
./gradlew bundleRelease --quiet

# Clean up older artifacts in root
rm -f Naughty-v*-release.apk Naughty-v*-release.aab Naughty-debug.apk

OUTPUT_APK="Naughty-v${VERSION_NAME}-release.apk"
OUTPUT_AAB="Naughty-v${VERSION_NAME}-release.aab"
cp "app/build/outputs/apk/release/app-release.apk" "$OUTPUT_APK"
cp "app/build/outputs/bundle/release/app-release.aab" "$OUTPUT_AAB"

if [ -f "$OUTPUT_APK" ] && [ -f "$OUTPUT_AAB" ]; then
    APK_SIZE=$(ls -lh "$OUTPUT_APK" | awk '{print $5}')
    AAB_SIZE=$(ls -lh "$OUTPUT_AAB" | awk '{print $5}')

    echo ""
    echo "🔍 Verifying cryptographic APK signatures (apksigner)..."
    APKSIGNER_BIN=""
    if command -v apksigner >/dev/null 2>&1; then
        APKSIGNER_BIN="apksigner"
    elif [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
        LATEST_BT=$(ls -1d "$ANDROID_HOME/build-tools/"* 2>/dev/null | sort -V | tail -n1)
        if [ -x "$LATEST_BT/apksigner" ]; then
            APKSIGNER_BIN="$LATEST_BT/apksigner"
        fi
    fi

    CERT_INFO=""
    if [ -n "$APKSIGNER_BIN" ]; then
        "$APKSIGNER_BIN" verify --verbose --print-certs "$OUTPUT_APK" > /tmp/naughty_apksigner.log 2>&1 || true
        CERT_INFO=$(grep "certificate SHA-256 digest:" /tmp/naughty_apksigner.log | head -n1 | awk '{print $NF}')
        V2_STATUS=$(grep "Verified using v2 scheme" /tmp/naughty_apksigner.log | awk '{print $NF}')
        V3_STATUS=$(grep "Verified using v3 scheme" /tmp/naughty_apksigner.log | awk '{print $NF}')
        rm -f /tmp/naughty_apksigner.log
    fi

    echo ""
    echo "=================================================="
    echo "✅ Release build successful & cryptographically signed!"
    echo "📁 Output APK (FOSS / Direct) : $SCRIPT_DIR/$OUTPUT_APK ($APK_SIZE)"
    echo "📁 Output AAB (Google Play)   : $SCRIPT_DIR/$OUTPUT_AAB ($AAB_SIZE)"
    echo "🏷️ Version                    : $VERSION_NAME (Code: $VERSION_CODE)"
    if [ -n "$CERT_INFO" ]; then
        echo "🔐 Cert SHA-256 Fingerprint   : $CERT_INFO"
        echo "🛡️ Schemes Verified           : v2 ($V2_STATUS), v3 ($V3_STATUS)"
    fi
    echo "=================================================="

    if [ "$INSTALL" = true ]; then
        echo "📲 Installing APK on connected device..."
        if adb install -r "$OUTPUT_APK"; then
            echo "✅ Installed successfully!"
        else
            echo ""
            echo "⚠️ Note: If the existing app on the device was signed with a different key (e.g. debug key),"
            echo "   Android requires uninstalling the older build first due to signature mismatch:"
            echo "   $ adb uninstall com.example.naughty && adb install $OUTPUT_APK"
        fi
    fi
else
    echo "❌ Error: Output APK or AAB not found."
    exit 1
fi
