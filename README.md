# TaboolaController

## Overview
TaboolaController is an Android app designed to control the cell colors of a companion display app (`TaboolaDisplayApp`). It allows users to select a cell position, color, and visibility, and then update the display app using either an AIDL service or a ContentProvider, depending on the display app's running state.

## Features
- Select cell position (0-100)
- Choose cell color (hex format, e.g., `#FF5722`)
- Toggle cell visibility
- Update cell color in the display app via:
  - **AIDL Service** (when display app is running)
  - **ContentProvider** (when display app is not running)

## How It Works
1. **Service Connection**
   - On launch, TaboolaController attempts to bind to the display app's AIDL service.
   - If connected, updates are sent via the service method `updateCellBackgroundColor(position, colorHex, isVisible, SECURITY_TOKEN)`.
   - If not connected, updates are sent via the ContentProvider.

2. **ContentProvider Fallback**
   - When the service is not available, the app builds a `ContentValues` object with the required parameters and calls `ContentResolver.update()` using the URI:
     `content://com.example.tabooladisplayapp.cellcolorprovider/cellColor/{position}`
   - The display app will process the update when it is next started.

3. **Security**
   - All update requests include a `SECURITY_TOKEN` that must match the display app's expected value.

## Usage
1. Launch TaboolaController.
2. Enter the cell position (0-100).
3. Enter the color in hex format (e.g., `#FF5722`).
4. Toggle visibility as needed.
5. Press **Send** to update the cell color.
   - If the display app is running, the update is sent via the service.
   - If not, the update is sent via the ContentProvider.

## Requirements
- Android device with both TaboolaController and TaboolaDisplayApp installed.
- The display app must expose the AIDL service and ContentProvider as described above.

## Troubleshooting
- If updates do not work, ensure the display app is installed and the security token matches.


