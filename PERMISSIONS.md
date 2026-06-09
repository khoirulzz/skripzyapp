# Permissions Guide

## Overview
This app requires various Android permissions to function properly. Starting from Android 6.0 (API level 23), users must grant permissions at runtime.

## Required Permissions

### INTERNET
- **Purpose**: Essential for accessing the Skripzy web application
- **Runtime Request**: No (system permission)
- **User Prompt**: Yes (during app install)

### ACCESS_NETWORK_STATE
- **Purpose**: Check network connectivity before loading web content
- **Runtime Request**: No (system permission)
- **User Prompt**: Yes (during app install)

## Optional Permissions

### CAMERA
- **Purpose**: Allow users to take photos for upload in forms
- **Runtime Request**: Yes (required from Android 6.0+)
- **User Prompt**: Requested when user attempts to use camera
- **Fallback**: Users can still select photos from gallery

### READ_EXTERNAL_STORAGE
- **Purpose**: Allow users to select files and photos from device storage
- **Runtime Request**: Yes (required from Android 6.0+)
- **User Prompt**: Requested when file upload is needed
- **Fallback**: App won't be able to access files if denied

### WRITE_EXTERNAL_STORAGE
- **Purpose**: Allow saving downloaded files to device
- **Runtime Request**: Yes (required from Android 6.0+)
- **User Prompt**: Requested when downloads are initiated
- **Fallback**: Downloads will be cancelled if permission is denied

### ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION
- **Purpose**: Allow sharing device location if requested by web app
- **Runtime Request**: Yes (required from Android 6.0+)
- **User Prompt**: Requested when location is needed
- **Fallback**: Web app can detect location is not available

## Permission Request Flow

### Automatic Requests
The app will automatically request permissions when needed:

1. **Camera Access**: When user clicks on camera input field in web form
2. **Storage Access**: When user clicks on file input field
3. **Location**: When Skripzy web app requests geolocation

### Manual Permission Check
You can check granted permissions in Android Settings:

**Settings → Apps → Skripzy Research → Permissions**

## Implementation Details

Permissions are handled in [MainActivity.kt](app/src/main/java/com/roozbehzarei/superwebview/MainActivity.kt):

```kotlin
// Permission launcher
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    Log.d(TAG, "Permissions requested: $permissions")
}
```

### Android Manifest Declaration
All permissions are declared in [AndroidManifest.xml](app/src/main/AndroidManifest.xml):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Platform-Specific Notes

### Android 6.0-9 (API 23-28)
- Runtime permission requests are required for dangerous permissions
- User can grant/deny each permission individually

### Android 10+ (API 29+)
- Scoped storage affects how files are accessed
- Write operations to external storage are restricted
- Download manager handles file downloads automatically

### Android 12+ (API 31+)
- Additional permission groups for approximate and precise location
- More granular permission controls

### Android 13+ (API 33+)
- READ_MEDIA_* permissions required for accessing media files
- Photo picker API available for safer file selection

## User Privacy

### Permissions NOT Requested
- Camera access is only enabled when explicitly needed
- Location is only shared with web app's permission
- No tracking or analytics permissions
- No personal data collection

### Data Handling
- App does not store camera photos
- Downloads are managed by Android's Download Manager
- Location data is only sent to Skripzy servers if user grants permission

## Troubleshooting

### "Permission denied" for camera
- Check if camera is available on the device
- Ensure camera permission is granted in Settings
- Try uploading from gallery instead

### Files won't download
- Check storage permission in Settings
- Ensure device has available storage space
- Check Downloads folder for files

### Location not working
- Enable location permission in Settings
- Enable location services on device
- Disable VPN if blocking location services

## Best Practices for Users

1. **Grant permissions wisely** - Only grant permissions you're comfortable sharing
2. **Check Settings** - You can revoke permissions anytime in app settings
3. **Report issues** - If permissions seem to be misbehaving, report to Skripzy support

## Resources

- [Android Permissions Documentation](https://developer.android.com/guide/topics/permissions/overview)
- [Jetpack Permissions API](https://developer.android.com/training/permissions)
- [Privacy & Security Best Practices](https://developer.android.com/topic/security/best-practices)
