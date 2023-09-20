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
import com.example.masmovilcarapp.R
import java.util.Random
import java.util.Calendar

class NotificationHelperTest(private var mContext: Context) {  
    private val CHANNEL_ID: String = mContext.getString(R.string.channel_id)

    fun createNotificationChannel() {
        // Create the NotificationChannel.
        val name = mContext.getString(R.string.channel_name)
        val descriptionText = mContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager: NotificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

    }

    fun buildNotificationContent(): Notification {

        val okIntent = Intent(mContext, MyBroadcastReceiverTest::class.java)
        okIntent.action = "OK_ACTION"
        val okPendingIntent = PendingIntent.getBroadcast(mContext, 0, okIntent, 0)



        return NotificationCompat.Builder(mContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ev_charger_yellow)
            .setContentTitle("Notification Test!")
            .setContentText("This is a test about building Android notifications")
            .setStyle(NotificationCompat.BigTextStyle().
                bigText("This is a test about building Android notifications"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ok, null, okPendingIntent)
            .setAutoCancel(true)
            .extend(
                // CarAppExtender create the notification for show on Android Auto head unit too.
                CarAppExtender.Builder()
                    .setContentTitle("¡Electrolinera MasMovil en tu ruta!")
                    .setContentText("¿Quieres reservar un cargador?")
                    .setImportance(NotificationManager.IMPORTANCE_HIGH) // IMPORTANCE_HIGH allows notificiation to appear as pop-up on head
                    .build()
            )
            .build()

    }


    fun showNotification(notification: Notification): Int {
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define
            var notificationId = genNotificationId()

            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return 0
            }
            notify(notificationId, notification)

            return notificationId
        }

    }


    // Generates a randomized notification ID.
    private fun genNotificationId(): Int {
        val timestamp = Calendar.getInstance().timeInMillis
        val random = Random()
        val NOTIFICATION_ID = timestamp.toInt() + random.nextInt(1000)

        return NOTIFICATION_ID
    }

}