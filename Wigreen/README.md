# Wigreen

App Android independiente para dos widgets de color salvia/oliva:

- Hora y fecha centradas sobre fondo transparente, con temperatura y condición de Samsung Clima.
- Barra Google con accesos a Google, búsqueda, voz y Lens, conservando el verde elegido.

Abrí Wigreen, elegí el widget y tocá Agregar al inicio. Para el clima, permití leer Samsung Clima y elegí una ubicación. Tocando el reloj podés cambiar la ciudad o abrir Samsung Clima; las nuevas ciudades se agregan allí.

Samsung obtiene y actualiza el tiempo. Wigreen lee los datos locales con el permiso `com.samsung.android.weather.permission.READ_DANGEROUS_PROVIDER`. Cuando Samsung usa The Weather Channel, muestra sus datos. Los datos con más de una hora muestran «sin actualizar». Todos los widgets de clima comparten la ubicación seleccionada.

No incluye anuncios, cuentas, Internet, GPS ni captura de micrófono/cámara. Google ejecuta voz y Lens con sus propios permisos. La barra no tiene actualizaciones periódicas. El reloj usa TextClock; el clima se lee aproximadamente cada 30 minutos según Android.

Requiere Android 8+. El clima necesita un Samsung con proveedor compatible; verificado en Galaxy Z Fold 6 con Android 16. La app Google debe estar habilitada para voz y Lens. Las actualizaciones de Samsung/Google pueden cambiar sus interfaces.

Código nativo Java; JDK 17, SDK 35, Gradle 8.9. Configurá ANDROID_HOME y compilá `gradlew.bat assembleDebug`. Para release, configurá tu propia clave en signing.properties como en Licon. Las claves y configuraciones privadas no se publican.

Identificador: ar.wigreen. Versión inicial: 1.0. Icono: hoja circular verde oliva.
