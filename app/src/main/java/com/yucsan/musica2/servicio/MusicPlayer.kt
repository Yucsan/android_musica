package com.yucsan.musica2.servicio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.service.MusicService

class MusicPlayer(private val context: Context) : MusicService.MusicServiceListener {

    companion object {
        private const val TAG = "MusicPlayer"
    }

    private var musicService: MusicService? = null
    private var isServiceBound = false

    // Listener para comunicar cambios a la UI
    interface MusicPlayerListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onSongChanged(song: Song?)
        fun onProgressUpdate(currentPosition: Long, duration: Long)
    }

    private var playerListener: MusicPlayerListener? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService().apply {
                setServiceListener(this@MusicPlayer)
            }
            isServiceBound = true
            Log.d(TAG, "✅ MusicService conectado")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isServiceBound = false
            Log.d(TAG, "❌ MusicService desconectado")
        }
    }

    fun setPlayerListener(listener: MusicPlayerListener) {
        this.playerListener = listener
    }

    fun bindService() {
        if (!isServiceBound) {
            val intent = Intent(context, MusicService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "🔗 Vinculando MusicService...")
        }
    }

    fun unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
            Log.d(TAG, "🔗 MusicService desvinculado")
        }
    }

    // ============================================================================
    // MÉTODOS EXISTENTES (mantener compatibilidad)
    // ============================================================================

    fun playSong(song: Song) {
        Log.d(TAG, "🎵 Reproducir canción: ${song.title}")
        musicService?.playSong(song)
    }

    fun pauseMusic() {
        Log.d(TAG, "⏸️ Pausar música")
        musicService?.pauseMusic()
    }

    fun resumeMusic() {
        Log.d(TAG, "▶️ Reanudar música")
        musicService?.resumeMusic()
    }

    fun stopMusic() {
        Log.d(TAG, "⏹️ Parar música")
        musicService?.stopMusic()
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    fun getCurrentPosition(): Long {
        return musicService?.getCurrentPosition() ?: 0L
    }

    fun getDuration(): Long {
        return musicService?.getDuration() ?: 0L
    }

    fun isPlaying(): Boolean {
        return musicService?.isPlaying() ?: false
    }

    // ============================================================================
    // NUEVOS MÉTODOS PARA COMPATIBILIDAD CON VIEWMODEL
    // ============================================================================

    /**
     * Reproducir desde URL (para YouTube streams)
     * Crea un Song temporal y lo reproduce
     */
    fun playFromUrl(url: String, title: String) {
        try {
            Log.d(TAG, "🌐 Reproducir desde URL: $title")
            Log.d(TAG, "🔗 URL: ${url.take(50)}...")

            // Crear Song temporal con la URL
            val tempSong = Song(
                id = "url_${System.currentTimeMillis()}", // ID único temporal
                title = title,
                artist = "Streaming",
                duration = 0L, // Se actualizará cuando se cargue
                thumbnailUrl = null,
                youtubeUrl = url,
                audioUrl = url // La URL directa del stream
            )

            // Usar el método existente
            playSong(tempSong)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reproduciendo desde URL", e)
        }
    }

    /**
     * Pausar (alias para compatibilidad)
     */
    fun pause() {
        pauseMusic()
    }

    /**
     * Reanudar (alias para compatibilidad)
     */
    fun resume() {
        resumeMusic()
    }

    /**
     * Parar (alias para compatibilidad)
     */
    fun stop() {
        stopMusic()
    }

    /**
     * Verificar si el servicio está listo
     */
    fun isServiceReady(): Boolean {
        return isServiceBound && musicService != null
    }

    /**
     * Reproducir con reintentos si el servicio no está listo
     */
    fun playWithRetry(song: Song, maxRetries: Int = 3) {
        var retries = 0

        fun attemptPlay() {
            if (isServiceReady()) {
                playSong(song)
                Log.d(TAG, "✅ Reproducción exitosa después de $retries reintentos")
            } else if (retries < maxRetries) {
                retries++
                Log.d(TAG, "⏳ Servicio no listo, reintento $retries/$maxRetries...")

                // Esperar un poco y reintentar
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    attemptPlay()
                }, 500L * retries) // Delay incremental
            } else {
                Log.e(TAG, "❌ No se pudo reproducir después de $maxRetries reintentos")
            }
        }

        attemptPlay()
    }

    /**
     * Reproducir URL con reintentos
     */
    fun playFromUrlWithRetry(url: String, title: String, maxRetries: Int = 3) {
        var retries = 0

        fun attemptPlay() {
            if (isServiceReady()) {
                playFromUrl(url, title)
                Log.d(TAG, "✅ Reproducción URL exitosa después de $retries reintentos")
            } else if (retries < maxRetries) {
                retries++
                Log.d(TAG, "⏳ Servicio no listo para URL, reintento $retries/$maxRetries...")

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    attemptPlay()
                }, 500L * retries)
            } else {
                Log.e(TAG, "❌ No se pudo reproducir URL después de $maxRetries reintentos")
            }
        }

        attemptPlay()
    }

    // ============================================================================
    // CALLBACKS DEL SERVICIO (sin cambios)
    // ============================================================================

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        Log.d(TAG, "🔄 Estado de reproducción: $isPlaying")
        playerListener?.onPlaybackStateChanged(isPlaying)
    }

    override fun onSongChanged(song: Song?) {
        Log.d(TAG, "🎵 Canción cambiada: ${song?.title}")
        playerListener?.onSongChanged(song)
    }

    override fun onProgressUpdate(currentPosition: Long, duration: Long) {
        // Log solo cada 10 segundos para no saturar
        if (currentPosition % 10000 < 1000) {
            Log.d(TAG, "⏱️ Progreso: ${currentPosition / 1000}s / ${duration / 1000}s")
        }
        playerListener?.onProgressUpdate(currentPosition, duration)
    }

    // ============================================================================
    // MÉTODOS DE DEBUGGING
    // ============================================================================

    /**
     * Obtener estado detallado para debugging
     */
    fun getDetailedStatus(): String {
        return buildString {
            appendLine("🔧 Estado del MusicPlayer:")
            appendLine("  • Servicio vinculado: $isServiceBound")
            appendLine("  • Servicio disponible: ${musicService != null}")
            appendLine("  • Reproduciendo: ${isPlaying()}")
            appendLine("  • Posición: ${getCurrentPosition() / 1000}s")
            appendLine("  • Duración: ${getDuration() / 1000}s")
        }
    }

    /**
     * Log del estado actual
     */
    fun logStatus() {
        Log.d(TAG, getDetailedStatus())
    }
}