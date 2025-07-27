package com.yucsan.musica2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si las necesitas
    }
}