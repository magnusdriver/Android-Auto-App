package helpers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.messagingservice.R
import com.example.messagingservice.models.NotificationModel
import com.google.gson.Gson
import java.util.Random
import java.util.Calendar

class NotificationHelper(private var mContext: Context) { 

    private lateinit var CHANNEL_ID: String


    fun createNotificationChannel(channel_id: String) {
        // Create the NotificationChannel.
        CHANNEL_ID = channel_id
        val name = CHANNEL_ID.split(".").last()
        val descriptionText = mContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(channel_id, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager: NotificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

    }

    fun getNotification(notificationInfo: NotificationModel): Notification {

        var smallIcon: Int = when(notificationInfo.notification_icon_type) {
            "Charger" -> R.drawable.ev_charger_yellow
            else -> R.drawable.notifications_48px
        }

        var notification: Notification = NotificationCompat.Builder(mContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(notificationInfo.notification_title)
            .setContentText(notificationInfo.notification_message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("This is a test about building Android notifications"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        return notification

    }

    fun buildNotification(notificationInfo: NotificationModel) {

        var notificationId = genNotificationId()

        var okIntent = Intent(mContext, MessagingBroadcastReceiver::class.java).apply {
            action = "messagingservice.intent.action.OK_ACTION"
            replaceExtras(null)
            putExtra("notification_type", notificationInfo.notification_type)
            putExtra("notification_id", notificationId)
            putExtra("registered_notification_id", notificationInfo.registered_notification_id)
        }

        if (notificationInfo.notification_type == "Reserva") {
            okIntent.putExtra("latitude", notificationInfo.latitude)
            okIntent.putExtra("longitude", notificationInfo.longitude)
        }


        var cancelIntent = Intent(mContext, MessagingBroadcastReceiver::class.java).apply {
            action = "messagingservice.intent.action.CANCEL_ACTION"
            replaceExtras(null)
            putExtra("notification_id", notificationId)
            putExtra("registered_notification_id", notificationInfo.registered_notification_id)
        }


        /*
        It's needed to use different requestCodes in the pendingIntents in order to make the system be able of differentiate them.
        Without this, the BroadcastReceiver always read the same extras from the first created intent.
         */
        var okPendingIntent = PendingIntent.getBroadcast(mContext, System.currentTimeMillis().toInt(), okIntent, 0)
        var cancelPendingIntent = PendingIntent.getBroadcast(mContext, System.currentTimeMillis().toInt(), cancelIntent, 0)



        // Select small icon for notification with a 'when' condition based on the notification icon type embedded on the json notification data.
        var smallIcon: Int = when(notificationInfo.notification_icon_type) {
            "Charger" -> R.drawable.ev_charger_yellow
            else -> R.drawable.notifications_48px
        }

        var autoNotificationBuilder = CarAppExtender.Builder()
        var phoneNotificationBuilder = NotificationCompat.Builder(mContext,CHANNEL_ID)

        phoneNotificationBuilder
            .setSmallIcon(smallIcon)
            .setContentTitle(notificationInfo.notification_title)
            .setContentText(notificationInfo.notification_message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setChannelId(CHANNEL_ID)

        autoNotificationBuilder
            .setContentTitle(notificationInfo.notification_title.toString())
            .setContentText(notificationInfo.notification_message.toString())
            .setImportance(NotificationManager.IMPORTANCE_HIGH) // IMPORTANCE_HIGH allows notification to appear as a pop-up notification on head unit

        when (notificationInfo.notification_type) {
            "Info" -> {}
            "Reserva" -> {
                phoneNotificationBuilder
                    .addAction(0, "OK", okPendingIntent)
                    .addAction(0, "CANCELAR", cancelPendingIntent)
                autoNotificationBuilder
                    .addAction(R.drawable.ok, "", okPendingIntent)
                    .addAction(R.drawable.cancelar, "", cancelPendingIntent)
            }
            else -> { }
        }

        var notification: Notification = phoneNotificationBuilder.extend(autoNotificationBuilder.build()).build()

        showNotification(notification, notificationId)
    }



    private fun showNotification(notification: Notification, notificationId: Int){
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define


            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            notify(notificationId, notification)
        }

    }


    // Generates a randomized notification ID.
    private fun genNotificationId(): Int {
        val timestamp = Calendar.getInstance().timeInMillis
        val random = Random()
        val NOTIFICATION_ID = timestamp.toInt() + random.nextInt(10000)


        return NOTIFICATION_ID
    }

    companion object {
        fun json2NotificationModel(jsonData: String): NotificationModel {
            return Gson().fromJson(jsonData, NotificationModel::class.java)
        }

    }

}