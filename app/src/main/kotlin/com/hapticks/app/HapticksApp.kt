package com.hapticks.app

import android.app.Application
import com.hapticks.app.data.HapticsPreferences
import com.hapticks.app.haptics.HapticEngine

class HapticksApp : Application() {

    val preferences: HapticsPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HapticsPreferences(this)
    }

    val hapticEngine: HapticEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HapticEngine(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
