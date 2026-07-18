package com.example.otweak

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import android.content.Context
import android.content.SharedPreferences

object TweakRepository {

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("otweak_prefs", Context.MODE_PRIVATE)
        loadThemeSettings()
    }

    fun isTermsAccepted(): Boolean {
        return prefs.getBoolean("terms_accepted", false)
    }

    fun setTermsAccepted(accepted: Boolean) {
        prefs.edit().putBoolean("terms_accepted", accepted).apply()
    }

    fun isTelegramJoined(): Boolean {
        return prefs.getBoolean("telegram_joined", false)
    }

    fun setTelegramJoined(joined: Boolean) {
        prefs.edit().putBoolean("telegram_joined", joined).apply()
    }

    private val _isGlowEffectEnabled = MutableStateFlow(false)
    val isGlowEffectEnabled: StateFlow<Boolean> = _isGlowEffectEnabled.asStateFlow()

    private val _isWidgetTransparencyEnabled = MutableStateFlow(false)
    val isWidgetTransparencyEnabled: StateFlow<Boolean> = _isWidgetTransparencyEnabled.asStateFlow()

    private val _isForceRotateDisabled = MutableStateFlow(false)
    val isForceRotateDisabled: StateFlow<Boolean> = _isForceRotateDisabled.asStateFlow()

    private val _clockStyleIndex = MutableStateFlow(0)
    val clockStyleIndex: StateFlow<Int> = _clockStyleIndex.asStateFlow()

    private val _clockColorHex = MutableStateFlow("ffffff") // Main color
    val clockColorHex: StateFlow<String> = _clockColorHex.asStateFlow()

    private val _clockSubColorHex = MutableStateFlow("ffffff") // Sub color
    val clockSubColorHex: StateFlow<String> = _clockSubColorHex.asStateFlow()

    private val _clockAlpha = MutableStateFlow(0.8f) // 0.0 to 1.0
    val clockAlpha: StateFlow<Float> = _clockAlpha.asStateFlow()

    val clockStyles = listOf(
        """{"info_widget":{"countdown":{"currentTime":0,"countdown_lunar_flag":false},"info_type":"com.vivo.systemuiplugin/none","slogan":""},"shortcut":{"left_tool":"phone","right_tool":"camera"},"style_id":22,"time_view":{"clock_font":{"font_id":"6","font_path":"/system/fonts/vivoSansClockHAVF.ttf","font_weight":600},"front_type":0,"glass":0,"location":{"align":2,"col_count":4,"row_count":4,"start_col":0,"start_row":0},"main_color":{"progress":0,"fixed_color":"#ff000000","type_id":-4},"style_id":"s7-4x4","sub_color":{"progress":0,"type_id":-1},"unfold_location":{"align":2,"col_count":4,"row_count":4,"start_col":2,"start_row":0},"version":"3"},"version":3}""",
        """{"info_widget":{"countdown":{"currentTime":0,"countdown_lunar_flag":false},"info_type":"com.vivo.systemuiplugin/none","slogan":""},"shortcut":{"left_tool":"phone","right_tool":"camera"},"style_id":22,"time_view":{"clock_font":{"font_id":"6","font_path":"/system/fonts/vivoSansClockHAVF.ttf","font_weight":600},"front_type":0,"glass":0,"location":{"align":0,"col_count":2,"row_count":5,"start_col":0,"start_row":0},"main_color":{"progress":0,"fixed_color":"#ffff0000","type_id":-4},"style_id":"s8-2x5","sub_color":{"progress":0,"fixed_color":"#ffffe600","type_id":-4},"unfold_location":{"align":0,"col_count":2,"row_count":5,"start_col":0,"start_row":0},"version":"3"},"version":3}""",
        """{"info_widget":{"countdown":{"currentTime":0,"countdown_lunar_flag":false},"info_type":"com.vivo.systemuiplugin/none","slogan":""},"shortcut":{"left_tool":"phone","right_tool":"camera"},"style_id":22,"time_view":{"clock_font":{"font_id":"6","font_path":"/system/fonts/vivoSansClockHAVF.ttf","font_weight":600},"front_type":0,"glass":0,"location":{"align":0,"col_count":2,"row_count":5,"start_col":0,"start_row":0},"main_color":{"progress":0,"fixed_color":"#fffff500","type_id":-4},"style_id":"s9-2x5","sub_color":{"progress":0,"fixed_color":"#ffff0000","type_id":-4},"unfold_location":{"align":0,"col_count":2,"row_count":5,"start_col":0,"start_row":0},"version":"3"},"version":3}"""
    )

    private val _themeMode = MutableStateFlow(0) // 0: System, 1: Light, 2: Dark
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _blackNightTheme = MutableStateFlow(false)
    val blackNightTheme: StateFlow<Boolean> = _blackNightTheme.asStateFlow()

    private val _useSystemThemeColor = MutableStateFlow(true)
    val useSystemThemeColor: StateFlow<Boolean> = _useSystemThemeColor.asStateFlow()

    suspend fun applySavedSettings() {
        if (prefs.getBoolean("glow_effect", false)) {
            setGlowEffect(true)
        }
        if (prefs.getBoolean("widget_transparency", false)) {
            setWidgetTransparency(true)
        }
        if (prefs.getBoolean("force_rotate_disabled", false)) {
            setForceRotateDisabled(true)
        }
        val savedStyle = prefs.getInt("clock_style_index", -1)
        if (savedStyle != -1) {
            val savedColor = prefs.getString("clock_color_hex", "ffffff") ?: "ffffff"
            val savedSubColor = prefs.getString("clock_sub_color_hex", "ffffff") ?: "ffffff"
            val savedAlpha = prefs.getFloat("clock_alpha", 0.8f)
            setClockCustomization(savedStyle, savedColor, savedSubColor, savedAlpha)
        }
    }

    suspend fun checkGlowEffectStatus() {
        val result = ShizukuManager.executeCommand("settings get system realtime_blur_state")
        result.onSuccess { value ->
            if (value == "1") {
                _isGlowEffectEnabled.value = true
            } else if (value == "null") {
                val contentResult = ShizukuManager.executeCommand("content query --uri content://settings/system --where \"name='realtime_blur_state'\"")
                contentResult.onSuccess { contentValue ->
                     _isGlowEffectEnabled.value = contentValue.contains("value=1")
                }
            } else {
                _isGlowEffectEnabled.value = false
            }
        }.onFailure {
             _isGlowEffectEnabled.value = false
        }
    }

    suspend fun setGlowEffect(enabled: Boolean): Result<Unit> {
        val value = if (enabled) 1 else 0
        val result = ShizukuManager.executeCommand("content insert --uri content://settings/system --bind name:s:realtime_blur_state --bind value:i:$value")
        return result.map {
            prefs.edit().putBoolean("glow_effect", enabled).apply()
            _isGlowEffectEnabled.value = enabled
        }
    }

    suspend fun checkWidgetTransparencyStatus() {
        val result = ShizukuManager.executeCommand("settings get secure launcher_widget_support_blur")
        result.onSuccess { value ->
            if (value == "1") {
                _isWidgetTransparencyEnabled.value = true
            } else if (value == "null") {
                val contentResult = ShizukuManager.executeCommand("content query --uri content://settings/secure --where \"name='launcher_widget_support_blur'\"")
                contentResult.onSuccess { contentValue ->
                     _isWidgetTransparencyEnabled.value = contentValue.contains("value=1")
                }
            } else {
                _isWidgetTransparencyEnabled.value = false
            }
        }.onFailure {
             _isWidgetTransparencyEnabled.value = false
        }
    }

    suspend fun setWidgetTransparency(enabled: Boolean): Result<Unit> {
        val value = if (enabled) 1 else 0
        val result = ShizukuManager.executeCommand("content insert --uri content://settings/secure --bind name:s:launcher_widget_support_blur --bind value:i:$value")
        return result.map {
            prefs.edit().putBoolean("widget_transparency", enabled).apply()
            _isWidgetTransparencyEnabled.value = enabled
        }
    }

    suspend fun checkForceRotateStatus() {
        val result = ShizukuManager.executeCommand("settings get secure show_rotation_suggestions")
        result.onSuccess { value ->
            if (value == "0") {
                _isForceRotateDisabled.value = true
            } else {
                _isForceRotateDisabled.value = false
            }
        }.onFailure {
             _isForceRotateDisabled.value = false
        }
    }

    suspend fun setForceRotateDisabled(disabled: Boolean): Result<Unit> {
        val value = if (disabled) 0 else 1
        val result = ShizukuManager.executeCommand("content insert --uri content://settings/secure --bind name:s:show_rotation_suggestions --bind value:i:$value")
        return result.map {
            prefs.edit().putBoolean("force_rotate_disabled", disabled).apply()
            _isForceRotateDisabled.value = disabled
        }
    }

    suspend fun checkClockCustomization() {
        val result = ShizukuManager.executeCommand("settings get secure lock_screen_theme_id_os")
        result.onSuccess { jsonString ->
            if (jsonString != "null" && jsonString.isNotBlank()) {
                try {
                    val json = JSONObject(jsonString)
                    val timeView = json.getJSONObject("time_view")
                    val styleId = timeView.getString("style_id")
                    
                    when (styleId) {
                        "s7-4x4", "s7-4x3" -> _clockStyleIndex.value = 0
                        "s8-2x5" -> _clockStyleIndex.value = 1
                        "s9-2x5" -> _clockStyleIndex.value = 2
                    }

                    val mainColor = timeView.getJSONObject("main_color")
                    if (mainColor.has("fixed_color")) {
                        val colorStr = mainColor.getString("fixed_color") // e.g. #80ffffff
                        if (colorStr.length == 9) {
                            val alphaHex = colorStr.substring(1, 3)
                            val rgbHex = colorStr.substring(3)
                            _clockAlpha.value = alphaHex.toInt(16) / 255f
                            _clockColorHex.value = rgbHex
                        }
                    }

                    if (timeView.has("sub_color")) {
                        val subColor = timeView.getJSONObject("sub_color")
                        if (subColor.has("fixed_color")) {
                            val subColorStr = subColor.getString("fixed_color")
                            if (subColorStr.length == 9) {
                                _clockSubColorHex.value = subColorStr.substring(3)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TweakRepository", "Failed to parse lock_screen_theme_id_os", e)
                }
            }
        }
    }

    suspend fun setClockCustomization(styleIndex: Int, colorHex: String, subColorHex: String, alpha: Float): Result<Unit> {
        return try {
            val templateStr = clockStyles.getOrElse(styleIndex) { clockStyles[0] }
            val json = JSONObject(templateStr)
            
            val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
            val alphaHex = String.format("%02x", alphaInt)
            val fullMainColor = "#${alphaHex}${colorHex}"
            val fullSubColor = "#${alphaHex}${subColorHex}"

            val timeView = json.getJSONObject("time_view")
            
            val mainColor = timeView.getJSONObject("main_color")
            if (mainColor.has("fixed_color") || mainColor.getInt("type_id") == -4) {
                mainColor.put("fixed_color", fullMainColor)
                mainColor.put("type_id", -4)
            }
            
            if (timeView.has("sub_color")) {
                val subColor = timeView.getJSONObject("sub_color")
                if (subColor.has("fixed_color") || subColor.getInt("type_id") == -4) {
                    subColor.put("fixed_color", fullSubColor)
                    subColor.put("type_id", -4)
                }
            }
            
            val newJsonStr = json.toString().replace("\\/", "/")
            
            // Grant ourselves permission via Shizuku
            ShizukuManager.executeCommand("pm grant com.otweak.app android.permission.WRITE_SECURE_SETTINGS")
            
            // Write directly using native Android API (exactly what SetEdit does)
            val success = android.provider.Settings.Secure.putString(
                appContext.contentResolver,
                "lock_screen_theme_id_os",
                newJsonStr
            )
            
            if (success) {
                prefs.edit()
                    .putInt("clock_style_index", styleIndex)
                    .putString("clock_color_hex", colorHex)
                    .putString("clock_sub_color_hex", subColorHex)
                    .putFloat("clock_alpha", alpha)
                    .apply()
                _clockStyleIndex.value = styleIndex
                _clockColorHex.value = colorHex
                _clockSubColorHex.value = subColorHex
                _clockAlpha.value = alpha
                return Result.success(Unit)
            } else {
                return Result.failure(Exception("Settings.Secure.putString returned false"))
            }
        } catch (e: Exception) {
            Log.e("TweakRepository", "Failed to set clock customization", e)
            Result.failure(e)
        }
    }

    fun loadThemeSettings() {
        _themeMode.value = prefs.getInt("theme_mode", 0)
        _blackNightTheme.value = prefs.getBoolean("black_night_theme", false)
        _useSystemThemeColor.value = prefs.getBoolean("use_system_theme_color", true)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setBlackNightTheme(enabled: Boolean) {
        prefs.edit().putBoolean("black_night_theme", enabled).apply()
        _blackNightTheme.value = enabled
    }

    fun setUseSystemThemeColor(enabled: Boolean) {
        prefs.edit().putBoolean("use_system_theme_color", enabled).apply()
        _useSystemThemeColor.value = enabled
    }
}
