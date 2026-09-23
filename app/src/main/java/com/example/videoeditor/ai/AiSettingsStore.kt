package com.example.videoeditor.ai

import android.content.Context

/**
 * Wan 2.1 (https://github.com/Wan-Video/Wan2.1) is open-weight and unrestricted, but it is
 * NOT something a phone can run — even the smallest 1.3B text-to-video variant needs a
 * discrete GPU with several GB of VRAM. "Your own AI platform" in practice means: you host
 * Wan 2.1 yourself (a cloud GPU box, a home PC with a good GPU, ComfyUI with the Wan nodes,
 * or a small FastAPI wrapper around the official inference scripts) and this app just calls
 * that server over HTTP. This class stores the endpoint you point it at.
 */
class AiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    companion object {
        private const val KEY_BASE_URL = "wan_base_url"
        private const val KEY_API_KEY = "wan_api_key"
    }
}
