<h1 align="center">
     Skripzy Research
</h1>
<p align="center">
     <a href="https://app.skripzy.id">🌐 Visit Skripzy</a> •
     <a href="CI_CD.md">📦 CI/CD Setup</a> •
     <a href="LICENSE">📄 License</a>
</p>

Native Android WebView wrapper for Skripzy Research (https://app.skripzy.id).

This is a customized Android application that wraps the Skripzy Research web platform as a native mobile app with automated GitHub Actions CI/CD pipeline for APK building.

## Features
- ✅ Native Android WebView for Skripzy Research
- ✅ Pull-to-refresh functionality
- ✅ File upload support (camera & gallery)
- ✅ Automatic APK builds via GitHub Actions
- ✅ Progress indicator
- ✅ Cookie & session persistence
- ✅ Download manager integration
- ✅ Fullscreen video support
- ✅ Dark mode support
- ✅ Customizable user-agent
- ✅ WebView debugging (debug builds)

## Requirements
- Android 6.0 (API 23) or higher
- Internet connection

## Build Options

### Local Build (Android Studio)
1. Clone the repository
2. Open with Android Studio
3. Click **Build** → **Make Project**
4. Run on emulator or device

### Local Build (Command Line)
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

### Automated Build (GitHub Actions)
See [CI_CD.md](CI_CD.md) for details on:
- Automatic APK generation
- Signing APKs for Play Store
- GitHub Secrets configuration
- Building signed release APKs

## Customization

### Change Website URL
Edit [MainActivity.kt](app/src/main/java/com/roozbehzarei/superwebview/MainActivity.kt):
```kotlin
private const val WEBSITE = "https://app.skripzy.id"
```

### Change App Name
Edit [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml):
```xml
<string name="app_name">Skripzy Research</string>
```

### Change Package ID
Edit [app/build.gradle.kts](app/build.gradle.kts):
```kotlin
applicationId = "id.skripzy.app"
```

### Update App Icon
Replace files in:
- `app/src/main/res/mipmap-*`
- `app/src/main/res/drawable-v24/`

## Permissions
The app requests the following permissions:
- **INTERNET** - Required for accessing Skripzy web app
- **ACCESS_NETWORK_STATE** - Check network connectivity
- **CAMERA** - Optional, for photo uploads
- **READ_EXTERNAL_STORAGE** - Optional, for file uploads
- **WRITE_EXTERNAL_STORAGE** - Optional, for downloads
- **ACCESS_FINE_LOCATION** - Optional, for location features
- **ACCESS_COARSE_LOCATION** - Optional, for location features

## GitHub Actions (CI/CD)

Automatic APK builds are triggered on:
- ✅ Push to `main` branch
- ✅ Push to `develop` branch
- ✅ Pull requests to `main` branch
- ✅ Manual workflow dispatch

**Builds are completely free!** GitHub provides:
- Unlimited build minutes for public repos
- 2,000 minutes/month for private repos
- 500 MB of artifact storage

See [CI_CD.md](CI_CD.md) for detailed setup instructions.
