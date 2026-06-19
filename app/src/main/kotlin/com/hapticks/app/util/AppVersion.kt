package com.hapticks.app.util

import android.content.Context

object AppVersion {
    /** Returns the versionName from the installed APK — always accurate, no generated class needed. */
    fun get(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "5.0.0"
        } catch (_: Exception) {
            "5.0.0"
        }
}
