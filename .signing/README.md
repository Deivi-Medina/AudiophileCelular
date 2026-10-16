# Clave de firma estable (`audiophile-release.p12`)

## Por qué existe

Un APK instalado solo se puede **actualizar** (sin desinstalar ni perder XP, logros y reseñas
locales) si el nuevo APK está firmado con **la misma clave**. Si cada compilación usara la
`signingConfig` de debug autogenerada —que es distinta en cada máquina y en cada runner— Android
rechazaría la actualización por conflicto de firma y habría que desinstalar.

Por eso el proyecto firma todos los APK de release con **una única clave fija**, la de este
directorio.

| Dato | Valor |
|---|---|
| Fichero | `.signing/audiophile-release.p12` (PKCS#12) |
| Alias | `audiophile` |
| Contraseña del almacén y de la clave | `audiophile-release` |
| Certificado | autofirmado, `CN=Audiophile Celular, O=Faro Labs, C=ES`, válido hasta 2056 |
| Huella SHA-256 del certificado | `4931b7007791e24cd9771a20d088ca4eed84316d7321253a4224c3189a605150` |

La huella está fijada en el workflow (`EXPECTED_CERT_SHA256`): si un build saliera firmado con
otra clave, el workflow **falla**. Es decir, la estabilidad de la firma se comprueba en cada
publicación, no se da por supuesta.

## Decisión y honestidad sobre su seguridad

**El keystore y su contraseña están versionados en el repositorio, y el repositorio es público.**
Esto es una **decisión de conveniencia, no una buena práctica**:

- Lo preferible es guardar la clave fuera del repo, en un secreto de GitHub
  (`ANDROID_KEYSTORE_BASE64` + `ANDROID_KEYSTORE_PASSWORD`). **No fue posible crearlos**: el
  token de automatización del equipo (`cto-new[bot]`) no tiene permiso sobre los secretos de
  Actions — `gh secret set` y `gh secret list` devuelven `HTTP 403: Resource not accessible by
  integration`. Ese permiso lo tiene el dueño o quien administre el repositorio.
- Consecuencia de tenerlo publicado: **cualquiera puede descargar esta clave y firmar un APK que
  Android aceptaría como "actualización"** de la app del dueño. No hay Play ni pagos detrás, así
  que el daño real es limitado, pero la identidad de firma del proyecto no es privada.
- Mitigaciones razonables, por orden de coste: (1) que el dueño cree los dos secretos y borre
  este directorio del repo; (2) hacer el repositorio privado; (3) dejar de repartir el APK y
  usarlo solo en local. Mientras eso no ocurra, esta es la situación real.

El workflow ya está preparado para el caso (1): **si existen los secretos, los usa y este
directorio deja de ser necesario**; no hay que tocar nada más, solo añadirlos.

## Cómo rotar la clave (no recomendado si ya hay una app instalada)

Rotar la clave obliga a desinstalar la app en todos los teléfonos donde esté puesta (se pierden
los datos locales). Solo tiene sentido ahora, antes de repartir la app a nadie: genera un
keystore nuevo, sustitúyelo, actualiza la huella en `EXPECTED_CERT_SHA256` y en esta tabla.

```bash
keytool -genkeypair -v -keystore audiophile-release.p12 -storetype PKCS12 \
  -alias audiophile -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass <pass> -keypass <pass> -dname "CN=Audiophile Celular,O=Faro Labs,C=ES"
```
