# Build Documentation

## Local Development Build

### Prerequisites
- Java JDK 17 or higher
- Android Studio (latest version recommended)
- Android SDK 36 with Build Tools 36.0.0
- Gradle 8.12.2 or higher

### Build Steps

#### Option 1: Using Android Studio
1. Open Android Studio
2. File → Open → Select project directory
3. Wait for Gradle sync to complete
4. Select device/emulator
5. Click **Run** (green play button) or Build → Build Bundle(s)/APK(s) → Build APK(s)

#### Option 2: Command Line

**Debug APK:**
```bash
./gradlew assembleDebug
```

**Release APK (unsigned):**
```bash
./gradlew assembleRelease
```

**Build with logging:**
```bash
./gradlew assembleDebug --info
```

### Output Locations
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Bundle: `app/build/outputs/bundle/release/app-release.aab`

## Signing Release APK

### Generate Keystore (One-time)
```bash
keytool -genkey -v -keystore skripzy-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias skripzy_key
```

### Create keystore.properties
```properties
storeFile=skripzy-release.jks
storePassword=your_store_password
keyAlias=skripzy_key
keyPassword=your_key_password
```

### Build Signed APK
```bash
./gradlew assembleRelease
```

The keystore configuration will be automatically read from `keystore.properties`.

## GitHub Actions CI/CD

See [CI_CD.md](CI_CD.md) for complete GitHub Actions setup.

### Free Automated Builds
- ✅ Automatic debug APK builds
- ✅ Automatic release APK builds (unsigned)
- ✅ Artifact storage for 7 days
- ✅ No build limits for public repos
- ✅ Free for private repos (2,000 min/month)

### Trigger Builds
- Push to `main` or `develop` branches
- Manual trigger via Actions tab
- Pull requests to `main` branch

## Build Configuration

### Key Files
- `build.gradle.kts` - Root build configuration
- `app/build.gradle.kts` - App-level configuration
- `gradle.properties` - Gradle settings
- `gradle/libs.versions.toml` - Dependency versions
- `keystore.properties` - Signing configuration (if signing)

### Important Settings

**Target SDK:** 36 (Android 16)
**Min SDK:** 23 (Android 6.0)
**Kotlin:** 2.2.10
**Compose BOM:** 2025.08.01

### Build Features
- **Compose**: Enabled for UI
- **ProGuard**: Enabled for release builds
- **Resource Shrinking**: Enabled for release builds
- **Build Config**: Generated automatically

## Troubleshooting

### Build fails with "SDK not found"
```bash
# Update SDK from Android Studio's SDK Manager or:
$ANDROID_HOME/tools/bin/sdkmanager "platforms;android-36"
```

### Gradle sync issues
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Out of memory during build
```bash
# Increase Gradle heap size in gradle.properties
org.gradle.jvmargs=-Xmx4096m
```

### APK too large
The release build uses ProGuard minification to reduce size:
- Minification enabled: `isMinifyEnabled = true`
- Resource shrinking: `isShrinkResources = true`
- ProGuard rules in: `app/proguard-rules.pro`

### Certificate expires
```bash
# Check keystore validity
keytool -list -v -keystore skripzy-release.jks

# Generate new keystore if needed (see above)
```

## Performance Optimization

### Build Speed Improvements
```bash
# Parallel build
org.gradle.parallel=true

# Increased daemon memory (gradle.properties)
org.gradle.jvmargs=-Xmx4096m

# Configuration on demand
org.gradle.configureondemand=true
```

### APK Size Optimization
The app is configured with:
- ProGuard code obfuscation
- Resource shrinking for unused resources
- WebView component stripping
- Minified Compose libraries

Current estimated sizes:
- Debug APK: ~50-60 MB
- Release APK: ~30-40 MB

## Useful Commands

```bash
# Clean build
./gradlew clean

# Build with verbose output
./gradlew build --info --debug

# Run tests
./gradlew test

# Generate dependency report
./gradlew dependencies

# Check for dependency updates
./gradlew dependencyUpdates

# Build and install on connected device
./gradlew installDebug

# Uninstall app
./gradlew uninstallDebug

# View all available tasks
./gradlew tasks

# Run lint checks
./gradlew lint
```

## Release to Google Play

### Prepare
1. Build signed release APK: `./gradlew assembleRelease`
2. Test APK on multiple devices
3. Verify all permissions and features

### Submit
1. Create app listing on [Google Play Console](https://play.google.com/console)
2. Upload APK to internal testing track first
3. Test with beta users
4. Gradually roll out to production

### References
- [Google Play Docs](https://developer.android.com/google-play)
- [App Signing Docs](https://developer.android.com/studio/publish/app-signing)
- [Play Console Help](https://support.google.com/googleplay/android-developer)
