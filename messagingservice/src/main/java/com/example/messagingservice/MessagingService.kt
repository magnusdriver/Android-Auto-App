package com.example.messagingservice

import PubSub.GoogleCloudPubSubDataSource
import PubSub.GoogleCloudPubSubDataSourceAccounts
import PubSub.GoogleCloudPubSubDataSourceLocations
import PubSub.GoogleCloudPubSubDataSourceNotificacions
import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.messagingservice.models.*
import com.google.gson.Gson
import helpers.EncryptionHelper
import helpers.NotificationHelper
import helpers.SharedNotificationHelper
import java.util.*
import kotlin.collections.ArrayList
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.sql.Timestamp


class MessagingService: Service() {

    inner class MessagingServiceBinder: Binder() {
        val service: MessagingService
            get() = this@MessagingService
    }

    private val gcloudProjectId = "project-id"

    private lateinit var notificationHelperMessagingService: NotificationHelper
    private lateinit var pubSubServerComDataSource: GoogleCloudPubSubDataSource<NotificationModel>
    private lateinit var pubSubNotificationsComDataSource: GoogleCloudPubSubDataSourceNotificacions

    // PubSub DataSource for general communication with server.
    private lateinit var pubSubAccountsComDataSource: GoogleCloudPubSubDataSourceAccounts
    private lateinit var pubSubLocationComDataSource: GoogleCloudPubSubDataSourceLocations

    private lateinit var sharedNotificationHelper: SharedNotificationHelper

    private var wakeLock: PowerManager.WakeLock? = null

    private val binder: Binder = MessagingServiceBinder()

    private var serviceAlive: Boolean = false

    private var loginData: LoginData? = null
    private var locationCampaign: Boolean = false
    private var locationTimer: Timer? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        var sInstance: MessagingService? = null
    }


    override fun onCreate() {
        super.onCreate()

        serviceAlive = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // wakeLock is supposed to by used to avoid the system killing the service. (At least with foreground service, not sure if it works with regular services)
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        var topic = "test"

        try {
            notificationHelperMessagingService = NotificationHelper(applicationContext)
            notificationHelperMessagingService.createNotificationChannel(applicationContext.getString(R.string.channel_id))
        } catch (e: Exception) {
            Log.i("Messaging Service", "Error creating channel")
        }


        sharedNotificationHelper = SharedNotificationHelper(notificationHelperMessagingService)


        pubSubNotificationsComDataSource = GoogleCloudPubSubDataSourceNotificacions(resources.openRawResource(R.raw.subscriber_credentials), gcloudProjectId, topic, applicationContext)
        pubSubAccountsComDataSource = GoogleCloudPubSubDataSourceAccounts(resources.openRawResource(R.raw.subscriber_credentials), gcloudProjectId, "register", applicationContext)
        pubSubAccountsComDataSource.subscribe("register-responses-sub")
        pubSubLocationComDataSource = GoogleCloudPubSubDataSourceLocations(resources.openRawResource(R.raw.subscriber_credentials), gcloudProjectId, "location", applicationContext)



        sInstance = this

    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnVal = super.onStartCommand(intent, flags, startId)

        if (returnVal == START_STICKY){
            Log.d("onStartCommand", "return-sticky: $returnVal == $START_STICKY")
        }

        when(intent?.action) {
            "messagingservice.intent.action.LOGIN" -> {
                val decryptedData = getDecryptedUserData(intent)
                registerLogin(Gson().fromJson(decryptedData, LoginData::class.java), "login")
            }
            "messagingservice.intent.action.LOGOUT" -> {
                val decryptedData = getDecryptedUserData(intent)
                registerLogin(Gson().fromJson(decryptedData, LoginData::class.java), "logout")
            }
            "messagingservice.intent.action.LOCATION_CAMPAIGNS" -> {
                val newLocationCampaignvalue = intent.getBooleanExtra("locationStatus", false)
                val campaignId = intent.getIntExtra("campaign_id", -1)

                
                if (newLocationCampaignvalue) {
                    val delay = 30000L // Retraso antes de la primera ejecución en milisegundos
                    val interval = 60000L // Intervalo en milisegundos (1 minuto en este caso)
                    locationTimer = Timer()
                    locationTimer?.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            // Código a ejecutar cada minuto
                            // Por ejemplo, mostrar un mensaje
                            sendLocation()
                            
                        }
                    }, delay, interval)
                } else {
                    locationTimer?.cancel()
                    locationTimer = null
                }
                locationCampaign = newLocationCampaignvalue
                val accountDataToSend = AccountMessageModel(
                    loginData?.idToken,
                    campaign_id = campaignId,
                    subscription_status = locationCampaign
                )
                val attributesMap = mapOf("messageSender" to "client", "accountMessageType" to "campaign-subscription")
                if (campaignId != -1) pubSubAccountsComDataSource.sendMessage(accountDataToSend, attributesMap)
            }
        }
        return START_STICKY
    }

    private fun getDecryptedUserData(intent: Intent): String? {
        val encryptedData = EncryptedDataModel(
            encryptedData = intent.getByteArrayExtra("encryptedData"),
            initializationVector = intent.getByteArrayExtra("initVector")
        )
        return EncryptionHelper.decryptWithKeyStore(encryptedData)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (loginData != null){
            registerLogin(loginData!!, "logout")
            Log.d("messagingService", "Logout done.")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        pubSubServerComDataSource.cancelSubscriptionCoroutine()
        serviceAlive = false
        Log.i("Messaging Service", "Service stopped")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.i("MessagingService","Service stopped without being started: ${e.message}")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (locationCampaign) locationTimer?.cancel()
        super.onDestroy()
    }

    fun getMessages(): ArrayList<NotificationModel> {
        return pubSubServerComDataSource.getMessages()
    }


    fun isAlive(): Boolean {
        return serviceAlive
    }


    fun sendResponse(response_url: String?): Int {
        return 200
    }

    private fun registerLogin(loginData: LoginData, accountMessageType: String){
        var oncar: Boolean? = null


        when (accountMessageType){
            "login" -> {
                this.loginData = loginData
                oncar = true
            }
            "logout" -> {
                this.loginData = null
                oncar = false
            }
            "campaign-subscription" -> {

            }
        }

        val accountDataToSend = AccountMessageModel(
            loginData.idToken,
            oncar = oncar,
        )
        val attributesMap = mapOf("messageSender" to "client", "accountMessageType" to accountMessageType)
        pubSubAccountsComDataSource.sendMessage(accountDataToSend, attributesMap)
    }


    fun getLoginGoogleId(): String?{
        return loginData?.id
    }

    fun passCampaignSubscription(subscriptions: List<String>){
        Log.d("Messaging Service", "Subscriptions list in Messaging Service")
        pubSubNotificationsComDataSource.establishCampaignSubscriptions(subscriptions)
    }

    fun sendLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MessagingService", "No locations permissions!")
            return
        }
        fusedLocationClient.getCurrentLocation(100, null) // 100 is priority for high accuracy location
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("Location", "$latitude, $longitude")
                    val newLocationMessage = LocationMessageModel(loginData?.idToken, latitude.toFloat(), longitude.toFloat(), Timestamp(Date().time))
                    pubSubLocationComDataSource.sendMessage(newLocationMessage, mapOf("messageSender" to "client"))
                }
            }
    }
}