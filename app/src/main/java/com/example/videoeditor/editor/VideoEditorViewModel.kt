package com.example.videoeditor.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.videoeditor.processing.VideoProcessor
import com.example.videoeditor.util.MediaStoreUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val processor = VideoProcessor(application)

    fun loadSource(uri: Uri, durationMs: Long) {
        _uiState.value = EditorUiState(
            sourceUri = uri,
            sourceDurationMs = durationMs,
            trimStartMs = 0,
            trimEndMs = durationMs
        )
    }

    fun setTrim(startMs: Long, endMs: Long) {
        _uiState.value = _uiState.value.copy(trimStartMs = startMs, trimEndMs = endMs)
    }

    fun setMuteOriginal(muted: Boolean) {
        _uiState.value = _uiState.value.copy(muteOriginalAudio = muted)
    }

    fun setMusic(spec: MusicTrackSpec?) {
        _uiState.value = _uiState.value.copy(music = spec)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setReverse(reverse: Boolean) {
        _uiState.value = _uiState.value.copy(reverse = reverse)
    }

    fun setColorEffect(effect: ColorEffect) {
        _uiState.value = _uiState.value.copy(colorEffect = effect)
    }

    fun addTextOverlay(spec: TextOverlaySpec) {
        _uiState.value = _uiState.value.copy(textOverlays = _uiState.value.textOverlays + spec)
    }

    fun removeTextOverlay(spec: TextOverlaySpec) {
        _uiState.value = _uiState.value.copy(textOverlays = _uiState.value.textOverlays - spec)
    }

    fun addImageOverlay(spec: ImageOverlaySpec) {
        _uiState.value = _uiState.value.copy(imageOverlays = _uiState.value.imageOverlays + spec)
    }

    fun export() {
        val app = getApplication<android.app.Application>()
        val outFile = File(app.cacheDir, "export_${System.currentTimeMillis()}.mp4")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportProgress = 0f, errorMessage = null)
            processor.export(_uiState.value, outFile).collect { event ->
                when (event) {
                    is VideoProcessor.ExportEvent.Progress ->
                        _uiState.value = _uiState.value.copy(exportProgress = event.fraction)

                    is VideoProcessor.ExportEvent.Completed -> {
                        val savedUri = MediaStoreUtil.saveVideoToGallery(app, outFile, outFile.name)
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportProgress = 1f,
                            exportedUri = savedUri
                        )
                    }

                    is VideoProcessor.ExportEvent.Failed ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            errorMessage = event.message
                        )
                }
            }
        }
    }
}
