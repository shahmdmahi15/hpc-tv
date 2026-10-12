package com.shahmdmahi.hpctv

import android.app.Application
import android.util.Log

class HpcTvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Safety handler: Prevents unhandled background coroutine exceptions from killing the TV app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("HpcTvApp", "Uncaught exception on thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
