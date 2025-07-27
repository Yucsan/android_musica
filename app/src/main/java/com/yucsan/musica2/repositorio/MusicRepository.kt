package com.yucsan.musica2.repositorio

import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.service.ModernYouTubeService

class MusicRepository {

    private val youtubeService = ModernYouTubeService()

    suspend fun searchSongs(query: String): List<Song> {
        return if (query.isBlank()) {
            youtubeService.getPopularSongs()
        } else {
            youtubeService.searchSongs(query)
        }
    }

    suspend fun getPopularSongs(): List<Song> {
        return youtubeService.getPopularSongs()
    }

    suspend fun getAudioStreamUrl(videoId: String): String? {
        return youtubeService.getAudioStreamUrl(videoId)
    }
}