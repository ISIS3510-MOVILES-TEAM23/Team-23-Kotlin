package com.example.team_23_kotlin

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MercandesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        FirebaseCrashlytics.getInstance().log("🚀 Crashlytics initialized successfully")
    }
}
