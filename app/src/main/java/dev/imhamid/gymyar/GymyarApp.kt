package dev.imhamid.gymyar

import android.app.Application

class GymyarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GymyarApp
            private set
    }
}
