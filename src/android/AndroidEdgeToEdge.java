/*
 * MIT License
 * Copyright (c) 2025 SquareetLabs
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.squareetlabs.cordova.edge2edge;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.app.Activity;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Plugin para implementar soporte de Edge-to-Edge en aplicaciones Cordova para Android.
 * Permite que las aplicaciones utilicen toda la pantalla, incluyendo las áreas de la barra de estado
 * y la barra de navegación, haciendo que estas sean transparentes y gestionando los insets adecuadamente.
 */
public class AndroidEdgeToEdge extends CordovaPlugin {
    /**
     * Callback para enviar eventos de cambios en los insets al código JavaScript.
     * Se utiliza para mantener una suscripción activa a los cambios de insets.
     */
    private CallbackContext insetsEventCallback;
    
    /**
     * Lista de nombres de paquetes que deben ser ignorados al enviar eventos de insets.
     * Útil para evitar problemas con plugins que lanzan sus propias activities.
     */
    private JSONArray ignoredPackages;
    
    /**
     * Almacena los últimos valores de insets enviados para evitar actualizaciones redundantes.
     */
    private int lastTopInset = -1;
    private int lastBottomInset = -1;

    /**
     * Método principal que ejecuta las acciones solicitadas desde JavaScript.
     * 
     * @param action Nombre de la acción a ejecutar: "enable", "getInsets" o "subscribeInsets"
     * @param args Argumentos JSON pasados desde JavaScript
     * @param callbackContext Contexto de callback para devolver resultados a JavaScript
     * @return true si la acción fue reconocida y procesada, false en caso contrario
     * @throws JSONException Si ocurre un error al procesar los datos JSON
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Activity activity = cordova.getActivity();
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        switch (action) {
            case "enable": {
                // Obtiene las opciones de configuración
                JSONObject opts = args.optJSONObject(0);
                // Determina si se deben usar iconos claros en la barra de estado (por defecto: true)
                boolean lightStatus = opts != null && opts.optBoolean("lightStatusBar", true);
                // Determina si se deben usar iconos claros en la barra de navegación (por defecto: true)
                boolean lightNav = opts != null && opts.optBoolean("lightNavigationBar", true);
                // Obtiene la lista de paquetes a ignorar (si existe)
                if (opts != null && opts.has("ignoredPackages")) {
                    try {
                        this.ignoredPackages = opts.getJSONArray("ignoredPackages");
                    } catch (JSONException e) {
                        // Si no es un array, intentamos crear uno con el valor como string
                        try {
                            String singlePackage = opts.getString("ignoredPackages");
                            this.ignoredPackages = new JSONArray();
                            this.ignoredPackages.put(singlePackage);
                        } catch (JSONException ignored) {
                            // Si falla, dejamos ignoredPackages como null
                        }
                    }
                }

                activity.runOnUiThread(() -> {
                    // Habilita el modo edge-to-edge (contenido debajo de las barras del sistema)
                    WindowCompat.setDecorFitsSystemWindows(window, false);
                    
                    // Hace las barras de estado y navegación transparentes en dispositivos con API 21+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.setStatusBarColor(Color.TRANSPARENT);
                        window.setNavigationBarColor(Color.TRANSPARENT);
                    }
                    
                    // Configura el controlador de insets para gestionar el comportamiento de las barras del sistema
                    WindowInsetsControllerCompat controller =
                            WindowCompat.getInsetsController(window, decorView);
                    if (controller != null) {
                        // Configura el comportamiento para mostrar barras al deslizar
                        controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                        // Configura el color de los iconos en las barras según las opciones
                        controller.setAppearanceLightStatusBars(lightStatus);
                        controller.setAppearanceLightNavigationBars(lightNav);
                    }
                    // Configura un listener para detectar cambios en los insets del sistema
                    ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                        WindowInsetsCompat w = insets;
                        // Obtiene el tamaño del inset superior (barra de estado)
                        int top = w.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                        // Obtiene el tamaño del inset inferior (barra de navegación)
                        int bottom = w.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                        // Envía los valores de insets al código JavaScript
                        sendInsetsToJS(top, bottom);
                        return insets;
                    });
                    // Solicita que se apliquen los insets
                    ViewCompat.requestApplyInsets(decorView);
                    // Envía respuesta de éxito al callback de JavaScript
                    callbackContext.success();
                });
                return true;
            }
            case "getInsets": {
                // Obtiene los valores actuales de insets del sistema
                activity.runOnUiThread(() -> {
                    // Obtiene los insets de la ventana raíz
                    WindowInsetsCompat w = ViewCompat.getRootWindowInsets(decorView);
                    int top = 0, bottom = 0;
                    if (w != null) {
                        // Obtiene el tamaño del inset superior (barra de estado)
                        top = w.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                        // Obtiene el tamaño del inset inferior (barra de navegación)
                        bottom = w.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    }
                    // Crea un objeto JSON con los valores de insets
                    JSONObject res = new JSONObject();
                    try { res.put("top", top); res.put("bottom", bottom); } catch (JSONException ignored) {}
                    // Envía la respuesta con los valores de insets al callback de JavaScript
                    callbackContext.success(res);
                });
                return true;
            }
            case "subscribeInsets": {
                // Almacena el callback para notificar cambios en los insets
                this.insetsEventCallback = callbackContext;
                // Crea un resultado sin datos pero que mantiene el callback activo
                PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
                // Indica que el callback debe mantenerse activo para futuras notificaciones
                pr.setKeepCallback(true);
                // Envía el resultado inicial al callback de JavaScript
                callbackContext.sendPluginResult(pr);
                return true;
            }
            case "checkInsets": {
                // Verifica y actualiza los insets actuales
                activity.runOnUiThread(() -> {
                    // Obtiene los insets de la ventana raíz
                    WindowInsetsCompat w = ViewCompat.getRootWindowInsets(decorView);
                    int top = 0, bottom = 0;
                    if (w != null) {
                        // Obtiene el tamaño del inset superior (barra de estado)
                        top = w.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                        // Obtiene el tamaño del inset inferior (barra de navegación)
                        bottom = w.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    }
                    // Envía los insets al código JavaScript
                    sendInsetsToJS(top, bottom);
                    // Envía respuesta de éxito al callback de JavaScript
                    callbackContext.success();
                });
                return true;
            }
            default:
                callbackContext.error("Unknown action: " + action);
                return false;
        }
    }

    /**
     * Verifica si el paquete actual debe ser ignorado según la lista de paquetes configurados.
     * 
     * @return true si el paquete actual está en la lista de paquetes a ignorar, false en caso contrario
     */
    private boolean shouldIgnoreCurrentPackage() {
        if (this.ignoredPackages == null || this.ignoredPackages.length() == 0) {
            return false;
        }
        
        Activity activity = cordova.getActivity();
        if (activity == null) {
            return false;
        }
        
        String currentPackage = activity.getClass().getPackage().getName();
        
        for (int i = 0; i < this.ignoredPackages.length(); i++) {
            try {
                String ignoredPackage = this.ignoredPackages.getString(i);
                if (currentPackage.startsWith(ignoredPackage)) {
                    return true;
                }
            } catch (JSONException ignored) {
                // Ignorar errores al leer elementos del array
            }
        }
        
        return false;
    }
    
    /**
     * Envía los valores de insets al código JavaScript y actualiza las variables CSS.
     * 
     * @param top Altura en píxeles del inset superior (barra de estado)
     * @param bottom Altura en píxeles del inset inferior (barra de navegación)
     */
    private void sendInsetsToJS(int top, int bottom) {
        // Verifica si el paquete actual debe ser ignorado
        if (shouldIgnoreCurrentPackage()) {
            return;
        }
        
        // Si hay un callback registrado para eventos de insets, envía los valores actualizados
        if (insetsEventCallback != null) {
            JSONObject res = new JSONObject();
            try { res.put("top", top); res.put("bottom", bottom); } catch (JSONException ignored) {}
            // Crea un resultado con los datos de insets
            PluginResult pr = new PluginResult(PluginResult.Status.OK, res);
            // Mantiene el callback activo para futuras actualizaciones
            pr.setKeepCallback(true);
            // Envía el resultado al callback de JavaScript
            insetsEventCallback.sendPluginResult(pr);
        }
        
        // Actualiza las variables CSS para que el diseño web pueda adaptarse a los insets
        String js =
            "(function(){"
          + "document.documentElement.style.setProperty('--cordova-safe-area-inset-top', '" + top + "px');"
          + "document.documentElement.style.setProperty('--cordova-safe-area-inset-bottom', '" + bottom + "px');"
          + "})();";
        // Ejecuta el código JavaScript en la WebView
        this.webView.getEngine().evaluateJavascript(js, null);
    }
}
