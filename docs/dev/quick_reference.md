# SexyTopo Quick Reference Guide

## Project at a Glance

**What is it?** Android cave surveying app that talks to laser devices over Bluetooth

**Language:** Java (167 files) + minimal Kotlin (2 files) = ~23.5k LOC

**Min/Target/Compile SDK:** 23 / 34 / 36 (Android 6.0 to 15)

**Key Libraries:** 
- AndroidX, Firebase (analytics + crashlytics), Nordic BLE library, Apache Commons

---

## Directory Quick Map

```
comms/          Bluetooth device drivers (DistoX, SAP5, Bric4, FCL, etc.)
model/          Data models (Survey, Leg, Station, Sketch)
control/        Activities & business logic
  ├── activity/  UI screens (Table, Plan, Elevation, Device, etc.)
  ├── io/        File I/O and format converters
  └── util/      Geometry, stats, naming utilities (1,960 LOC)
testutils/      Test data generators
res/            Android resources (strings, layouts, images)
```

---

## Key Classes to Know

| Class | Purpose |
|-------|---------|
| `Survey` | Main data container (active station, legs, sketches, undo stack) |
| `Leg` | Immutable measurement (distance, azimuth, inclination) |
| `Station` | Named point in cave |
| `Sketch` | Drawing canvas (plan/elevation) with path/symbol/text details |
| `SurveyManager` | Singleton holding current survey, manages autosave |
| `InstrumentType` | Enum mapping device names to Communicator classes |
| `Communicator` | Interface for device-specific communication |
| `SurveyUpdater` | Core logic: applies legs to survey |
| `Exporter` | Base class for export formats (Therion, SVG, Survex, etc.) |
| `SexyTopoActivity` | Base activity class (permissions, menu, file ops) |

---

## Architecture Patterns Used

1. **Singleton** - SurveyManager, SexyTopo app
2. **Strategy** - Device communicators via Communicator interface
3. **Observer** - LocalBroadcastManager for model change events
4. **Adapter** - Format importers/exporters
5. **Factory** - InstrumentType creates device-specific communicators
6. **Command** - SurveyUpdater.update() applies measurements
7. **Immutable Values** - Leg, Station, Coord2D/3D

---

## Data Flow at a Glance

### Receiving Measurement
```
BluetoothDevice 
  → DistoXCommunicator.parse() 
  → SurveyManager.updateSurvey(Leg)
  → SurveyUpdater.update(Survey, Leg)
  → NEW_STATION_CREATED_EVENT broadcast
  → Activities update UI
```

### Saving Survey
```
User clicks Save
  → Saver.save(Survey)
  → SurveyJsonTranslater converts to JSON
  → DocumentFile.create(data.json, plan.json, metadata.json)
```

### Exporting Survey
```
User selects format
  → Format-specific Exporter.run(Survey)
  → File written to export subdirectory
```

---

## Build Commands Cheat Sheet

```bash
# Build
./gradlew build                  # Full build with tests
./gradlew assembleDebug          # Debug APK only
./gradlew clean build            # Clean rebuild

# Test
./gradlew test                   # Run all unit tests
./gradlew test --info            # Show detailed output
./gradlew test --tests "Class"   # Specific test class

# Development
./gradlew installDebug           # Build and install on device
```

---

## Testing Quick Facts

- **38 test files** with ~2,588 lines
- **~11% test coverage** by line count
- **JUnit 4 + Mockito** framework
- Tests cover: model, comms protocols, geometry utilities, exporters
- **test/** for JVM tests, **androidTest/** for device tests

**Test Utilities:**
- `BasicTestSurveyCreator.createStraightNorth()` - Simple test survey
- `BasicTestSurveyCreator.createRightRight()` - L-shaped survey

---

## Supported Devices

1. DistoX2 (Bluetooth Classic)
2. DistoX BLE
3. Shetland Attack Pony 5 & 6
4. Bric4 & Bric5
5. Friken Cave Laser (FCL)
6. DiscoX
7. CavwayX1 BLE
8. Manual/analogue input

---

## Supported Formats

**Export:** Therion (primary), SVG, Survex, PocketTopo, Compass  
**Import:** Therion, Survex, PocketTopo (support limited)

---

## Extension Points

**Add New Device:**
1. Create `NewDeviceCommunicator extends Communicator`
2. Add enum to `InstrumentType`
3. Implement protocol classes

**Add New Export Format:**
1. Create `NewFormatExporter extends Exporter`
2. Implement `run()` method
3. Add to exporter list

**Add New Projection:**
1. Create projection class
2. Extend `Projection2D`
3. Add to activity menu

---

## Important Files

| File | Purpose |
|------|---------|
| `SexyTopoConstants.java` | App constants (event names, file extensions, UUIDs) |
| `SurveyManager.java` | Central survey state management |
| `SurveyUpdater.java` | Core measurement application logic |
| `InstrumentType.java` | Device type to communicator mapping |
| `SexyTopoActivity.java` | Base class for all activities |
| `build.gradle` | Gradle configuration (SDK, dependencies) |
| `AndroidManifest.xml` | App permissions and activities |

---

## Permissions Required

- Bluetooth: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`
- Location: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (required by Android for BLE)
- Storage: `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`
- Other: `INTERNET` (Firebase), `VIBRATE`

---

## Configuration & Gradle

**SDK Versions:**
- Min: 23 (Android 6.0)
- Target: 34 (Android 14)
- Compile: 36 (Android 15 preview)

**Build Features:**
- Minification: Disabled
- Jetifier: Enabled
- AndroidX: Required

**Important Properties:**
```gradle
android.useAndroidX=true
android.enableJetifier=true
org.gradle.unsafe.configuration-cache=true
```

---

## File Structure for Survey Data

```
Survey Directory/
  ├── data.json              # Core survey (stations, legs)
  ├── plan.json              # Plan view sketch
  ├── ext-elevation.json     # Extended elevation sketch
  ├── metadata.json          # Trip info
  ├── autosave               # Autosave backup
  ├── log                    # Device communication log
  └── export/                # Export subdirectories
      ├── therion/
      ├── svg/
      └── [other formats]
```

---

## Event Constants (for listeners)

From `SexyTopoConstants.java`:
- `SURVEY_UPDATED_EVENT` - Survey data changed
- `NEW_STATION_CREATED_EVENT` - Station added
- `SYSTEM_LOG_UPDATED_EVENT` - System log changed
- `DEVICE_LOG_UPDATED_EVENT` - Device log changed
- `CALIBRATION_UPDATED_EVENT` - Calibration changed

Listen via `LocalBroadcastManager.registerReceiver()`

---

## Code Conventions

**Package Structure:**
- `org.hwyl.sexytopo.comms.*` - Device drivers
- `org.hwyl.sexytopo.model.*` - Data models
- `org.hwyl.sexytopo.control.*` - Logic & UI
- `org.hwyl.sexytopo.control.util` - Utilities

**Naming:**
- Interfaces: `Communicator`, `Exporter`
- Implementations: `DistoXCommunicator`, `TherionExporter`
- Enums: `InstrumentType`, `BrushColour`
- Tests: `*Test` suffix
- Constants: `CONSTANT_NAME` (all caps)

**Methods:**
- Getters: `get*()`, `is*()`
- Setters: `set*()`
- Creators: `create*()`, `build*()`

---

## Recent Features (Last Commits)

- DiscoX device support
- FCL (Friken Cave Laser) Enhanced BLE Protocol v2.0
- Android 6.0 compatibility improvements
- Reverse move to different station
- Various bug fixes (leg operations, naming, etc.)

---

## Tips for Contributors

1. **Model classes** should be immutable or nearly immutable
2. **Validations** happen in model constructors
3. **UI updates** happen via event broadcasts, not direct calls
4. **Tests** go in mirrored directory structure under `app/src/test/`
5. **Comments** sparingly - code should be self-documenting
6. **Exceptions** - use `IllegalArgumentException` for invalid params
7. **Singletons** - used for shared state (SurveyManager, SexyTopo)

---

## Known Limitations

- ProGuard minification disabled (for debugging)
- Some format importers are experimental/unreliable
- Android requires location permission for Bluetooth scanning
- Large surveys may have performance issues with 3D-to-2D projection
- No native code (pure Java/Kotlin)

---

## License

GNU General Public License v3 - Free and open source

---

## Useful Links

- **Repository:** https://github.com/richsmith/sexytopo
- **Play Store:** https://play.google.com/store/apps/details?id=org.hwyl.sexytopo
- **F-Droid:** https://f-droid.org/en/packages/org.hwyl.sexytopo/
- **User Guide:** https://www.cavinguk.co.uk/info/sexytopo.html
- **BCRA:** https://bcra.org.uk (British Cave Research Association)

---

Generated from codebase analysis - November 2024
