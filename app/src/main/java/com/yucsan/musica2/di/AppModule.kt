package com.yucsan.musica2.di

import android.content.Context
import com.yucsan.musica2.service.JamendoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para inyección de dependencias
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Proporciona instancia singleton de JamendoService
     */
    @Provides
    @Singleton
    fun provideJamendoService(): JamendoService {
        return JamendoService.getInstance()
    }

    /**
     * Proporciona contexto de aplicación si es necesario
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }
}