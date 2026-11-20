package dev.imhamid.gymyar

import android.app.Application
import dev.imhamid.gymyar.work.Reminders

class GymyarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Reminders.createChannel(this)
        Reminders.schedule(this)
    }

    companion object {
        lateinit var instance: GymyarApp
            private set
    }
}
