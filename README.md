# Jason's QR Generator

A native Android QR code generator, scanner, and management app built with Kotlin + Jetpack Compose.

**No ads. No watermarks. No subscriptions. Just QR codes.**

[![Download APK](https://img.shields.io/badge/Download-APK-red?style=for-the-badge&logo=android)](https://github.com/jsnlawrence/jasonsqrgenerator/releases/download/1.2/jasonsqrgeneratorv1.2.apk)

---

## Features

### Generator
- **Text / URL** — Encode any text or link
- **WiFi** — Generate scannable WiFi login QR codes (WPA/WEP/Open)
- **Contact Card** — vCard 3.0 with name, phone, email, organization
- **JSON** — Raw JSON payloads (Android Enterprise/EMM provisioning configs)

### Privacy & Security
- Each device gets an anonymous ID — your QR library is private to your device
- No account or login required
- Email dispatch rate-limited to 50/day per device

### Customization
- Custom foreground (dots) and background colors via visual color picker
- Error correction levels: Low (7%), Medium (15%), Quartile (25%), High (30%)
- Configurable pixel dimensions

### Scanner
- Real-time camera scanning powered by **CameraX + ML Kit**
- Instant barcode detection with copy-to-clipboard

### Cloud Library
- Save, name, and manage QR codes synced to a cloud backend
- Reload saved codes into the generator for editing
- Delete codes you no longer need

### Email Dispatch
- Send generated QR codes as inline email attachments
- Powered by server-side SMTP (no email credentials on device)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| QR Generation | ZXing Core |
| Camera/Scanning | CameraX + Google ML Kit Barcode |
| Networking | Retrofit 2 + OkHttp |
| Backend | Python Flask on PythonAnywhere |
| Theme | Custom dark theme with red accent (#E53935) |

---

## Architecture

```
app/src/main/java/com/jasonlawrence/qrmaster/
├── data/
│   ├── api/          # Retrofit service + client
│   ├── model/        # QRCode, QROptions, EmailRequest, ApiResponse
│   └── repository/   # QRRepository (network calls)
├── navigation/       # Bottom nav (Generate | Scan | Email)
├── ui/
│   ├── generator/    # Editor tab + Library tab
│   ├── scanner/      # CameraX live scanner
│   ├── email/        # Email dispatch screen
│   └── theme/        # Dark theme, colors, typography
└── viewmodel/        # QRViewModel (all business logic)
```

---

## API Endpoints

The app communicates with a Flask REST API:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/qr_service/api/qrcodes` | List all saved QR codes |
| POST | `/qr_service/api/qrcodes` | Save a new QR code |
| DELETE | `/qr_service/api/qrcodes/{id}` | Delete a QR code |
| POST | `/qr_service/api/send_email` | Send QR code via email |

---


## Install

### Direct Download
[Download the latest APK](https://github.com/jsnlawrence/jasonsqrgenerator/releases/download/1.2/jasonsqrgeneratorv1.2.apk)


## License

Personal project by JLaw All rights reserved.
