#!/bin/bash

# Usage: ./build.sh [apk|aab|both] [--publish]
# Default when no argument is provided: apk
# Use --publish to upload the release to the server

RELEASE_DIR="release"

# Get the version from the version.properties file
VERSION=$(grep "app.version.name" ./version.properties | cut -d'=' -f2)

# Load configuration from release.properties
load_config() {
    if [ -f "release.properties" ]; then
        while IFS='=' read -r key value; do
            # Skip comments and empty lines
            [[ $key =~ ^[[:space:]]*# ]] && continue
            [[ -z $key ]] && continue

            # Remove surrounding whitespace
            key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

            # Export as environment variable (convert dots to underscores)
            env_key=$(echo "$key" | tr '.' '_' | tr '[:lower:]' '[:upper:]')
            export "$env_key"="$value"
        done < "release.properties"
    else
        echo "Warning: release.properties not found"
    fi
}

# Load configuration
load_config

# Parse arguments
PUBLISH=false
BUILD_MODE="apk"

while [[ $# -gt 0 ]]; do
    case $1 in
        --publish)
            PUBLISH=true
            shift
            ;;
        apk|aab|both)
            BUILD_MODE="$1"
            shift
            ;;
        *)
            echo "Usage: $0 [apk|aab|both] [--publish]"
            exit 1
            ;;
    esac
done

# Ensure the release directory exists
mkdir -p "$RELEASE_DIR"

build_apks() {
  echo "Building APKs (with APK splits enabled)..."
  ./gradlew :app:assembleRelease -PapkSplits=true

  ZIP_FILE="$RELEASE_DIR/leaf-vpn-android_${VERSION}.zip"

  TARGETS=(
    "arm64-v8a"
    "x86_64"
    "all"
    "x86"
    "armeabi-v7a"
  )

  APK_FILES=()

  for TARGET in "${TARGETS[@]}"; do
    APK_FILE="./app/build/outputs/apk/release/leaf_vpn_${VERSION}_${TARGET}.apk"
    if [ -f "$APK_FILE" ]; then
      APK_FILES+=("$APK_FILE")
    else
      echo "Warning: File $APK_FILE does not exist."
    fi
  done

  if [ ${#APK_FILES[@]} -eq 0 ]; then
    echo "Error: No APK files found to zip."
    exit 1
  fi

  echo "Zipping APKs..."
  if zip -j "$ZIP_FILE" "${APK_FILES[@]}"; then
    echo "Successfully created $ZIP_FILE"
  else
    echo "Error: Failed to create $ZIP_FILE"
    exit 1
  fi
}

build_aab() {
  echo "Building Android App Bundle (AAB)..."
  ./gradlew :app:bundleRelease

  AAB_SRC="./app/build/outputs/bundle/release/app-release.aab"
  AAB_ZIP="$RELEASE_DIR/leaf-vpn-android_${VERSION}_aab.zip"

  if [ -f "$AAB_SRC" ]; then
    echo "Zipping AAB..."
    if zip -j "$AAB_ZIP" "$AAB_SRC"; then
      echo "Successfully created $AAB_ZIP"
    else
      echo "Error: Failed to zip AAB to $AAB_ZIP"
      exit 1
    fi
  else
    echo "Error: AAB not found at $AAB_SRC"
    exit 1
  fi
}

prepare_changelog() {
    # Array to store changelog entries
    CHANGELOG_ENTRIES=()
    INDEX=0
    
    # Find all values directories (including values and values-* for different languages)
    VALUES_DIRS=$(find app/src/main/res -name "values*" -type d | sort)
    
    for DIR in $VALUES_DIRS; do
        STRINGS_FILE="$DIR/strings.xml"
        
        if [ -f "$STRINGS_FILE" ]; then
            # Extract language code from directory name
            if [[ "$DIR" == "app/src/main/res/values" ]]; then
                LANG_CODE="en"
            else
                # Extract language code from values-XX format
                LANG_CODE=$(echo "$DIR" | sed 's|.*/values-\([a-zA-Z_-]*\)|\1|' | sed 's|-r.*||')
                # Handle special cases
                case "$LANG_CODE" in
                    "in") LANG_CODE="id" ;;  # Indonesian
                    "uz") LANG_CODE="uz" ;;  # Uzbek
                    *) LANG_CODE="$LANG_CODE" ;;
                esac
            fi
            
            # Extract release_notes string value from XML
            RELEASE_NOTES=$(grep -oP '(?<=name="release_notes">)[^<]+' "$STRINGS_FILE" 2>/dev/null || echo "")
            
            if [ -n "$RELEASE_NOTES" ]; then
                # Escape single quotes for curl
                ESCAPED_NOTES=$(echo "$RELEASE_NOTES" | sed "s/'/'\\\\''/g")
                CHANGELOG_ENTRIES+=("$LANG_CODE:$ESCAPED_NOTES")
            fi
        fi
    done
    
    # Output the changelog entries for curl command construction
    for ENTRY in "${CHANGELOG_ENTRIES[@]}"; do
        LANG_CODE=$(echo "$ENTRY" | cut -d: -f1)
        CONTENT=$(echo "$ENTRY" | cut -d: -f2-)
        printf ' -F "change_log[%d][lang_code]=%s" -F "change_log[%d][text]=%s"' "$INDEX" "$LANG_CODE" "$INDEX" "$CONTENT"
        ((INDEX++))
    done
}

upload_release() {
    local zip_file="$1"

    if [ ! -f "$zip_file" ]; then
        echo "Error: ZIP file $zip_file not found for upload"
        return 1
    fi

    # Check if required environment variables are set
    if [ -z "${UPLOAD_API_URL}" ] || [ -z "${UPLOAD_API_KEY}" ]; then
        echo "Error: UPLOAD_API_URL and UPLOAD_API_KEY must be set in release.properties"
        return 1
    fi

    echo "Uploading release to server..."
    echo "API URL: ${UPLOAD_API_URL}"
    echo "Version: ${VERSION}"
    echo "Platform: android"

    # Prepare changelog
    CHANGELOG_CONTENT=$(prepare_changelog)

    # Build curl command with change_log as array fields
    if [ -n "$CHANGELOG_CONTENT" ]; then
        # Use eval to properly execute the curl command with dynamic arguments
        response=$(eval "curl -s -w '\n%{http_code}' -X POST '${UPLOAD_API_URL}' \
            -H 'X-API-Key: ${UPLOAD_API_KEY}' \
            -F 'zip=@${zip_file}' \
            -F 'platform=android' \
            -F 'version=${VERSION}' \
            -F 'source=${UPLOAD_SOURCE:-direct}' \
            $CHANGELOG_CONTENT")
    else
        # Upload without changelog
        response=$(curl -s -w "\n%{http_code}" -X POST "${UPLOAD_API_URL}" \
            -H "X-API-Key: ${UPLOAD_API_KEY}" \
            -F "zip=@${zip_file}" \
            -F "platform=android" \
            -F "version=${VERSION}" \
            -F "source=${UPLOAD_SOURCE:-direct}")
    fi

    # Extract status code and response body
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" -eq 200 ]; then
        echo "Upload successful!"
        echo "Response: $body"
        return 0
    else
        echo "Upload failed with HTTP $http_code"
        echo "Response: $body"
        return 1
    fi
}

# Main logic: choose build mode
case "$BUILD_MODE" in
  apk)
    build_apks
    if [ "$PUBLISH" = true ]; then
        ZIP_FILE="$RELEASE_DIR/leaf-vpn-android_${VERSION}.zip"
        upload_release "$ZIP_FILE"
    fi
    ;;
  aab)
    build_aab
    if [ "$PUBLISH" = true ]; then
        AAB_ZIP="$RELEASE_DIR/leaf-vpn-android_${VERSION}_aab.zip"
        upload_release "$AAB_ZIP"
    fi
    ;;
  both)
    build_apks
    build_aab
    if [ "$PUBLISH" = true ]; then
        echo "Warning: Publishing both APK and AAB not supported in single command."
        echo "Please run separate commands for each build type."
    fi
    ;;
  *)
    echo "Usage: $0 [apk|aab|both] [--publish]"
    exit 1
    ;;
esac

if [ "$PUBLISH" = true ]; then
    echo "Release process completed with publishing."
else
    echo "Build process completed. Use --publish to upload to server."
fi