package com.yucsan.musica2

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application configurada con Hilt
 */
@HiltAndroidApp
class MusicApplication : Application() {

    companion object {
        private const val TAG = "MusicApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 MusicApplication iniciada")
        Log.d(TAG, "📱 App configurada con Hilt")

        // Aquí puedes inicializar otros servicios globales si es necesario
        initializeServices()
    }

    private fun initializeServices() {
        try {
            // Inicialización de servicios globales
            Log.d(TAG, "🔧 Inicializando servicios globales...")

            // Por ejemplo, puedes pre-cargar configuraciones
            // o inicializar servicios que necesites en toda la app

            Log.d(TAG, "✅ Servicios globales inicializados")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando servicios globales", e)
        }
    }
}