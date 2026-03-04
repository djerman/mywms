This application is intended only for use with myWMS system.

# los-mobile-view

**los-mobile-view** is an Android application developed by **Atekom d.o.o.**  
It provides mobile access to the **los-mobile** warehouse management system (based on myWMS) through a WebView interface.

## 📲 Features

- Loads the los-mobile system in a WebView
- Manual entry of WMS server URL
- Integration with external barcode scanning apps (e.g. ZXing)
- Multilingual support (English, Serbian Cyrillic, German, Russian, Spanish)
- Minimal and efficient interface for warehouse users

## 🛠️ Installation

1. Clone the repository:
   git clone https://github.com/djerman/mywms/tree/master/los-mobile-view

2. Open the project in Android Studio

3. Build and run the app on a device or emulator

4. On first launch, open the settings and enter your WMS server URL

## 🚀 Google Play Release Artifacts

For release uploads to Google Play (existing app updates), generate a signed **Android App Bundle (.aab)**.

After release build, use these additional files in Play Console:

- **Deobfuscation file (R8/ProGuard)**  
  `app/build/outputs/mapping/release/mapping.txt`
- **Native debug symbols**  
  `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`

These files improve crash and ANR diagnostics in Play Console.

Release build command:

`./gradlew :app:bundleRelease`

The build also creates:

- `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`

## 🌐 Localization

The app supports the following languages:
- English
- Serbian (Cyrillic)
- German
- Russian
- Spanish

Language is selected based on the system settings.

## 🔒 Privacy Policy

The app does not collect, store, or transmit any personal data.

### What it does:
- Displays your internal los-mobile system in WebView
- Allows manual input of a server URL
- Launches external barcode scanner apps when triggered
- Does not track or analyze user behavior
- Does not connect to third-party services

### Permissions:
- Camera – for barcode scanning (when initiated by user)
- Internet – for connecting to your WMS server

## 🏢 Developer

**Goran Djermanovic**  
Žike Popović 1, 15000 Šabac, Serbia  
Email: djermanovicgoran@yahoo.com

## 📝 License

This project is open-source and released under the MIT License.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
