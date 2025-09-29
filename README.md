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
    backgroundColor: '#FFFFFF',
    // Optional: packages to ignore (prevents issues with plugins like camera)
    ignoredPackages: [
      'org.apache.cordova.camera',
      'org.apache.cordova.file'
    ]
  });
  
  AndroidEdgeToEdge.subscribeInsets(({ top, bottom, left, right }) => {
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
  // await AndroidEdgeToEdge.disable();
});
```

## Example Usage in Ionic 3
```typescript
// Importar en app.component.ts o el componente principal
import { Platform } from 'ionic-angular';

@Component({
  templateUrl: 'app.html'
})
export class MyApp {
  constructor(platform: Platform) {
    platform.ready().then(() => {
      this.setupEdgeToEdge();
    });
  }

  private setupEdgeToEdge() {
    // Verificar si el plugin está disponible
    if (typeof (<any>window).AndroidEdgeToEdge !== 'undefined') {
      // Habilitar el modo edge-to-edge
      (<any>window).AndroidEdgeToEdge.enable({
        lightStatusBar: true,
        lightNavigationBar: true,
        backgroundColor: '#FFFFFF',
        ignoredPackages: ['org.apache.cordova.camera']
      }).then(() => {
        console.log('Edge-to-edge mode enabled');
      }).catch(err => {
        console.error('Error enabling edge-to-edge mode:', err);
      });

      // Suscribirse a los cambios de insets
      (<any>window).AndroidEdgeToEdge.subscribeInsets(({top, bottom, left, right}) => {
        // Obtener referencias a los elementos principales
        const header = document.getElementsByClassName('header').item(0) as HTMLElement | null;
        const footer = document.getElementsByClassName('footer').item(document.getElementsByClassName('footer').length - 1) as HTMLElement | null;
        const appRoot = document.getElementsByClassName('app-root').item(0) as HTMLElement | null;
        const content = document.getElementsByClassName('scroll-content').item(0) as HTMLElement | null;
        
        // Aplicar insets según los elementos disponibles
        if (header && footer && appRoot) {
          // Caso: Tenemos header y footer
          const headerHeightNum = parseFloat(getComputedStyle(header).height) || 0;
          const footerHeightNum = parseFloat(getComputedStyle(footer).height) || 0;
          
          // Calcular offsets
          const offsetHeader = Math.max(0, top - headerHeightNum);
          const offsetFooter = Math.max(0, bottom - footerHeightNum);
          
          // Aplicar estilos
          header.style.paddingTop = `${top}px`;
          header.style.paddingLeft = `${left}px`;
          header.style.paddingRight = `${right}px`;
          
          footer.style.paddingBottom = `${bottom}px`;
          footer.style.paddingLeft = `${left}px`;
          footer.style.paddingRight = `${right}px`;
          
          // Ajustar el contenido principal
          if (content) {
            content.style.paddingLeft = `${left}px`;
            content.style.paddingRight = `${right}px`;
          }
          
          console.log('Insets applied to header and footer');
        } else if (header && appRoot) {
          // Caso: Solo tenemos header
          header.style.paddingTop = `${top}px`;
          header.style.paddingLeft = `${left}px`;
          header.style.paddingRight = `${right}px`;
          
          if (content) {
            content.style.paddingLeft = `${left}px`;
            content.style.paddingRight = `${right}px`;
            content.style.paddingBottom = `${bottom}px`;
          }
          
          console.log('Insets applied to header');
        } else if (footer && appRoot) {
          // Caso: Solo tenemos footer
          footer.style.paddingBottom = `${bottom}px`;
          footer.style.paddingLeft = `${left}px`;
          footer.style.paddingRight = `${right}px`;
          
          if (content) {
            content.style.paddingLeft = `${left}px`;
            content.style.paddingRight = `${right}px`;
            content.style.paddingTop = `${top}px`;
          }
          
          console.log('Insets applied to footer');
        } else if (appRoot) {
          // Caso: Solo tenemos el contenedor principal
          if (content) {
            content.style.paddingTop = `${top}px`;
            content.style.paddingBottom = `${bottom}px`;
            content.style.paddingLeft = `${left}px`;
            content.style.paddingRight = `${right}px`;
          }
          
          console.log('Insets applied to content');
        }
      });
      
      // Verificar insets cuando cambia la orientación
      window.addEventListener('orientationchange', () => {
        setTimeout(() => {
          (<any>window).AndroidEdgeToEdge.checkInsets().then(insets => {
            console.log('Insets updated after orientation change:', insets);
          });
        }, 300);
      });
    }
  }
  
  // Método para deshabilitar edge-to-edge si es necesario
  private disableEdgeToEdge() {
    if (typeof (<any>window).AndroidEdgeToEdge !== 'undefined') {
      (<any>window).AndroidEdgeToEdge.disable().then(() => {
        console.log('Edge-to-edge mode disabled');
      });
    }
  }
}
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