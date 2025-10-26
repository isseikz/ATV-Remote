# Remote APK Installation System

## Overview

This system enables automatic distribution and installation of development APK builds to test devices without requiring physical `adb install` connections. When code is pushed to the repository, GitHub Actions automatically builds the APK, uploads it to S3, and sends push notifications to registered test devices for easy installation.

## Architecture

### Components

1. **Helper Android App** (`/helperApp`)
   - Receives Firebase Cloud Messaging (FCM) notifications
   - Downloads APK files from pre-signed S3 URLs
   - Handles APK installation via FileProvider
   - Maintains build history

2. **Backend Server** (`/demo/server`)
   - Manages device registration
   - Generates S3 pre-signed URLs
   - Sends FCM notifications to devices
   - Provides REST API for CI/CD integration

3. **CI/CD Pipeline** (`.github/workflows/build-and-distribute-apk.yml`)
   - Builds demo APK on push/PR
   - Uploads APK to AWS S3
   - Triggers backend notification webhook

4. **Cloud Services**
   - AWS S3: Stores APK files (private bucket)
   - Firebase Cloud Messaging: Push notifications to devices

## Setup Instructions

### Prerequisites

- Firebase project with FCM enabled
- AWS account with S3 bucket
- Firebase service account JSON key
- AWS credentials (access key ID and secret)

### 1. Firebase Setup

1. Create a Firebase project at https://console.firebase.google.com
2. Enable Firebase Cloud Messaging
3. Download the Firebase service account JSON:
   - Go to Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Save the JSON file securely
4. Add `google-services.json` to `/helperApp/` directory for Android client

### 2. AWS S3 Setup

1. Create an S3 bucket for APK storage
2. Configure bucket policy to be private
3. Create IAM user with S3 read/write permissions
4. Generate access keys for the IAM user

### 3. Backend Server Configuration

Create or update `.env` file in the project root:

```bash
# Firebase Configuration
FCM_SERVICE_ACCOUNT_PATH=/path/to/firebase-service-account.json

# AWS Configuration
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

Or set environment variables:

```bash
export FCM_SERVICE_ACCOUNT_PATH=/path/to/firebase-service-account.json
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
```

### 4. GitHub Secrets Configuration

Add the following secrets to your GitHub repository:

- `AWS_ACCESS_KEY_ID`: AWS access key
- `AWS_SECRET_ACCESS_KEY`: AWS secret key
- `BACKEND_API_TOKEN`: API token for backend authentication (optional)

Update workflow file `.github/workflows/build-and-distribute-apk.yml`:

```yaml
env:
  AWS_REGION: us-east-1
  S3_BUCKET: your-apk-bucket-name
  BACKEND_URL: https://your-server.com
```

### 5. Build and Deploy

#### Build Helper App

```bash
./gradlew :helperApp:assembleDebug
```

Install the helper app on test devices manually (initial setup only):

```bash
adb install helperApp/build/outputs/apk/debug/helperApp-debug.apk
```

#### Run Backend Server

```bash
./gradlew :demo:server:runApp
```

The server will start on `http://0.0.0.0:8080` by default.

### 6. Helper App Configuration

1. Open the helper app on your test device
2. Tap the Settings icon
3. Enter your backend server URL (e.g., `https://your-server.com`)
4. Save the configuration
5. Grant "Install unknown apps" permission when prompted
6. Grant notification permission

The app will automatically register with the backend server.

## Usage

### For Developers

1. Push code to a branch or create a pull request
2. GitHub Actions automatically builds the APK
3. APK is uploaded to S3
4. Backend server receives notification and sends FCM messages to all registered devices

### For Testers

1. Receive notification on test device
2. Tap notification to view download progress
3. When download completes, tap to install
4. Confirm installation in system dialog
5. View build history in helper app

## API Reference

### Device Registration

**Endpoint:** `POST /api/devices/register`

**Request Body:**
```json
{
  "deviceId": "unique-device-id",
  "fcmToken": "fcm-token-from-firebase",
  "deviceModel": "Pixel 6",
  "androidVersion": "14",
  "appVersion": "1.0.0"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Device registered successfully",
  "deviceId": "unique-device-id"
}
```

### Build Notification

**Endpoint:** `POST /api/builds/notify`

**Headers:**
- `Authorization: Bearer <api-token>` (optional)

**Request Body:**
```json
{
  "s3Bucket": "your-bucket",
  "s3Key": "builds/feature-branch/app-v1.0.0-abc123.apk",
  "branchName": "feature-branch",
  "commitHash": "abc123...",
  "versionName": "1.0.0",
  "versionCode": 1,
  "buildTimestamp": 1234567890,
  "targetDeviceGroup": null
}
```

**Response:**
```json
{
  "success": true,
  "message": "Build notification sent",
  "devicesNotified": 5,
  "totalDevices": 5,
  "results": {
    "device1": true,
    "device2": true
  }
}
```

### Get Registered Devices

**Endpoint:** `GET /api/devices`

**Response:**
```json
{
  "success": true,
  "count": 5,
  "devices": [
    {
      "deviceId": "unique-device-id",
      "fcmToken": "token",
      "deviceModel": "Pixel 6",
      "androidVersion": "14",
      "appVersion": "1.0.0",
      "registeredAt": 1234567890,
      "lastSeen": 1234567890
    }
  ]
}
```

### Health Check

**Endpoint:** `GET /api/health`

**Response:**
```json
{
  "status": "healthy",
  "service": "apk-installer",
  "devicesRegistered": 5
}
```

## Security Considerations

### NFR-S-01: Private S3 Bucket
- S3 bucket is configured as private
- APKs are only accessible via pre-signed URLs with expiration (2 hours default)

### NFR-S-02: HTTPS Communication
- All backend APIs use HTTPS
- Helper app only connects to HTTPS endpoints

### NFR-S-03: API Authentication
- Backend endpoints can be protected with API tokens
- FCM service account credentials stored securely

### User Permissions
- Users must manually grant "Install unknown apps" permission
- Notification permission required for FCM
- Installation requires user confirmation via system dialog

## Troubleshooting

### Helper App Not Receiving Notifications

1. Check FCM token is registered:
   - Open helper app Settings
   - Verify FCM token is displayed
   
2. Check notification permissions:
   - Android Settings → Apps → Helper App → Notifications
   - Ensure notifications are enabled

3. Check backend logs:
   - Verify device is registered in backend
   - Check FCM send results

### Download Failures

1. Check S3 pre-signed URL:
   - Verify URL expiration hasn't passed
   - Check S3 bucket permissions

2. Check network connectivity:
   - Ensure device has internet access
   - Check firewall rules

### Installation Failures

1. Check install permission:
   - Android Settings → Apps → Special app access → Install unknown apps
   - Enable for Helper App

2. Check APK integrity:
   - Verify APK downloaded completely
   - Check file size matches original

## Future Enhancements

- Device grouping for targeted distribution
- Build approval workflow
- Installation analytics and reporting
- Rollback functionality
- Multiple APK variants support
- Silent updates for beta testing groups

## License

This system is part of the ATV-Remote project and follows the same license.
