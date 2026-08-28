# Firebase para Mi Día con Dios

La app ya está preparada para usar Firestore sin romper el contenido local.

## 1. Crear el proyecto

1. En Firebase Console crea un proyecto.
2. Agrega una aplicación Android.
3. Usa exactamente este package name: `com.modu.midiacondios`.
4. Descarga `google-services.json`.
5. Colócalo en `app/google-services.json`.

El proyecto Gradle detecta ese archivo automáticamente y activa Google Services.

## 2. Crear Firestore

Crea una base de datos Cloud Firestore.

Colección: `devotionals`

Cada documento debe llamarse con la fecha ISO:

`YYYY-MM-DD`

Ejemplo:

`2026-08-28`

Campos de texto requeridos:

- `reference`: referencia bíblica, por ejemplo `Salmos 23:1`
- `verse`: texto breve que se mostrará como palabra del día
- `reflection`: reflexión del día
- `prayer`: oración del día

Si Firebase no está configurado, no hay Internet, falta el documento del día o falta algún campo, la aplicación debe seguir mostrando el devocional local incluido en el APK.

## 3. Reglas iniciales sugeridas

Para una primera fase, la app solo necesita lectura pública de los devocionales y ninguna escritura desde los teléfonos de usuarios. Antes de producción configura reglas que permitan lectura únicamente de la colección de contenido publicada y mantengan las escrituras restringidas a administradores.

## 4. Antes de Play Store

La clave de firma debug estable incluida en el flujo de GitHub es únicamente para APK de prueba. El AAB de producción debe usar una clave de publicación privada diferente y protegida. Nunca publiques usando la clave debug.
