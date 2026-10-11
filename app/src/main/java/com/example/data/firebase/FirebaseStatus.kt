package com.example.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * ¿Está Firebase configurado en esta instalación?
 *
 * Firebase solo se inicializa solo si la app trae `google-services.json` (el
 * plugin `com.google.gms.google-services` genera `google_app_id` y demás
 * recursos). Sin ese archivo **no hay app de Firebase** y todo lo que dependa
 * de Firestore tiene que comportarse como si no existiera backend:
 * cero seguidores, sección de reseñas vacía, sin errores en pantalla.
 *
 * Esta es la única comprobación que decide si se enciende la parte de Firebase.
 */
object FirebaseStatus {

    /** `true` si hay al menos una app de Firebase inicializada (hay configuración). */
    fun isConfigured(context: Context): Boolean = try {
        FirebaseApp.getApps(context.applicationContext).isNotEmpty()
    } catch (t: Throwable) {
        // Nunca puede tumbar la app: si algo raro pasa, se trata como "sin Firebase".
        false
    }
}
