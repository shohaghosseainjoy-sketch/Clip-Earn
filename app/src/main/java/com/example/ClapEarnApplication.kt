package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class ClapEarnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDT1Wua3wSSafas7E6BKtsap5Srerw3-eE")
                        .setApplicationId("1:42422643851:android:777741ed77d8432dfcb88e")
                        .setProjectId("gen-lang-client-0386476779")
                        .setDatabaseUrl("https://gen-lang-client-0386476779-default-rtdb.firebaseio.com")
                        .setStorageBucket("gen-lang-client-0386476779.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("ClapEarnApplication", "Successfully initialized default FirebaseApp programmatic options.")
                } catch (e: Exception) {
                    Log.e("ClapEarnApplication", "Failed to initialize programmatic app, trying context default: ${e.message}")
                    FirebaseApp.initializeApp(this)
                }
            }
        } catch (e: Exception) {
            Log.e("ClapEarnApplication", "FirebaseApp initialization completely failed: ${e.message}")
        }
    }
}
