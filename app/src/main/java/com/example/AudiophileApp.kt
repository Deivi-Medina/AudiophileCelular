package com.example

import android.app.Application
import com.example.data.firebase.FirebaseBootstrap

/**
 * Application de la app.
 *
 * Su único trabajo extra es intentar conectar Firebase al arrancar. Es un
 * intento **seguro**: sin `google-services.json` no hay app de Firebase, así que
 * [FirebaseBootstrap.install] no conecta nada y la app se comporta igual que hoy.
 */
class AudiophileApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Nunca puede tumbar el arranque: si algo falla, se sigue sin Firebase.
        runCatching { FirebaseBootstrap.install(this) }
    }
}
