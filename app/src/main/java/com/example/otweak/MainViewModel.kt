package com.example.otweak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _clickedWallpapers = MutableStateFlow(false)
    val clickedWallpapers: StateFlow<Boolean> = _clickedWallpapers.asStateFlow()

    val isGlowEffectEnabled: StateFlow<Boolean> = TweakRepository.isGlowEffectEnabled
    val isWidgetTransparencyEnabled: StateFlow<Boolean> = TweakRepository.isWidgetTransparencyEnabled
    val isForceRotateDisabled: StateFlow<Boolean> = TweakRepository.isForceRotateDisabled
    
    val clockStyleIndex = TweakRepository.clockStyleIndex
    val clockColorHex = TweakRepository.clockColorHex
    val clockSubColorHex = TweakRepository.clockSubColorHex
    val clockAlpha = TweakRepository.clockAlpha

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
    fun markWallpapersClicked() { _clickedWallpapers.value = true }

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
