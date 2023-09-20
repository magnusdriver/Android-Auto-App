package com.example.autoapp

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.security.AccessController.getContext

class HelloWorldSession: Session(), DefaultLifecycleObserver {

    override fun onCreateScreen(intent: Intent): Screen {
        lifecycle.addObserver(this)

        return HelloWorldScreen(carContext)
    }

    override fun onDestroy(owner: LifecycleOwner) {

        super.onDestroy(this)
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        // Respond to configuration changes
    }

    override fun onNewIntent(intent: Intent) {
        // Handle intent
    }


}