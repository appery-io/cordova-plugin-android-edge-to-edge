/**
 * @fileoverview Plugin para implementar soporte Edge-to-Edge en aplicaciones Cordova para Android.
 * @license MIT
 * @copyright Squareet Labs 2025
 */

var exec = require('cordova/exec');

/**
 * API JavaScript para el plugin AndroidEdgeToEdge
 * @namespace
 */
var AndroidEdgeToEdge = {
  /**
   * Habilita el modo edge-to-edge en la aplicación Android
   * @param {Object} [opts] - Opciones de configuración
   * @param {boolean} [opts.lightStatusBar=true] - Si true, usa iconos claros en la barra de estado
   * @param {boolean} [opts.lightNavigationBar=true] - Si true, usa iconos claros en la barra de navegación
   * @param {string} [opts.backgroundColor="#FFFFFF"] - Color de fondo para el contenedor de la WebView (formato CSS)
   * @param {string|string[]} [opts.ignoredPackages] - Paquetes a ignorar al enviar eventos de insets
   *                                                  (útil para evitar problemas con plugins como camera)
   * @returns {Promise<void>} Promesa que se resuelve cuando se habilita el modo edge-to-edge
   */
  enable: function (opts) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "AndroidEdgeToEdge", "enable", [opts || {}]);
    });
  },
  
  /**
   * Deshabilita el modo edge-to-edge en la aplicación Android
   * @returns {Promise<void>} Promesa que se resuelve cuando se deshabilita el modo edge-to-edge
   */
  disable: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "AndroidEdgeToEdge", "disable", []);
    });
  },
  
  /**
   * Obtiene los valores actuales de insets del sistema
   * @returns {Promise<{top: number, bottom: number, left: number, right: number}>} Promesa que se resuelve con los valores de insets
   */
  getInsets: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "AndroidEdgeToEdge", "getInsets", []);
    });
  },
  
  /**
   * Suscribe a cambios en los insets del sistema
   * @param {Function} cb - Función de callback que recibe los valores de insets actualizados
   * @param {Object} cb.insets - Objeto con los valores de insets
   * @param {number} cb.insets.top - Altura en píxeles del inset superior (barra de estado)
   * @param {number} cb.insets.bottom - Altura en píxeles del inset inferior (barra de navegación)
   * @param {number} cb.insets.left - Ancho en píxeles del inset izquierdo
   * @param {number} cb.insets.right - Ancho en píxeles del inset derecho
   */
  subscribeInsets: function (cb) {
    exec(function (data) {
      try { cb && cb(data); } catch (e) {}
    }, function () {}, "AndroidEdgeToEdge", "subscribeInsets", []);
  },
  
  /**
   * Verifica y actualiza los insets del sistema
   * Útil cuando se necesita recalcular los insets después de cambios en la UI
   * @returns {Promise<{top: number, bottom: number, left: number, right: number}>} Promesa que se resuelve con los valores de insets actualizados
   */
  checkInsets: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "AndroidEdgeToEdge", "checkInsets", []);
    });
  },
  
  /**
   * Cambia el color de fondo del contenedor de la WebView
   * @param {string} color - Color en formato CSS (#RRGGBB o #AARRGGBB)
   * @returns {Promise<void>} Promesa que se resuelve cuando se cambia el color de fondo
   */
  setBackgroundColor: function (color) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "AndroidEdgeToEdge", "setBackgroundColor", [color]);
    });
  }
};

module.exports = AndroidEdgeToEdge;
