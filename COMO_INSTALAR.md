# Cómo instalar y actualizar Audiophile Celular en el teléfono

Esta app **no está en Google Play**: se instala a mano desde el propio repositorio de GitHub.
Funciona bien, pero es un APK fuera de tienda: Android avisa, Play Protect puede preguntar, y
**no hay actualizaciones automáticas** (cada versión nueva se instala abriendo su enlace, ver el
final).

---

## 1. Abrir la página de versiones en el móvil

En el navegador del teléfono abre:

**https://github.com/Deivi-Medina/AudiophileCelular/releases/latest**

Es la versión más reciente. Si quieres el enlace directo al archivo (solo funciona cuando la
versión publicada se llama `v1.0`):

`https://github.com/Deivi-Medina/AudiophileCelular/releases/latest/download/AudiophileCelular-v1.0.apk`

El repositorio es público, así que **no hace falta iniciar sesión en GitHub** para descargar.

## 2. Descargar el `.apk`

En esa página, dentro de **Assets**, toca el archivo que termina en **`.apk`**
(`AudiophileCelular-v1.0.apk`). Se descarga en la carpeta *Descargas* del teléfono.

> Ignora los enlaces "Source code (zip / tar.gz)": esos son el código, no la app.

## 3. Permitir instalar apps de "origen desconocido" (solo la primera vez)

1. Abre la notificación de descarga (o **Archivos → Descargas**) y toca el `.apk`.
2. Android dirá que por seguridad no puede instalar apps de este origen.
3. Toca **Ajustes** (o **Configuración**) y activa **Permitir desde esta fuente** para el
   navegador que usaste (Chrome, Firefox…).
4. Vuelve atrás y toca **Instalar**.

Solo hay que hacerlo la primera vez por navegador. Chrome puede pedir además "Permitir instalar
apps desconocidas"; es el mismo permiso.

## 4. Instalar encima para **actualizar sin perder datos**

Cuando salga una versión nueva: descarga el `.apk` nuevo y **ábrelo sin desinstalar la app**.

- Mientras la firma sea la misma (lo es: todos los APK de este repositorio se firman con la
  misma clave estable), Android lo trata como una **actualización**: se conservan XP, logros,
  favoritas, playlists, reseñas y descargas.
- **No desinstales** la app para actualizar: al desinstalar se borran los datos locales
  (los que no estén en Firebase).

Si Android se queja de que **"la app no se instaló"** o habla de un conflicto de firma, es que
la versión que tenías puesta se firmó con otra clave (por ejemplo, una compilación de Android
Studio o de un APK de debug antiguo). En ese caso, una sola vez: **desinstalar → instalar la
versión nueva** (se pierden los datos locales) y a partir de ahí las actualizaciones vuelven a
entrar encima sin problema.

## 5. Si Play Protect avisa

Es normal en apps instaladas fuera de Play. Puede aparecer:

- *"Play Protect no reconoce esta app"* o *"App no segura"* → toca **Instalar de todas formas**
  (o **Más detalles → Instalar de todas formas**).
- *"Enviar a Google para comprobar"* → se puede omitir.

Esto no significa que la app tenga virus: Google no ha analizado una app que no pasa por su
tienda. Si el aviso bloquea la instalación del todo, se puede desactivar temporalmente
**Play Store → perfil → Play Protect → ⚙️ → desactivar "Analizar apps"**, instalar, y volver a
activarlo.

## 6. Lo que conviene saber

- Sin actualizaciones automáticas: hay que repetir el paso 2 con cada versión nueva. Guarda el
  enlace de *Releases* en favoritos del navegador.
- Solo para este teléfono / uso personal: no está pensada para repartirse, y no tiene ningún
  tipo de control parental ni comprobación de Play.
- La app **descarga y reproduce audio de YouTube**: eso va contra los términos de YouTube y
  hace inviable publicarla en tiendas. Uso personal, fuera de tienda.
- Antes de instalar algo, comprueba en el Release que el APK es el de este repositorio.

---

## Para el equipo (no para el dueño)

- El APK lo construye y publica el workflow **`Android release APK`**
  (`.github/workflows/android-release-apk.yml`): `assembleRelease` firmado con la clave estable
  de `.signing/` y adjuntado a un **GitHub Release**.
- Se dispara de dos formas: **push de un tag `v*`** (por ejemplo `git tag v1.0 && git push origin v1.0`)
  o el botón **Actions → Android release APK → Run workflow** (sin tag, publica
  `v1.0-build<nnn>`).
- Nombre del asset: `AudiophileCelular-<tag>.apk`, más el artefacto del run `apk-release-firmado`.
- Siguiente versión: sube `versionCode` (una línea en `app/build.gradle.kts`) antes del tag;
  así el teléfono muestra la versión correcta en Ajustes → Apps.
