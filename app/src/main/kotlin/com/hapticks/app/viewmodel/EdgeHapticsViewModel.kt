package com.hapticks.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsPreferences
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.edge.EdgeHapticsBridge
import com.hapticks.app.haptics.HapticPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EdgeHapticsViewModel(
    application: Application,
    private val preferences: HapticsPreferences,
) : AndroidViewModel(application) {

    sealed class TestEvent {
        object Fired : TestEvent()
        object NoVibrator : TestEvent()
    }

    val settings: StateFlow<HapticsSettings> = preferences.settings
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HapticsSettings.Default,
        )

    private val _testEvent = MutableStateFlow<TestEvent?>(null)
    val testEvent: StateFlow<TestEvent?> = _testEvent.asStateFlow()

    private val hapticksApp: HapticksApp = application as HapticksApp

    /** True while the LibXposed service bridge is bound (LSPosed runtime is talking to this app). */
    val isLsposedXposedBridgeActive: StateFlow<Boolean> = hapticksApp.xposedServiceConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = hapticksApp.xposedServiceConnected.value,
        )

    fun setEdgePattern(pattern: HapticPattern) {
        viewModelScope.launch {
            preferences.setEdgePattern(pattern)
        }
    }

    fun setEdgeIntensity(intensity: Float) {
        viewModelScope.launch {
            preferences.setEdgeIntensity(intensity)
        }
    }

    fun setA11yScrollBoundEdge(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setA11yScrollBoundEdge(enabled)
            if (enabled) preferences.setEdgeLsposedLibxposedPath(false)
        }
    }

    fun setEdgeLsposedLibxposedPath(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEdgeLsposedLibxposedPath(enabled)
            if (enabled) preferences.setA11yScrollBoundEdge(false)
        }
    }

    /** Edge waveform test (dedicated test button only). */
    fun testEdgeHaptic() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                EdgeHapticsBridge.testEdgeHaptic(getApplication())
            }
            _testEvent.value = when (result) {
                EdgeHapticsBridge.TestResult.Fired -> TestEvent.Fired
                EdgeHapticsBridge.TestResult.NoVibrator -> TestEvent.NoVibrator
            }
        }
    }

    fun consumeTestEvent() {
        _testEvent.value = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as HapticksApp
                return EdgeHapticsViewModel(app, app.preferences) as T
            }
        }
    }
}
