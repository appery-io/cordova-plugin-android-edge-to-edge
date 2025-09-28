# Android Edge to Edge
Edge-to-edge support for **Android 15 (API 35)** in Cordova (Cordova Android 10+). Prevents overlaps with status/toolbar by exposing WindowInsets and making bars transparent.

## Installation
```bash
cordova plugin add @squareetlabs/cordova-plugin-android-edge-to-edge
```

## Usage in Cordova applications
```js
document.addEventListener('deviceready', async () => {
  await AndroidEdgeToEdge.enable({ 
    lightStatusBar: true, 
    lightNavigationBar: true,
    // Optional: packages to ignore (prevents issues with plugins like camera)
    ignoredPackages: [
      'org.apache.cordova.camera',
      'org.apache.cordova.file'
    ]
  });
  
  AndroidEdgeToEdge.subscribeInsets(({ top, bottom }) => {
    const toolbar = document.querySelector('.app-toolbar');
    const bottomBar = document.querySelector('.app-bottom-bar');
    if (toolbar) toolbar.style.paddingTop = `calc(var(--toolbar, 0px) + ${top}px)`;
    if (bottomBar) bottomBar.style.paddingBottom = `calc(var(--bottombar, 0px) + ${bottom}px)`;
  });
});
```

## Example Usage in Ionic 3
```js
 if (!isUndefined((<any>window).AndroidEdgeToEdge)) {
    (<any>window).AndroidEdgeToEdge.enable({lightStatusBar: true, lightNavigationBar: true});
    (<any>window).AndroidEdgeToEdge.subscribeInsets(({top, bottom}) => {
        const header = document.getElementsByClassName('header').item(0) as HTMLElement | null;
        const footer = document.getElementsByClassName('footer').item(document.getElementsByClassName('footer').length - 1) as HTMLElement | null;
        const appRoot = document.getElementsByClassName('app-root').item(0) as HTMLElement | null;
        if (header && footer && appRoot) {
            const headerHeightNum = parseFloat(getComputedStyle(header).height) || 0;
            const bottomHeightNum = parseFloat(getComputedStyle(footer).height) || 0;
            const offsetHeader = top - headerHeightNum;
            const offsetFooter = bottom + bottomHeightNum;
            const offsetHeaderPx = `${offsetHeader}px`;
            const offsetFooterPx = `${offsetFooter}px`;
            const totalOffset = `${offsetHeader + offsetFooter}px`;
            footer.style.bottom = offsetFooterPx;
            appRoot.style.top = offsetHeaderPx;
            appRoot.style.bottom = offsetFooterPx;
            appRoot.style.height = `calc(100% - ${totalOffset})`;
            console.log('Full computed');
        } else if (header && appRoot) {
            const headerHeightNum = parseFloat(getComputedStyle(header).height) || 0;
            const offsetHeader = top - headerHeightNum;
            const offsetHeaderPx = `${offsetHeader}px`;
            appRoot.style.top = offsetHeaderPx;
            appRoot.style.height = `calc(100% - ${offsetHeaderPx})`;
            console.log('Header computed');
        } else if (footer && appRoot) {
            const footerHeightNum = parseFloat(getComputedStyle(footer).height) || 0;
            const offsetFooter = bottom + footerHeightNum;
            const offsetFooterPx = `${offsetFooter}px`;
            footer.style.bottom = offsetFooterPx;
            appRoot.style.bottom = offsetFooterPx;
            appRoot.style.height = `calc(100% - ${offsetFooterPx})`;
            console.log('Footer computed');
        } else if (appRoot) {
            console.log('None computed');
            appRoot.style.height = '100%';
        }
    });
}
```

## API Reference

### Methods

#### `enable(options)`
Enables edge-to-edge mode in the Android application.

**Parameters:**
- `options` (Object): Configuration options (see Options table below)

**Returns:** Promise that resolves when edge-to-edge mode is enabled

#### `getInsets()`
Gets the current system insets values.

**Returns:** Promise that resolves with an object containing `top` and `bottom` inset values in pixels

#### `subscribeInsets(callback)`
Subscribes to system insets changes.

**Parameters:**
- `callback` (Function): Function that will be called when insets change, with an object containing `top` and `bottom` values in pixels

#### `checkInsets()`
Checks and updates the system insets. Useful when UI elements change and you need to recalculate insets.

**Returns:** Promise that resolves when the check is complete

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `lightStatusBar` | boolean | `true` | If `true`, uses light icons in the status bar |
| `lightNavigationBar` | boolean | `true` | If `true`, uses light icons in the navigation bar |
| `ignoredPackages` | string/array | `null` | Packages to ignore when sending insets events. Useful to prevent issues with plugins that launch their own activities like `org.apache.cordova.camera.CameraLauncher`. Can be a single string or an array of strings. |
