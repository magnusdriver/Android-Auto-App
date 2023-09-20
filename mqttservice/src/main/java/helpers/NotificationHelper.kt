package helpers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.mqttservice.R
import com.example.mqttservice.models.NotificationModel
import com.google.gson.Gson
import java.security.AccessController.getContext
import java.util.Random
import java.util.Calendar

class NotificationHelper(private var mContext: Context) {  

    private val CHANNEL_ID: String = mContext.getString(R.string.channel_id)


    fun createNotificationChannel(channel_id: String) {
        // Create the NotificationChannel.
        val name = mContext.getString(R.string.channel_name)
        val descriptionText = mContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(channel_id, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager: NotificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

    }

    
    fun buildNotification(notificationInfo: NotificationModel) {

        var notificationId = genNotificationId()

        var okIntent = Intent(mContext, MyBroadcastReceiver::class.java).apply {
            action = "mqttservice.intent.action.OK_ACTION"
            putExtra("order_url", notificationInfo.order_url)
            putExtra("notification_type", notificationInfo.notification_type)
            putExtra("notification_id", notificationId)
        }

        var okPendingIntent = PendingIntent.getBroadcast(mContext, 0, okIntent, 0)


        // Select small icon for notification with a 'when' condition based on the notification icon type embedded on the json notification data.
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
            .addAction(R.drawable.ok, null, okPendingIntent)
            .setAutoCancel(true)
            .extend(
                // CarAppExtender create the notification for show on Android Auto head unit too.
                CarAppExtender.Builder()
                    .setContentTitle(notificationInfo.notification_title.toString())
                    .setContentText(notificationInfo.notification_message.toString())
                    .setImportance(NotificationManager.IMPORTANCE_HIGH) // IMPORTANCE_HIGH allows notificiation to appear as pop-up on head
                    .build()
            )
            .build()

        showNotification(notification, notificationId)
    }


    private fun sendResponse(response_url: String): Int {
        return 200
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

    // Two ways to show notifications on Android auto head unit.

    /*fun showNotification(notification: Notification): Int {
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define
            var notificationId = genNotificationId()

            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return 0
            }
            notify(notificationId, notification)

            return notificationId
        }

    }*/

    //2nd way
    /*fun showNotification(notification: NotificationCompat.Builder): Int {
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define
            var notificationId = genNotificationId()

            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return 0
            }
            notify(notificationId, notification.build())

            //val notifyCar: Boolean =

            if((HelloWorldService.sInstance?.getConnStatus() ?: false)) {
                CarNotificationManager.from(HelloWorldService.sInstance?.currentSession!!.carContext).notify(notificationId, notification)
            }

            return notificationId
        }

    }*/

    // Generates a randomized notification ID.
    private fun genNotificationId(): Int {
        //val timestamp = Calendar.getInstance().timeInMillis
        //val random = Random()
        //val NOTIFICATION_ID = timestamp.toInt() + random.nextInt(1000)
        //val NOTIFICATION_ID = random.nextInt(1000000)


        //return NOTIFICATION_ID
        return 1
    }

    companion object {
        fun json2NotificationModel(jsonData: String): NotificationModel {
            return Gson().fromJson(jsonData, NotificationModel::class.java)
        }

    }

}