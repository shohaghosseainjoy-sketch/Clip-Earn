package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class UpdateChecker(
    private val context: Context,
    private val currentVersionCode: Int
) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to retrieve FirebaseFirestore instance: ${e.message}")
            null
        }
    }

    fun checkForUpdate(onUpdateAvailable: (UpdateInfo) -> Unit) {
        val db = firestore ?: return
        try {
            db.collection("app_config")
                .document("version")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val latestCode = doc.getLong("latestVersionCode")?.toInt() ?: 0
                        val latestVersion = doc.getString("latestVersion") ?: ""
                        val apkUrl = doc.getString("apkDownloadUrl") ?: ""
                        val releaseNotes = doc.getString("releaseNotes") ?: ""
                        val forceUpdate = doc.getBoolean("forceUpdate") ?: false

                        if (latestCode > currentVersionCode) {
                            onUpdateAvailable(
                                UpdateInfo(
                                    latestVersion = latestVersion,
                                    apkUrl = apkUrl,
                                    releaseNotes = releaseNotes,
                                    forceUpdate = forceUpdate
                                )
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UpdateChecker", "Failed to fetch version from firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error during checkForUpdate: ${e.message}")
        }
    }
}

data class UpdateInfo(
    val latestVersion: String,
    val apkUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean
)
