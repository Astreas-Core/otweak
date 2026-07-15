package com.example.otweak

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuManager {
    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val BINDER_RECEIVED_LISTENER = Shizuku.OnBinderReceivedListener {
        checkAvailability()
        checkPermission()
    }
    
    private val BINDER_DEAD_LISTENER = Shizuku.OnBinderDeadListener {
        checkAvailability()
        checkPermission()
    }
    
    private val REQUEST_PERMISSION_RESULT_LISTENER = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 1 && grantResult == PackageManager.PERMISSION_GRANTED) {
            _hasPermission.value = true
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER)
        Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        checkAvailability()
        checkPermission()
    }

    fun checkAvailability() {
        _isShizukuAvailable.value = Shizuku.pingBinder()
    }

    fun checkPermission() {
        if (!Shizuku.pingBinder()) {
            _hasPermission.value = false
            return
        }
        _hasPermission.value = if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1)
            } else {
                _hasPermission.value = true
            }
        }
    }

    suspend fun executeCommand(command: String): Result<String> {
        return executeCommandArgs(arrayOf("sh", "-c", command))
    }

    suspend fun executeCommandArgs(args: Array<String>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    return@withContext Result.failure(Exception("Shizuku not available or permission denied"))
                }
                
                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                method.isAccessible = true
                val process = method.invoke(null, args, null, null) as Process
                
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                
                if (exitCode == 0) {
                    Result.success(output.trim())
                } else {
                    Result.failure(Exception(error.ifEmpty { "Command failed with exit code $exitCode" }))
                }
            } catch (e: Exception) {
                Log.e("ShizukuManager", "Error executing command", e)
                Result.failure(e)
            }
        }
    }
}
