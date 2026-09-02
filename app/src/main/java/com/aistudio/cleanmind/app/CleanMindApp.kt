package com.aistudio.cleanmind.app

import android.app.Application
import android.content.Context

class CleanMindApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(applicationContext)
    }

    companion object {
        fun getAppContainer(context: Context): AppContainer {
            return (context.applicationContext as? CleanMindApp)?.appContainer
                ?: DefaultAppContainer(context.applicationContext)
        }
    }
}
