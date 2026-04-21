package com.los_jorges.plan_bar

import android.app.Application
import com.los_jorges.plan_bar.session.SessionManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
