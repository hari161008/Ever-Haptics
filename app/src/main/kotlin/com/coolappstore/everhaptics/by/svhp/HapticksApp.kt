package com.coolappstore.everhaptics.by.svhp

import android.app.Application
import com.coolappstore.everhaptics.by.svhp.data.HapticsPreferences
import com.coolappstore.everhaptics.by.svhp.haptics.HapticEngine
import com.coolappstore.everhaptics.by.svhp.xposed.XposedEdgeRemotePrefs
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HapticksApp : Application(), XposedServiceHelper.OnServiceListener {

    val preferences: HapticsPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HapticsPreferences(this)
    }

    val hapticEngine: HapticEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HapticEngine(this)
    }

    @Volatile
    var xposedService: XposedService? = null
        private set

    private val _xposedServiceConnected = MutableStateFlow(false)
    val xposedServiceConnected: StateFlow<Boolean> = _xposedServiceConnected.asStateFlow()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
        appScope.launch {
            preferences.settings
                .distinctUntilChanged()
                .collect { settings ->
                    val svc = xposedService ?: return@collect
                    withContext(Dispatchers.IO) {
                        XposedEdgeRemotePrefs.push(svc, settings)
                    }
                }
        }
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        _xposedServiceConnected.value = true
        appScope.launch {
            val settings = try {
                preferences.settings.first()
            } catch (_: Throwable) {
                return@launch
            }
            withContext(Dispatchers.IO) {
                XposedEdgeRemotePrefs.push(service, settings)
            }
        }
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
        _xposedServiceConnected.value = false
    }
}
