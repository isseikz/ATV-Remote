# Firebase Configuration

This file should contain your Firebase `google-services.json` configuration.

## How to get google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click on the gear icon → Project Settings
4. Scroll down to "Your apps" section
5. Select your Android app (or add one if not exists)
6. Download `google-services.json`
7. Place it in the `helperApp/` directory

## Package Name

Make sure your Firebase Android app uses the package name:
```
tokyo.isseikuzumaki.atvremote.helper
```

## Required Services

Enable the following services in Firebase Console:
- Firebase Cloud Messaging (FCM)

## Security Note

The `google-services.json` file is in `.gitignore` and should NOT be committed to version control.
For development, each developer should add their own configuration file.
For production, use Firebase App Distribution or environment-specific configurations.
