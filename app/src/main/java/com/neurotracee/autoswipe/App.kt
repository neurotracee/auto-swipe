package com.neurotracee.autoswipe

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
    }
}
