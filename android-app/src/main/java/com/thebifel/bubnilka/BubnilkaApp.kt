package com.thebifel.bubnilka

import android.app.Application
import com.thebifel.bubnilka.runtime.BubnilkaLog

class BubnilkaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BubnilkaLog.state("application_created")
    }
}
