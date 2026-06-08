package com.opendesign

import android.app.Application
import com.opendesign.data.db.AppDatabase

class OpenDesignApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: OpenDesignApp
            private set
    }
}
