# Android Edge to Edge

Edge-to-edge support for **Android 15 (API 35)** in Cordova (Cordova Android 10+). Prevents overlaps with status/toolbar by exposing WindowInsets and making bars transparent.

## Installation
```bash
cordova plugin add @squareetlabs/cordova-plugin-android-edge-to-edge
```

## Sample Code, it is recommended to add it into app constructor into section this.platform.ready().then(() => { 
```js

  await window.AndroidEdgeToEdge.enable({ 
    lightStatusBar: true, 
    lightNavigationBar: true,
    backgroundColor: '#FFFFFF',
    // Optional: packages to ignore (prevents issues with plugins like camera)
    ignoredPackages: [
      'org.apache.cordova.camera',
      'org.apache.cordova.file'
    ]
  });
  
  window.AndroidEdgeToEdge.subscribeInsets(({ top, bottom, left, right }) => {
    const toolbar = document.querySelector('.app-toolbar');
    const bottomBar = document.querySelector('.app-bottom-bar');
    if (toolbar) toolbar.style.paddingTop = `calc(var(--toolbar, 0px) + ${top}px)`;
    if (bottomBar) bottomBar.style.paddingBottom = `calc(var(--bottombar, 0px) + ${bottom}px)`;
    
    // You can also access insets via CSS variables:
    // --cordova-safe-area-inset-top
    // --cordova-safe-area-inset-bottom
    // --cordova-safe-area-inset-left
    // --cordova-safe-area-inset-right
  });
  
  // When you need to disable edge-to-edge mode:
  // await window.AndroidEdgeToEdge.disable();
  
  // If you need to change the background color dynamically:
  // await window.AndroidEdgeToEdge.setBackgroundColor('#000000');

```

## Key Features

- **Automatic Margin Handling**: Automatically applies margins to WebView based on system insets
- **Full Edge-to-Edge Support**: Makes status and navigation bars transparent
- **Keyboard Support**: Handles virtual keyboard appearance and disappearance
- **CSS Variables**: Exposes insets as CSS variables for easy styling
- **Cutout Support**: Handles display cutouts (notches) properly
- **Callback API**: Notifies your app when insets change
- **Plugin Compatibility**: Option to ignore specific packages that might conflict

## API Reference

### Methods

#### `enable(options)`
Enables edge-to-edge mode in the Android application.

**Parameters:**
- `options` (Object): Configuration options (see Options table below)

**Returns:** Promise that resolves when edge-to-edge mode is enabled

#### `disable()`
Disables edge-to-edge mode and restores default system bars.

**Returns:** Promise that resolves when edge-to-edge mode is disabled

#### `getInsets()`
Gets the current system insets values.

**Returns:** Promise that resolves with an object containing `top`, `bottom`, `left`, and `right` inset values in pixels

#### `subscribeInsets(callback)`
Subscribes to system insets changes.

**Parameters:**
- `callback` (Function): Function that will be called when insets change, with an object containing `top`, `bottom`, `left`, and `right` values in pixels

#### `checkInsets()`
Checks and updates the system insets. Useful when UI elements change and you need to recalculate insets.

**Returns:** Promise that resolves with the current inset values

#### `setBackgroundColor(color)`
Changes the background color of the WebView container.

**Parameters:**
- `color` (string): Color in CSS format (#RRGGBB or #AARRGGBB)

**Returns:** Promise that resolves when the background color has been changed

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `lightStatusBar` | boolean | `true` | If `true`, uses light icons in the status bar |
| `lightNavigationBar` | boolean | `true` | If `true`, uses light icons in the navigation bar |
| `backgroundColor` | string | `#FFFFFF` | Background color for the WebView container |
| `ignoredPackages` | string/array | `null` | Packages to ignore when sending insets events. Useful to prevent issues with plugins that launch their own activities like `org.apache.cordova.camera.CameraLauncher`. Can be a single string or an array of strings. |

### CSS Variables

The plugin automatically sets the following CSS variables that you can use in your stylesheets:

```css
:root {
  --cordova-safe-area-inset-top: 0px;
  --cordova-safe-area-inset-bottom: 0px;
  --cordova-safe-area-inset-left: 0px;
  --cordova-safe-area-inset-right: 0px;
}
```

## Troubleshooting

If you experience issues with the plugin:

1. Make sure you're using Cordova Android 10 or higher
2. Try calling `checkInsets()` after UI changes or orientation changes
3. For plugins that launch their own activities (like camera), add them to the `ignoredPackages` option
4. If you're using a custom WebView implementation, make sure it's compatible with WindowInsets API
