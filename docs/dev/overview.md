# Overview

This is a (mostly) AI-generated overview of how to develop SexyTopo. This is intended for use by AI agents, but may be useful for humans as well!

## Project Overview

SexyTopo is an Android app for cave and underground surveying. It communicates with laser measuring devices over Bluetooth, builds up survey skeletons from laser measurements, and provides sketching tools for mapping cave details. The app supports 9+ different laser instruments (DistoX2, DistoX BLE, SAP5/6, Bric4/5, FCL, DiscoX, etc.) and can import/export survey data in multiple formats (Therion, SVG, Survex, PocketTopo, Compass).


## General Instructions

If these instructions conflict with something in the code, or are incomplete, update the documents.

## Build & Development Commands

### Build
```bash
./gradlew build          # Full debug build
./gradlew assembleDebug  # Build debug APK
./gradlew assembleRelease  # Build release APK
```

### Testing
```bash
./gradlew test           # Run all unit tests
./gradlew test --tests=ClassName  # Run single test class
./gradlew connectedAndroidTest  # Run instrumented tests (requires device/emulator)
```

### Linting & Code Quality
```bash
./gradlew lint           # Run Android lint checks
```

### Clean
```bash
./gradlew clean          # Clean build artifacts
```

## Architecture & Design Patterns

### High-Level Architecture
SexyTopo follows an **MVC pattern** with clear separation of concerns:

- **Communication Layer** (`comms/`): Device handling code for various cave measuring instruments.
- **Model Layer** (`model/`): Domain entities representing survey data (Survey, Leg, Station, Sketch, Area, Graph). Models use fail-fast validation in constructors and are generally immutable.
- **Control Layer** (`control/`): Activities (UI), business logic, utilities, and I/O operations.
- **I/O Layer** (`control/io/`): Format conversion for import and export formats.

### Design Patterns Used
- **Singleton**: Application state management
- **Strategy**: Pluggable Bluetooth device drivers
- **Observer**: Event-driven architecture via LocalBroadcastManager
- **Adapter**: Format converters (Therion, SVG, Survex, etc.)
- **Factory**: Device communicator creation
- **Command**: Undo/redo implementation
- **Immutable Values**: Domain models prioritize immutability

### Event-Driven Communication
Components communicate through `LocalBroadcastManager` broadcasts rather than tight coupling. Key broadcasts include:
- Connection state changes
- Survey data updates
- Instrument measurements
- Sketch modifications

## Key Modules & Responsibilities

### `comms/` - Bluetooth Device Drivers
Each laser instrument implementation follows a consistent pattern:
- **`*Communicator.java`**: Protocol-specific parsing and communication logic
- **`*BleManager.java`**: Connection lifecycle management
- Base classes: `Communicator` and `BleManager` provide common functionality
- **Adding new instruments**: Create new `*Communicator` and `*BleManager` classes, register in `InstrumentType` enum

### `model/` - Domain Model
Core entities representing survey data:
- **Survey**: Top-level container for a cave survey
- **Leg**: Individual measurement from instrument (distance, bearing, inclination)
- **Station**: Named points in the survey
- **Sketch**: Drawing data and sketching metadata
- **Graph**: Network structure for station connections

All model classes validate data at construction time (fail-fast approach).

### `control/` - UI & Business Logic
- **Activities**: Top-level UI components (SurveyActivity, etc.)
- **Utilities** (`util/`): helper functions for calculations, file operations, string manipulation
- **Services**: Background operations and Bluetooth management

### `control/io/` - Format Conversion
**Export**: Therion (primary), SVG, Survex, PocketTopo, and others
**Import**: Therion, Survex, and others

Each format is handled by dedicated import/export classes. When adding format support, extend existing import/export infrastructure.

## Code Style & Conventions
- **Java 8**: Target and source compatibility set to Java 1.8
- **Naming**: Follows standard Java conventions
- **Testing**: JUnit 4 + Mockito; avoid mocks if at all possible, especially mocks for the Android framework
- **Linting**: `abortOnError` disabled; `UnnecessaryLocalVariable` and `SameParameterValue` warnings disabled
- **Min API**: 23 (Android 6.0); Compile SDK: 36 (Android 15)
- Do not use fully qualified class names - import them properly instead
- Do not insert comments to show what has just been added - only add comments where it is helpful to inform why something is implemented that way, or to explain something that is complex
- Use British English in IDs, comments, variable names etc.
- When adding strings, remember to add translations
- When updating either sketch view (graph view), remember to consider landscape mode
- When updating either sketch view (graph view), remember to consider landscape mode

## Testing

- **Unit Tests**: Located in `app/src/test/` directory
- **Framework**: JUnit 4 + Mockito
- **Instrumented Tests**: Android-specific tests use `androidx.test` framework
- **Test Runner**: AndroidJUnitRunner

Example patterns:
- Mock Bluetooth/instrument responses for device driver tests
- Use Mockito for dependency injection in model tests
- Return default values in unit tests (`unitTests.returnDefaultValues = true`)

## Dependencies & External Tools

- **Gradle**
- **Android Gradle Plugin**
- **Firebase**: Analytics + Crashlytics for error reporting
- **BLE**: Nordic Semiconductor BLE library
- **Apache Commons**: Lang3, Text, IO for utilities
- **Testing**: JUnit, Mockito

## Useful References
- **User Manual**: app/src/main/assets/guide/index.html
- **User Guide**: [SexyTopo Guide at cavinguk.co.uk](https://www.cavinguk.co.uk/info/sexytopo.html)
- **Supported Instruments**: See README.md for full list
- **Export/Import Formats**: README.md documents supported formats and current limitations
- **GitHub Issues**: Primary bug report and feature request channel
