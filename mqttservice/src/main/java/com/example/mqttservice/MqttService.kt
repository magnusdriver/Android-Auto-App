package com.example.mqttservice

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import helpers.MyBroadcastReceiver
import helpers.NotificationHelper
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttService: Service() {

    private lateinit var notificationClient: MqttAndroidClient

    private lateinit var notificationHelperMqttService: NotificationHelper


    override fun onCreate() {
        super.onCreate()

        try {
            notificationHelperMqttService = NotificationHelper(applicationContext)
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating channel", Toast.LENGTH_LONG).show()
        }

        notificationHelperMqttService.createNotificationChannel("com.example.masmovilcarapp.mqtt_service")


        var myCallback = object: MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val msg = "Receive message: ${message.toString()} from topic: $topic"
                Log.d(this.javaClass.name, msg)

                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()

                notificationHelperMqttService.buildNotification(NotificationHelper.json2NotificationModel(message.toString()))
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(this.javaClass.name, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(this.javaClass.name, "Delivery complete")
            }
        }

        var myActionListener = object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(this.javaClass.name, "Connection success")

                Toast.makeText(applicationContext, "MQTT Connection success", Toast.LENGTH_LONG).show()
                var topic: String = "test/" + getString(R.string.client_id)
                notificationClient.subscribe(topic, 1, null, null)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(this.javaClass.name, "Connection failure: ${exception.toString()}")

                Toast.makeText(applicationContext, "MQTT Connection fails: ${exception.toString()}",
                    Toast.LENGTH_LONG).show()
            }
        }

        notificationClient = MqttAndroidClient(applicationContext, getString(R.string.mqtt_broker_ip), getString(R.string.client_id))

        var connectionOptions = MqttConnectOptions()
        connectionOptions.userName = getString(R.string.mqtt_user)
        connectionOptions.password = getString(R.string.mqtt_passwd).toCharArray()

        notificationClient.connect(connectionOptions, myCallback, myActionListener) 

        notificationClient.setCallback(myCallback)

    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        notificationClient.disconnect()

        super.onDestroy()
    }
}
