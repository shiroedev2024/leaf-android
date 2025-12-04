# Leaf VPN Android

A demonstration application showcasing how to integrate the Leaf VPN SDK into an Android
application.

## Overview

This application serves as a reference implementation for developers looking to integrate the Leaf
VPN SDK into their Android applications. It demonstrates core VPN functionality, user interface
design patterns, and best practices for VPN application development.

## Features

- Connect and disconnect VPN service
- Switch between multiple outbound connections
- Custom VPN configuration settings
- Memory logging and diagnostics
- Tile Service for quick VPN access from Quick Settings
- Auto-profile support for one-click configuration
- Multi-language support with 8 localized languages
- Update management with versioned releases

## Project Structure

### Core Components

- **Activities**: Main application entry points and UI containers
    - `MainActivity`: Primary application interface
    - `AutoProfileActivity`: Handles deep links for automatic profile configuration
    - `MemoryLoggerActivity`: Diagnostic tool for monitoring logs

- **ViewModels**: Business logic and state management
    - `LeafViewModel`: Manages VPN service state, connections, and settings
    - `UpdateViewModel`: Handles application update checking and notifications
    - `AutoProfileViewModel`: Manages automatic profile configuration

- **Screens**: Compose UI implementation
    - `MainScreen`: Root screen that hosts navigation
    - `DashboardScreen`: Primary VPN control screen
    - `SettingsScreen`: Configuration options
    - `ProfileScreen`: Profile selection and management
    - `MemoryLogger`: Log viewing interface

- **Services**:
    - `LeafVPNTileService`: Quick Settings tile for VPN control

## Building and Running

The project uses Gradle for building and includes a convenient `build.sh` script for creating
release packages.

### Building APKs

```bash
./build.sh apk
```

### Building App Bundle (AAB)

```bash
./build.sh aab
```

### Publishing Releases

```bash
./build.sh apk --publish
```

## Configuration

The application uses several configuration files:

- `version.properties`: Controls application version information
- `release.properties`: Contains signing and release server configuration
- `strings.xml`: Contains localized strings and release notes

## Multi-language Support

The application supports the following languages:

- English (default)
- Spanish
- Arabic
- Persian
- Indonesian
- Russian
- Turkish
- Uzbek

## License

See the [LICENSE](LICENSE) file for details.