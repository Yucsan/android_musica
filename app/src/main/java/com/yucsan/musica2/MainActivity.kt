package com.yucsan.musica2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yucsan.musica2.servicio.MusicPlayer
import com.yucsan.musica2.ui.theme.Musica2Theme
import com.yucsan.musica2.ui.screens.MusicScreen
import com.yucsan.musica2.ui.screens.PermissionHandler
import com.yucsan.musica2.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint // ✅ AGREGAR ESTA LÍNEA


@AndroidEntryPoint // ✅ AGREGAR ESTA LÍNEA
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Musica2Theme {
                MusicApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp() {
    val context = LocalContext.current
    val viewModel: MusicViewModel = viewModel()

    // Crear MusicPlayer y hacer bind solo 1 vez
    val musicPlayer = remember { MusicPlayer(context) }

    LaunchedEffect(Unit) {
        musicPlayer.bindService()
        viewModel.setMusicPlayer(musicPlayer)
    }

    // Desvincular el servicio cuando MusicApp desaparezca
    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.unbindService()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Mi Música")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        PermissionHandler {
            MusicScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MusicAppPreview() {
    Musica2Theme {
        MusicApp()
    }
}