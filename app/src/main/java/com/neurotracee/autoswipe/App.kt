package com.neurotracee.autoswipe

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        LogBook.init(this)
        LogBook.installCrashHandler(this)
        if (LogBook.lastCrashAt > 0) AppState.crashNotice = true
    }
}
