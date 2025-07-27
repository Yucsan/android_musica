package com.yucsan.musica2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.yucsan.musica2.MainActivity
import com.yucsan.musica2.R
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.modelo.uri

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val NOTIFICATION_ID = 1

        // Acciones para los botones de la notificación
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentSong: Song? = null
    private var isServiceStarted = false

    // Binder para comunicación con la actividad
    private val musicBinder = MusicBinder()

    // Interface para comunicar cambios a la UI
    interface MusicServiceListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onSongChanged(song: Song?)
        fun onProgressUpdate(currentPosition: Long, duration: Long)
    }

    private var serviceListener: MusicServiceListener? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        createNotificationChannel()
        initializeMediaSession()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            // Listener para cambios en el estado de reproducción
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            serviceListener?.onPlaybackStateChanged(isPlaying)
                            updateNotification()
                        }
                        Player.STATE_ENDED -> {
                            // Canción terminó, reproducir siguiente
                            serviceListener?.onPlaybackStateChanged(false)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    serviceListener?.onPlaybackStateChanged(isPlaying)
                    updateNotification()

                    // Actualizar progreso si está reproduciendo
                    if (isPlaying) {
                        startProgressUpdates()
                    }
                }
            })
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproductor de Música",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles del reproductor de música"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaSession() {
        // Simplificamos por ahora, se puede mejorar después
    }

    override fun onBind(intent: Intent?): IBinder {
        return musicBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resumeMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_NEXT -> nextSong()
            ACTION_PREVIOUS -> previousSong()
            ACTION_STOP -> stopService()
        }
        return START_STICKY
    }

    // Métodos públicos para controlar la reproducción
    fun setServiceListener(listener: MusicServiceListener) {
        this.serviceListener = listener
    }



    fun playSong(song: Song) {
        Log.d("MusicService", "🎵 INTENTANDO REPRODUCIR:")
        Log.d("MusicService", "   Título: ${song.title}")
        Log.d("MusicService", "   Artista: ${song.artist}")
        Log.d("MusicService", "   YouTube URL: ${song.youtubeUrl}")
        Log.d("MusicService", "   Audio URL: ${song.audioUrl}")

        // Obtener URI de forma segura
        val mediaUri = song.uri
        if (mediaUri == null) {
            Log.e("MusicService", "❌ URI es null, no se puede reproducir")
            return
        }

        Log.d("MusicService", "   URI que usará ExoPlayer: $mediaUri")

        // Verificar si es URL real de YouTube o de prueba
        when {
            song.audioUrl?.contains("youtube.com") == true || song.audioUrl?.contains("googlevideo.com") == true -> {
                Log.d("MusicService", "✅ USANDO URL REAL DE YOUTUBE")
            }
            song.audioUrl?.contains("soundhelix.com") == true -> {
                Log.w("MusicService", "⚠️ USANDO URL DE PRUEBA")
            }
            song.audioUrl.isNullOrEmpty() -> {
                Log.e("MusicService", "❌ URL DE AUDIO VACÍA O NULL")
            }
            else -> {
                Log.d("MusicService", "🔍 URL desconocida: ${song.audioUrl}")
            }
        }

        currentSong = song

        try {
            val mediaItem = MediaItem.fromUri(mediaUri)

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }

            startForegroundService()
            serviceListener?.onSongChanged(song)

        } catch (e: Exception) {
            Log.e("MusicService", "❌ ERROR REPRODUCIENDO:", e)
        }
    }


    fun pauseMusic() {
        exoPlayer?.playWhenReady = false
    }

    fun resumeMusic() {
        exoPlayer?.playWhenReady = true
    }

    fun stopMusic() {
        exoPlayer?.stop()
        stopForeground(true)
        isServiceStarted = false
    }

    fun nextSong() {
        // Esta función será llamada desde el ViewModel
        // para cambiar a la siguiente canción
    }

    fun previousSong() {
        // Esta función será llamada desde el ViewModel
        // para cambiar a la canción anterior
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    private fun startForegroundService() {
        if (!isServiceStarted) {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            isServiceStarted = true
        }
    }

    private fun updateNotification() {
        if (isServiceStarted) {
            val notification = createNotification()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val song = currentSong
        val isPlaying = exoPlayer?.isPlaying ?: false

        // Intent para abrir la app al tocar la notificación
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intents para los botones de control
        val playPauseIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MusicService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MusicService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "Sin título")
            .setContentText(song?.artist ?: "Artista desconocido")
            .setSmallIcon(R.drawable.ic_music_note) // Necesitarás crear este icono
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, MusicService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_skip_previous,
                "Anterior",
                previousIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) "Pausar" else "Reproducir",
                playPauseIntent
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Siguiente",
                nextIntent
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun startProgressUpdates() {
        // Actualizar progreso cada segundo
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                if (exoPlayer?.isPlaying == true) {
                    serviceListener?.onProgressUpdate(
                        getCurrentPosition(),
                        getDuration()
                    )
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }

    private fun stopService() {
        stopMusic()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        mediaSession?.release()
        serviceListener = null
    }
}