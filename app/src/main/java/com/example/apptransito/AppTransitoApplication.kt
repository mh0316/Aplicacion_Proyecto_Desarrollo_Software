package com.example.apptransito

import TokenManager
import android.annotation.SuppressLint
import android.app.Application

class AppTransitoApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var tokenManager: TokenManager
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar TokenManager
        tokenManager = TokenManager(this)

        // Inicializar RetrofitClient con TokenManager
        RetrofitClient.initialize(tokenManager)
    }
}