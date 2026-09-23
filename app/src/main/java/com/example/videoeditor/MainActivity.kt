package com.example.videoeditor

import android.Manifest
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videoeditor.editor.VideoEditorViewModel
import com.example.videoeditor.processing.SlideshowExporter
import com.example.videoeditor.ui.screens.EditorScreen
import com.example.videoeditor.ui.screens.HomeScreen
import com.example.videoeditor.ui.screens.ImageToVideoScreen
import com.example.videoeditor.ui.screens.IntroScreen
import com.example.videoeditor.ui.screens.SettingsScreen
import com.example.videoeditor.ui.screens.TextToVideoScreen
import com.example.videoeditor.ui.theme.VideoEditorTheme

@UnstableApi
class MainActivity : ComponentActivity() {

    private val viewModel: VideoEditorViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results not individually inspected; pickers will simply fail silently if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // System-level splash (instant icon on a themed background) shows for the brief
        // moment before Compose is ready; the animated IntroScreen below takes over
        // immediately after and does the real "motion" branding.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestMediaPermissions()

        setContent {
            VideoEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "intro") {
                        composable("intro") {
                            IntroScreen(onFinished = {
                                navController.navigate("home") {
                                    popUpTo("intro") { inclusive = true }
                                }
                            })
                        }
                        composable("home") {
                            HomeScreen(
                                onVideoPicked = { uri ->
                                    val duration = readDurationMs(uri)
                                    viewModel.loadSource(uri, duration)
                                    navController.navigate("editor")
                                },
                                onImageToVideo = { navController.navigate("image_to_video") },
                                onTextToVideo = { navController.navigate("text_to_video") },
                                onSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("editor") {
                            EditorScreen(viewModel)
                        }
                        composable("image_to_video") {
                            ImageToVideoScreen(onBuilt = { clips ->
                                Toast.makeText(this@MainActivity, "Exporting slideshow…", Toast.LENGTH_SHORT).show()
                                SlideshowExporter.export(
                                    context = this@MainActivity,
                                    clips = clips,
                                    onComplete = { uri ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            if (uri != null) "Saved to Movies/VideoEditor" else "Export failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onError = { msg ->
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            })
                        }
                        composable("text_to_video") {
                            TextToVideoScreen(
                                onGenerated = { uri ->
                                    val duration = readDurationMs(uri)
                                    viewModel.loadSource(uri, duration)
                                    navController.navigate("editor")
                                },
                                onOpenSettings = { navController.navigate("settings") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    private fun requestMediaPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    private fun readDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
