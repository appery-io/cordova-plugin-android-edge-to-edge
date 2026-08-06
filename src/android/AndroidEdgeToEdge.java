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

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Edge-to-edge support for Cordova on Android 15+ (API 35) / Android 16.
 *
 * Cordova Android 14+/15 already applies system-bar margins to the WebView in
 * CordovaActivity.createViews() when preference {@code AndroidEdgeToEdge} is
 * false. This plugin must not install a competing OnApplyWindowInsetsListener
 * that overwrites those margins (often to 0 on Samsung One UI / Android 16)
 * while CSS {@code env(safe-area-inset-*)} / Ionic {@code --ion-safe-area-*}
 * stay large — that stretches {@code ion-tab-bar} with icons stuck at the top.
 *
 * Behaviour:
 * <ul>
 *   <li>Default (Cordova owns insets): keep Cordova WebView margins, set
 *       {@code --ion-safe-area-*} to 0, expose Cordova margins as
 *       {@code --cordova-safe-area-inset-*} in CSS pixels.</li>
 *   <li>Opt-in ({@code AndroidEdgeToEdge=true}): do not pad the WebView;
 *       set {@code --ion-safe-area-*} from system insets so Ionic pads
 *       toolbars / tab bars correctly.</li>
 * </ul>
 */
public class AndroidEdgeToEdge extends CordovaPlugin {

    private CallbackContext insetsEventCallback;
    private JSONArray ignoredPackages;
    private boolean enabled = false;
    private int backgroundColor = Color.WHITE;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        Activity activity = cordova.getActivity();
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        switch (action) {
            case "setBackgroundColor": {
                String colorStr = args.optString(0, "#FFFFFF");
                try {
                    int color = Color.parseColor(colorStr);
                    activity.runOnUiThread(() -> {
                        this.backgroundColor = color;
                        setBackgroundColor(color);
                        callbackContext.success();
                    });
                } catch (Exception e) {
                    callbackContext.error("Invalid color format. Use #RRGGBB or #AARRGGBB format.");
                }
                return true;
            }
            case "enable": {
                JSONObject opts = args.optJSONObject(0);
                boolean lightStatus = opts == null || opts.optBoolean("lightStatusBar", true);
                boolean lightNav = opts == null || opts.optBoolean("lightNavigationBar", true);

                if (opts != null && opts.has("backgroundColor")) {
                    try {
                        this.backgroundColor = Color.parseColor(opts.getString("backgroundColor"));
                    } catch (Exception ignored) {
                        // keep previous / default background
                    }
                }

                if (opts != null && opts.has("ignoredPackages")) {
                    try {
                        this.ignoredPackages = opts.getJSONArray("ignoredPackages");
                    } catch (JSONException e) {
                        try {
                            String singlePackage = opts.getString("ignoredPackages");
                            this.ignoredPackages = new JSONArray();
                            this.ignoredPackages.put(singlePackage);
                        } catch (JSONException ignored) {
                            // leave ignoredPackages as null
                        }
                    }
                }

                activity.runOnUiThread(() -> {
                    WindowCompat.setDecorFitsSystemWindows(window, false);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.setStatusBarColor(Color.TRANSPARENT);
                        window.setNavigationBarColor(Color.TRANSPARENT);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.setNavigationBarContrastEnforced(false);
                        window.setStatusBarContrastEnforced(false);
                    }

                    WindowInsetsControllerCompat controller =
                            WindowCompat.getInsetsController(window, decorView);
                    if (controller != null) {
                        controller.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                        controller.setAppearanceLightStatusBars(lightStatus);
                        controller.setAppearanceLightNavigationBars(lightNav);
                    }

                    setBackgroundColor(this.backgroundColor);
                    applyInsets();
                    enabled = true;
                    callbackContext.success();
                });
                return true;
            }
            case "disable": {
                activity.runOnUiThread(() -> {
                    // Keep DecorFitsSystemWindows=false: Cordova Android 14+ expects it.
                    // Only restore opaque bars and detach our listener.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.setStatusBarColor(Color.BLACK);
                        window.setNavigationBarColor(Color.BLACK);
                    }

                    removeInsets();
                    enabled = false;
                    callbackContext.success();
                });
                return true;
            }
            case "getInsets": {
                activity.runOnUiThread(() -> callbackContext.success(getCurrentInsetsAsJson()));
                return true;
            }
            case "subscribeInsets": {
                this.insetsEventCallback = callbackContext;
                PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
                pr.setKeepCallback(true);
                callbackContext.sendPluginResult(pr);

                activity.runOnUiThread(() -> notifyInsetsToJS(getCurrentInsetsAsJson()));
                return true;
            }
            case "checkInsets": {
                activity.runOnUiThread(() -> {
                    applyInsets();
                    callbackContext.success(getCurrentInsetsAsJson());
                });
                return true;
            }
            default:
                callbackContext.error("Unknown action: " + action);
                return false;
        }
    }

    /**
     * CordovaActivity pads the WebView for system bars unless preference
     * AndroidEdgeToEdge is true (app opts into full edge-to-edge).
     */
    private boolean cordovaOwnsSystemBarInsets() {
        return preferences == null || !preferences.getBoolean("AndroidEdgeToEdge", false);
    }

    private float density() {
        View webView = this.webView.getView();
        DisplayMetrics metrics = webView.getResources().getDisplayMetrics();
        return metrics.density <= 0f ? 1f : metrics.density;
    }

    private int pxToCss(int physicalPx) {
        return Math.round(physicalPx / density());
    }

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
                // skip bad entries
            }
        }
        return false;
    }

    /**
     * Returns WebView layout margins converted to CSS pixels for JS / CSS vars.
     */
    private JSONObject getCurrentInsetsAsJson() {
        JSONObject res = new JSONObject();
        try {
            View webView = this.webView.getView();
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) webView.getLayoutParams();
            res.put("top", pxToCss(layoutParams.topMargin));
            res.put("bottom", pxToCss(layoutParams.bottomMargin));
            res.put("left", pxToCss(layoutParams.leftMargin));
            res.put("right", pxToCss(layoutParams.rightMargin));
        } catch (Exception ignored) {
            try {
                res.put("top", 0);
                res.put("bottom", 0);
                res.put("left", 0);
                res.put("right", 0);
            } catch (JSONException ignored2) {
                // ignore
            }
        }
        return res;
    }

    private JSONObject insetsFromSystemBars(WindowInsetsCompat windowInsets) {
        JSONObject res = new JSONObject();
        try {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());

            // When the keyboard is open, Ionic/keyboard plugins usually handle bottom padding;
            // expose the IME overlap above the nav bar so callers can react if needed.
            int bottomPhysical = keyboardVisible
                    ? Math.max(bars.bottom, ime.bottom)
                    : bars.bottom;

            res.put("top", pxToCss(bars.top));
            res.put("bottom", pxToCss(bottomPhysical));
            res.put("left", pxToCss(bars.left));
            res.put("right", pxToCss(bars.right));
        } catch (JSONException ignored) {
            // ignore
        }
        return res;
    }

    private void notifyInsetsToJS(JSONObject insets) {
        if (shouldIgnoreCurrentPackage()) {
            return;
        }

        if (insetsEventCallback != null) {
            PluginResult pr = new PluginResult(PluginResult.Status.OK, insets);
            pr.setKeepCallback(true);
            insetsEventCallback.sendPluginResult(pr);
        }

        syncCssVariables(insets, cordovaOwnsSystemBarInsets());
    }

    /**
     * @param cordovaOwns when true, Ionic must not also pad for system bars
     *                    ({@code --ion-safe-area-*} = 0). When false, Ionic should
     *                    pad using the provided inset values.
     */
    private void syncCssVariables(JSONObject insets, boolean cordovaOwns) {
        try {
            int top = insets.optInt("top", 0);
            int bottom = insets.optInt("bottom", 0);
            int left = insets.optInt("left", 0);
            int right = insets.optInt("right", 0);

            int ionTop = cordovaOwns ? 0 : top;
            int ionBottom = cordovaOwns ? 0 : bottom;
            int ionLeft = cordovaOwns ? 0 : left;
            int ionRight = cordovaOwns ? 0 : right;

            String js =
                    "(function(){"
                  + "var r=document.documentElement;"
                  + "r.style.setProperty('--cordova-safe-area-inset-top','" + top + "px');"
                  + "r.style.setProperty('--cordova-safe-area-inset-bottom','" + bottom + "px');"
                  + "r.style.setProperty('--cordova-safe-area-inset-left','" + left + "px');"
                  + "r.style.setProperty('--cordova-safe-area-inset-right','" + right + "px');"
                  + "r.style.setProperty('--ion-safe-area-top','" + ionTop + "px');"
                  + "r.style.setProperty('--ion-safe-area-bottom','" + ionBottom + "px');"
                  + "r.style.setProperty('--ion-safe-area-left','" + ionLeft + "px');"
                  + "r.style.setProperty('--ion-safe-area-right','" + ionRight + "px');"
                  + "if(!document.getElementById('android-edge-to-edge-safe-area-fix')){"
                  + "  var s=document.createElement('style');"
                  + "  s.id='android-edge-to-edge-safe-area-fix';"
                  + "  s.textContent="
                  + (cordovaOwns
                        ? "':root{--ion-safe-area-top:0px!important;--ion-safe-area-bottom:0px!important;"
                          + "--ion-safe-area-left:0px!important;--ion-safe-area-right:0px!important;}"
                          + "ion-tab-bar{padding-bottom:0!important;"
                          + "height:var(--aioTabsHeight,56px)!important;max-height:64px!important;"
                          + "min-height:50px!important;box-sizing:border-box;}'"
                        : "''")
                  + ";"
                  + "  if(s.textContent){(document.head||document.documentElement).appendChild(s);}"
                  + "}"
                  + "})();";

            this.webView.getEngine().evaluateJavascript(js, null);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void applyInsets() {
        final View webView = this.webView.getView();
        final boolean cordovaOwns = cordovaOwnsSystemBarInsets();

        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, windowInsets) -> {
            if (cordovaOwns) {
                // CordovaActivity.rootLayout already set WebView margins.
                // Do not overwrite them and do not consume insets.
                notifyInsetsToJS(getCurrentInsetsAsJson());
                return windowInsets;
            }

            // App opted into AndroidEdgeToEdge=true: leave WebView full-bleed and
            // let Ionic pad via --ion-safe-area-*.
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (mlp.topMargin != 0 || mlp.bottomMargin != 0
                    || mlp.leftMargin != 0 || mlp.rightMargin != 0) {
                mlp.setMargins(0, 0, 0, 0);
                v.setLayoutParams(mlp);
            }

            notifyInsetsToJS(insetsFromSystemBars(windowInsets));
            return windowInsets;
        });

        // Initial sync (margins may already be set by Cordova).
        WindowInsetsCompat current = ViewCompat.getRootWindowInsets(webView);
        if (cordovaOwns) {
            notifyInsetsToJS(getCurrentInsetsAsJson());
        } else if (current != null) {
            notifyInsetsToJS(insetsFromSystemBars(current));
        } else {
            notifyInsetsToJS(getCurrentInsetsAsJson());
        }

        ViewCompat.requestApplyInsets(webView);
    }

    private void removeInsets() {
        View webView = this.webView.getView();
        ViewCompat.setOnApplyWindowInsetsListener(webView, null);

        // Do not clear Cordova-managed margins when Cordova owns insets.
        if (!cordovaOwnsSystemBarInsets()) {
            ViewGroup.MarginLayoutParams mlp =
                    (ViewGroup.MarginLayoutParams) webView.getLayoutParams();
            mlp.setMargins(0, 0, 0, 0);
            webView.setLayoutParams(mlp);
        }

        notifyInsetsToJS(getCurrentInsetsAsJson());
    }

    private void setBackgroundColor(int color) {
        View webView = this.webView.getView();
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(color);
        }
    }
}
