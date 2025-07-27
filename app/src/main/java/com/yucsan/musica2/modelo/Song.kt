package com.yucsan.musica2.modelo

import android.net.Uri

data class Song(
    val id: String, // YouTube video ID
    val title: String,
    val artist: String,
    val duration: Long, // duración en milisegundos
    val thumbnailUrl: String? = null, // miniatura del video
    val youtubeUrl: String, // URL original de YouTube
    val audioUrl: String? = null // URL del stream de audio (se obtiene dinámicamente)
) {
    // Función para formatear la duración
    fun getFormattedDuration(): String {
        val minutes = duration / 1000 / 60
        val seconds = (duration / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    // URI para el reproductor
    val uri: Uri
        get() = Uri.parse(audioUrl ?: youtubeUrl)
}