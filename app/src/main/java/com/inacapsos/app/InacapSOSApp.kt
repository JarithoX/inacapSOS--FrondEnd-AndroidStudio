package com.inacapsos.app

import android.app.Application
import com.inacapsos.app.core.AppSession

class InacapSOSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSession.create(this)
    }
}
