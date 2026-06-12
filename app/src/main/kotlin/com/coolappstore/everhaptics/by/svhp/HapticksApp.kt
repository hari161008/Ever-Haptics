package com.coolappstore.everhaptics.by.svhp

import android.app.Application
import com.coolappstore.everhaptics.by.svhp.data.HapticsPreferences
import com.coolappstore.everhaptics.by.svhp.haptics.HapticEngine

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
