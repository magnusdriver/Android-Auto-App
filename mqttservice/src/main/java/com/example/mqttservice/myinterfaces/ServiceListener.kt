package com.example.mqttservice.myinterfaces

interface ServiceListener {
    fun onServiceDataReceived(data: String)
}