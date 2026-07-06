package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FirebaseManager provides a unified architecture for our cloud syncing foundation.
 * This class prepares Firebase Authentication, Firestore database synchronization,
 * and Cloud Messaging for notifications, compiling cleanly for Phase 1.
 */
class FirebaseManager private constructor(private val context: Context) {

    // Prepared Firebase instance placeholders
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firebaseMessaging: FirebaseMessaging? = null

    init {
        try {
            // Attempt standard setup. If google-services.json is missing, this will fail gracefully.
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            firebaseMessaging = FirebaseMessaging.getInstance()
            Log.d("FirebaseManager", "Firebase services initialized successfully.")
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Firebase initialization deferred. Add google-services.json to activate. " + e.localizedMessage)
        }
    }

    /**
     * Checks if cloud sync is authenticated.
     */
    fun isUserSignedIn(): Boolean {
        return firebaseAuth?.currentUser != null
    }

    /**
     * Retrieve the current authenticated user's ID.
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth?.currentUser?.uid
    }

    /**
     * Dummy trigger for Phase 2 Firestore sync.
     */
    fun syncDataToCloud(callback: (Boolean) -> Unit) {
        Log.d("FirebaseManager", "Cloud sync triggered. Awaiting Firestore active state in Phase 2.")
        callback(true)
    }

    /**
     * Retrieve and cache FCM Token for Cloud Messaging.
     */
    fun retrieveFcmToken(onTokenResult: (String?) -> Unit) {
        val messaging = firebaseMessaging
        if (messaging != null) {
            messaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FirebaseManager", "Fetching FCM registration token failed", task.exception)
                    onTokenResult(null)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("FirebaseManager", "FCM Token: $token")
                onTokenResult(token)
            }
        } else {
            Log.w("FirebaseManager", "FCM not initialized. Please configure Google Services plugin.")
            onTokenResult(null)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
