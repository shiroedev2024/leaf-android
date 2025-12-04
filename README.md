# Leaf VPN Android Build Script

## Overview

The `build.sh` script handles building Android APKs and AABs, with optional publishing to the
release server.

## Usage

### Basic Building

```bash
# Build APKs only (default)
./build.sh
./build.sh apk

# Build AAB only
./build.sh aab

# Build both APKs and AAB
./build.sh both
```

### Publishing Releases

To upload a release to the server, use the `--publish` flag:

```bash
# Build and publish APKs
./build.sh apk --publish

# Build and publish AAB
./build.sh aab --publish
```

**Note:** Publishing both APKs and AAB in a single command is not supported. Run separate commands
for each.

## Configuration

### release.properties

The script reads configuration from `release.properties`:

```properties
# Keystore configuration
storeFile=keystore.jks
storePassword=123456
keyAlias=ang
keyPassword=123456

# API Configuration for Release Uploads
upload.api.url=https://api.yourdomain.com/api/admin/uploads/release
upload.api.key=your-release-api-key-here
upload.source=direct
```

### Environment Variables

The following environment variables are loaded from `release.properties`:

- `UPLOAD_API_URL`: The API endpoint for uploading releases
- `UPLOAD_API_KEY`: API key for authentication
- `UPLOAD_SOURCE`: Source type (default: `direct`)

## Release Notes

Release notes are now stored in the app's string resources for proper localization support. The
script automatically reads release notes from all supported languages.

### Localization Structure

Release notes are stored in `strings.xml` files for each supported language:

- `app/src/main/res/values/strings.xml` (English - default)
- `app/src/main/res/values-es/strings.xml` (Spanish)
- `app/src/main/res/values-ar/strings.xml` (Arabic)
- `app/src/main/res/values-fa/strings.xml` (Persian)
- `app/src/main/res/values-in/strings.xml` (Indonesian)
- `app/src/main/res/values-ru/strings.xml` (Russian)
- `app/src/main/res/values-tr/strings.xml` (Turkish)
- `app/src/main/res/values-uz/strings.xml` (Uzbek)

### Format

Add a `release_notes` string to each `strings.xml` file with the following format:

```xml
<string name="release_notes">• Feature update description\n• Bug fix description\n• Performance improvement</string>
```

### Example

```xml
<string name="release_notes">• Update leaf core\n• Improve connection stability\n• Add ability to bypass or block geoip/geosites\n• Fix some crash issues</string>
```

### API Integration

When `--publish` is used, the script:

1. Builds the specified artifacts (APK/AAB)
2. Reads release notes from all `strings.xml` files and converts them to multi-language changelog
   data
3. Uploads the ZIP file to the configured API endpoint
4. Sends metadata including version, platform, source, and localized changelog

The API receives changelog data as an array with entries for each language:

- `change_log[0][lang_code]=en` and `change_log[0][text]=...`
- `change_log[1][lang_code]=es` and `change_log[1][text]=...`
- etc.

## API Integration

When `--publish` is used, the script:

1. Builds the specified artifacts (APK/AAB)
2. Reads release notes from all `strings.xml` files and converts them to multi-language changelog
   data
3. Uploads the ZIP file to the configured API endpoint
4. Sends metadata including version, platform, source, and localized changelog

### API Endpoint

The script uses the following API endpoint for uploads:
`POST /api/admin/uploads/release`

### Request Parameters

- `zip`: The ZIP file containing release artifacts
- `platform`: `android`
- `version`: Version from `version.properties`
- `source`: From `release.properties` (default: `direct`)
- `change_log`: Array of changelog entries sent as form fields:
    - `change_log[0][lang_code]`: Language code (e.g., `en`)
    - `change_log[0][text]`: Changelog text content

## Examples

### Complete Release Process

```bash
# 1. Update version in version.properties if needed
echo "app.version.name=1.9.6" > version.properties

# 2. Edit release notes
nano app/src/main/res/values/strings.xml

# 3. Build and publish
./build.sh apk --publish
```

### Testing Without Publishing

```bash
# Build only (no upload)
./build.sh apk

# Verify the ZIP file was created
ls -la release/leaf-vpn-android_*.zip
```

## Error Handling

The script will exit with error codes for various failure conditions:

- Missing APK files during build
- Failed ZIP creation
- Missing API configuration
- Upload failures (non-200 HTTP responses)

## Security Notes

- Store API keys securely in `release.properties`
- Never commit `release.properties` with real API keys to version control
- Use environment-specific configurations for different deployment environments