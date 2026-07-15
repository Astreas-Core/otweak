package com.example.otweak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

class MainViewModel : ViewModel() {
    val isShizukuAvailable = ShizukuManager.isShizukuAvailable
    val hasPermission = ShizukuManager.hasPermission

    private val _isTermsAccepted = MutableStateFlow(TweakRepository.isTermsAccepted())
    val isTermsAccepted: StateFlow<Boolean> = _isTermsAccepted.asStateFlow()

    private val _isTelegramJoined = MutableStateFlow(TweakRepository.isTelegramJoined())
    val isTelegramJoined: StateFlow<Boolean> = _isTelegramJoined.asStateFlow()

    private val _clickedSupport = MutableStateFlow(false)
    val clickedSupport: StateFlow<Boolean> = _clickedSupport.asStateFlow()

    private val _clickedHub = MutableStateFlow(false)
    val clickedHub: StateFlow<Boolean> = _clickedHub.asStateFlow()

    val isGlowEffectEnabled: StateFlow<Boolean> = TweakRepository.isGlowEffectEnabled
    val isWidgetTransparencyEnabled: StateFlow<Boolean> = TweakRepository.isWidgetTransparencyEnabled
    val isForceRotateDisabled: StateFlow<Boolean> = TweakRepository.isForceRotateDisabled
    
    val clockStyleIndex = TweakRepository.clockStyleIndex
    val clockColorHex = TweakRepository.clockColorHex
    val clockSubColorHex = TweakRepository.clockSubColorHex
    val clockAlpha = TweakRepository.clockAlpha

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()
    
    private val _updateUrl = MutableStateFlow<String?>(null)
    val updateUrl: StateFlow<String?> = _updateUrl.asStateFlow()

    init {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/Astreas-Core/otweak/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val tagName = jsonObject.getString("tag_name")
                    val currentVersion = BuildConfig.VERSION_NAME
                    
                    val cleanTagName = tagName.removePrefix("v")
                    val cleanCurrent = currentVersion.removePrefix("v")
                    
                    if (cleanTagName != cleanCurrent) {
                        _updateAvailable.value = true
                        _updateUrl.value = jsonObject.getString("html_url")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun refreshTweakStatuses() {
        viewModelScope.launch {
            if (ShizukuManager.hasPermission.value) {
                TweakRepository.applySavedSettings()
                TweakRepository.checkGlowEffectStatus()
                TweakRepository.checkWidgetTransparencyStatus()
                TweakRepository.checkForceRotateStatus()
                TweakRepository.checkClockCustomization()
            }
        }
    }

    fun acceptTerms() {
        TweakRepository.setTermsAccepted(true)
        _isTermsAccepted.value = true
    }

    fun markSupportClicked() { _clickedSupport.value = true }
    fun markHubClicked() { _clickedHub.value = true }

    fun completeTelegramOnboarding() {
        TweakRepository.setTelegramJoined(true)
        _isTelegramJoined.value = true
    }

    fun setGlowEffect(enabled: Boolean) {
        viewModelScope.launch {
            TweakRepository.setGlowEffect(enabled)
        }
    }

    fun setWidgetTransparency(enabled: Boolean) {
        viewModelScope.launch {
            TweakRepository.setWidgetTransparency(enabled)
        }
    }

    fun setForceRotateDisabled(disabled: Boolean) {
        viewModelScope.launch {
            TweakRepository.setForceRotateDisabled(disabled)
        }
    }

    fun setClockCustomization(styleIndex: Int, colorHex: String, subColorHex: String, alpha: Float, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = TweakRepository.setClockCustomization(styleIndex, colorHex, subColorHex, alpha)
            onResult(result.isSuccess)
        }
    }
}
