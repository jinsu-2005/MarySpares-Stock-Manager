package com.marytwowheelers.spares

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.marytwowheelers.spares.sync.SyncManager

class MarySparesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Automatically trigger cloud sync whenever user authentication state changes to logged in
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                SyncManager.enqueueSync(applicationContext)
            }
        }
    }
}

