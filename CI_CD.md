# GitHub Actions CI/CD Setup

## Overview
This project uses GitHub Actions to automatically build APKs on every push and pull request.

## Features
- ✅ **Automatic Debug APK builds** on every push
- ✅ **Automatic Release APK builds** (unsigned) on every push  
- ✅ **Artifact storage** for 7 days
- ✅ **Manual trigger** for custom builds
- ✅ **Automatic release creation** on main branch pushes

## Build Status
View the build status in the [Actions](../../actions) tab.

## Generated APKs

### Debug APK
- Built automatically on every push
- Can be installed directly on Android devices
- Great for testing and development
- Download from the workflow artifacts

### Release APK (Unsigned)
- Built automatically on every push
- Requires manual signing before uploading to Google Play
- Can be installed for distribution testing
- Download from the workflow artifacts

## Signing Release APK for Google Play

### Step 1: Create Keystore (One-time setup)

```bash
keytool -genkey -v -keystore skripzy-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias skripzy_key
```

You'll be prompted to enter:
- Keystore password
- Key password (can be same as keystore password)
- Your name, organization, etc.

**Important:** Keep the keystore file and passwords secure!

### Step 2: Configure app/build.gradle.kts

The project is already configured to use the keystore. Update `app/build.gradle.kts` if needed:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(project.getProperty("storeFile") ?: "")
        storePassword = project.getProperty("storePassword") as? String
        keyAlias = project.getProperty("keyAlias") as? String
        keyPassword = project.getProperty("keyPassword") as? String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### Step 3: Add Signing Configuration

Create `keystore.properties` in the project root (copy from `keystore.properties.example`):

```properties
storeFile=skripzy-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=skripzy_key
keyPassword=YOUR_KEY_PASSWORD
```

**Important:** Never commit `keystore.properties` to version control!

### Step 4: Build Signed Release APK

```bash
./gradlew assembleRelease -PstoreFile=skripzy-release.jks -PstorePassword=YOUR_PASSWORD -PkeyAlias=skripzy_key -PkeyPassword=YOUR_KEY_PASSWORD
```

Or if using `keystore.properties`:

```bash
./gradlew assembleRelease
```

## GitHub Secrets (Optional - for CI/CD signing)

To automatically sign APKs in GitHub Actions, add these secrets to your repository:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Add these secrets:
   - `SIGNING_STORE_FILE_BASE64`: Base64 encoded keystore file
   - `SIGNING_STORE_PASSWORD`: Keystore password
   - `SIGNING_KEY_ALIAS`: Key alias
   - `SIGNING_KEY_PASSWORD`: Key password

To encode the keystore:
```bash
base64 -i skripzy-release.jks > keystore_base64.txt
```

Then update the workflow to use these secrets:

```yaml
- name: Decode Keystore
  run: |
    echo "${{ secrets.SIGNING_STORE_FILE_BASE64 }}" | base64 -d > skripzy-release.jks

- name: Build Signed Release APK
  run: ./gradlew assembleRelease -PstoreFile=skripzy-release.jks -PstorePassword=${{ secrets.SIGNING_STORE_PASSWORD }} -PkeyAlias=${{ secrets.SIGNING_KEY_ALIAS }} -PkeyPassword=${{ secrets.SIGNING_KEY_PASSWORD }}
```

## Costs

**GitHub Actions for this project is completely FREE!**

### Free Tier Includes:
- **Unlimited minutes** for public repositories
- **2,000 minutes/month** for private repositories
- **500 MB** of storage for workflow artifacts
- Each workflow can run for up to **6 hours**

This project uses minimal resources (~2-3 minutes per build), so you'll have plenty of free CI/CD minutes available.

## Troubleshooting

### Build fails with "SDK not found"
- GitHub's ubuntu-latest image includes Android SDK 36
- If you need a different API level, update the workflow file

### APK not generated
- Check the build logs in the Actions tab
- Ensure `gradlew` has execute permissions: `chmod +x gradlew`
- Verify Java version (using JDK 17)

### Artifacts not uploading
- Ensure the APK output path is correct
- Check workflow permissions (read/write access)

## References
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin Documentation](https://developer.android.com/studio/build)
- [APK Signing Guide](https://developer.android.com/studio/publish/app-signing)
