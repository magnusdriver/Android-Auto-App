package com.example.autoapp

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Binder
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.car.app.CarAppService
import androidx.car.app.CarToast
import androidx.car.app.Session
import androidx.car.app.connection.CarConnection
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.Lifecycle


class HelloWorldService: CarAppService() {
    private var connState: Boolean = false // This variable controls the car-device connection status.


    // Singleton object for giving access to variables or functions from out of this class. (Alternative to Intents)
    companion object {
        var sInstance: HelloWorldService? = null
    }

    override fun onCreateSession(): Session {
        sInstance = this
        connState = true
        return HelloWorldSession()
    }

    override fun onDestroy(): Unit {
        connState = false
        super.onDestroy()
    }

    override fun createHostValidator(): HostValidator {

        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }


    private fun onConnectionStateUpdated(connectionState: Int) {
        val message = when(connectionState) {
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> "Not connected to a head unit"
            CarConnection.CONNECTION_TYPE_NATIVE -> "Connected to Android Automotive OS"
            CarConnection.CONNECTION_TYPE_PROJECTION -> "Connected to Android Auto"
            else -> "Unknown car connection type"
        }
        if(this.currentSession!!.lifecycle.currentState == Lifecycle.State.CREATED) {
            CarToast.makeText(this.currentSession!!.carContext, message, CarToast.LENGTH_SHORT)
                .show()
        }
        connState = connectionState != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
    }

    public fun getConnStatus(): Boolean {
        return connState
    }
}