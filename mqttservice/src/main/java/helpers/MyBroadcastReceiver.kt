package helpers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.car.app.notification.CarNotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MyBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent?.action.equals("mqttservice.intent.action.OK_ACTION")) {
            cancelNotification(context, intent.getIntExtra("notification_id", 0))
        }

    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        Toast.makeText(context, notificationId.toString(), Toast.LENGTH_SHORT).show()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}