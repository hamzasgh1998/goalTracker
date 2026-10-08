package com.milestone.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MilestoneApplication : Application() {

    @Inject
    lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        // Kicks off on a background scope; the splash screen waits on isReady.
        appInitializer.initialize()
    }
}
