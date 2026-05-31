# Privacy Policy — BetterWiFiFTP

**Effective Date:** May 31, 2026  
**Developer:** SM Habib Jr  
**Contact:** smhabib.abir2030@gmail.com

---

> **Short version:** BetterWiFiFTP does not collect, transmit, or share any of your personal data with us or any third party. All activity stays entirely on your local network and on your device.

---

## 1. Introduction

BetterWiFiFTP is a local Wi-Fi file-sharing application for Android. It turns your Android device into an FTP server and an HTTP file server that other devices on the **same** Wi-Fi network can connect to.

This Privacy Policy explains what information the App accesses or stores, how it is used, and your rights regarding that information.

---

## 2. Information the App Accesses or Stores

BetterWiFiFTP accesses only the information needed to operate a local file-sharing server. No information is sent to any external server, and no analytics or tracking code is present in the App.

### 2a. Server Credentials (Stored Locally)

The App lets you set a username and password to protect access to your FTP/HTTP server. These credentials are stored exclusively in **Android SharedPreferences on your device**. They are never uploaded, transmitted to us, or shared with any third party.

### 2b. Wi-Fi Network Information (Read-Only, Not Stored)

The App reads your device's local IP address and Wi-Fi network name (SSID) at runtime so it can display the server address to you and configure passive-mode FTP connections. This information is **displayed on-screen only** — it is never stored, logged, or transmitted outside your device.

### 2c. File System Access (User-Selected Folder Only)

The App reads your device's storage to serve files over FTP and HTTP. It accesses **only the folder you explicitly select**. File names, sizes, and modification dates are used to generate directory listings served to devices on your local network. The App does not read, copy, or transmit your files to any external service.

### 2d. Data We Do NOT Collect

- Name, email address, or contact information
- Precise or approximate device location (GPS)
- Contacts, calendar, or call logs
- Device identifiers (advertising ID, IMEI, etc.)
- Usage analytics or crash reports
- Browsing history or app activity logs
- Payment or financial information

---

## 3. How We Use the Information

The limited information the App accesses is used solely for the following operational purposes:

- Displaying your device's local IP address and server port so you can connect from other devices
- Configuring passive-mode FTP data connections using your local IP address
- Authenticating connecting clients against the username and password you set
- Serving the files in the folder you selected to authorized clients on your local network
- Showing a foreground notification while the server is running (required by Android for background services)

None of these purposes involve transmitting data outside your device or local network.

---

## 4. Local Network Communication

BetterWiFiFTP creates an FTP server (default port 2121) and an HTTP server (default port 8888) that listen for incoming connections on your local Wi-Fi network. All data transfers occur **only between devices on the same local network**. The App does not make any outbound connections to the internet on its own.

You are responsible for the security of your local network and for choosing who you share the server address and credentials with.

---

## 5. Third-Party Services and SDKs

BetterWiFiFTP uses **no third-party analytics, advertising, crash-reporting, or tracking SDKs**. The only libraries used are:

| Library | Purpose | Data collected |
|---|---|---|
| Jetpack Compose & AndroidX (Google) | Android UI framework | None — runs entirely on-device |
| Kotlin Coroutines (JetBrains) | Concurrency | None — runs entirely on-device |

No data is shared with any third party.

---

## 6. Data Storage and Security

The only data the App persists is the FTP server username and password you configure, stored in Android's SharedPreferences (private mode). This data is stored in the App's private sandbox on your device and is not accessible to other apps.

Because the App operates entirely on your local network and stores no data on external servers, there is no remote database or cloud storage to secure or breach.

We recommend:
- Setting a strong username and password in the App
- Only running the server when you are on a trusted private network

---

## 7. Android Permissions Explained

| Permission | Why it's needed |
|---|---|
| `INTERNET` | Opens server sockets so other devices can connect to the FTP and HTTP servers |
| `ACCESS_WIFI_STATE` | Reads Wi-Fi connection status and displays your network name (SSID) |
| `ACCESS_NETWORK_STATE` | Detects network changes (e.g., stops the server when Wi-Fi is lost) |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keeps the server running while the screen is off |
| `POST_NOTIFICATIONS` *(Android 13+)* | Displays the foreground service notification |
| `READ_EXTERNAL_STORAGE` *(Android 12 and below)* | Reads files in the folder you select to serve over FTP/HTTP |
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` *(Android 13+)* | Granular replacements for READ_EXTERNAL_STORAGE on newer Android versions |

The App does **not** request `WRITE_EXTERNAL_STORAGE`, location, contacts, camera, or microphone permissions.

---

## 8. Children's Privacy

BetterWiFiFTP is not directed at children under the age of 13. The App does not knowingly collect any personal information from children. Because the App collects no personal data from any user, there is no special risk to children's privacy from using this App.

If you are a parent or guardian and have concerns, please contact us at the address below.

---

## 9. Your Rights

Because BetterWiFiFTP does not collect or transmit personal data, there is no data held by us that you would need to request access to, correct, or delete. The only data associated with the App is stored locally on your device and is fully under your control:

- You can change your username or password at any time within the App.
- Uninstalling the App removes all locally stored data (SharedPreferences) from your device.

If you are located in the EEA, United Kingdom, or California — since we hold no personal data about you, the obligations under GDPR, UK GDPR, and CCPA that relate to data we hold do not apply. You retain full control over everything on your own device.

---

## 10. Changes to This Privacy Policy

We may update this Privacy Policy from time to time. When we do, we will revise the "Effective Date" at the top of this document. Continued use of the App after changes are posted constitutes your acceptance of the updated policy.

Any future version of the App that introduces data collection will be accompanied by an updated Privacy Policy disclosing those changes before they take effect.

---

## 11. Contact Us

If you have any questions or concerns about this Privacy Policy, please contact:

**Developer:** SM Habib Jr  
**Email:** smhabib.abir2030@gmail.com
