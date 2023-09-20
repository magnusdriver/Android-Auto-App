package helpers

import android.app.PendingIntent.getActivity
import android.content.*
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.messagingservice.MessagingService
import com.example.messagingservice.R
import com.example.messagingservice.models.EncryptedDataModel
import com.example.messagingservice.models.LoginData
import com.example.messagingservice.models.NotificationModel
import com.google.gson.Gson

class MessagingBroadcastReceiver: BroadcastReceiver() {

    private lateinit var messagingService: MessagingService
    override fun onReceive(context: Context, intent: Intent) {

        when(intent.action) {
            "messagingservice.intent.action.OK_ACTION" -> {
                Toast.makeText(context, "OK pulsado.", Toast.LENGTH_SHORT).show()
                cancelNotification(context, intent.getIntExtra("notification_id", 0))
                Thread.sleep(1000)
                if (intent.getStringExtra("notification_type") == "Reserva") {
                    val latitude = intent.getFloatExtra("latitude", 0f)
                    val longitude = intent.getFloatExtra("longitude", 0f)
                    val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                    val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapsIntent.setPackage("com.google.android.apps.maps")
                    mapsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    if (mapsIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapsIntent)
                    } else {
                        // Google Maps no está instalado
                        // Puedes manejar esta situación según tus necesidades
                    }

                    SharedNotificationHelper.sInstance?.helper?.buildNotification(
                        NotificationModel(
                            notification_type = "Info",
                            notification_icon_type = "Notificacion",
                            notification_title = context.getString(R.string.reserve_title),
                            notification_message = context.getString(R.string.reserve_message)
                        )
                    )
                }
                
            }
            "messagingservice.intent.action.CANCEL_ACTION" -> {
                Toast.makeText(context, "Cancelar pulsado.", Toast.LENGTH_SHORT).show()
                Log.d("BroadcastReceiver", "notification id: " + intent.getIntExtra("notification_id", 0))
                cancelNotification(context, intent.getIntExtra("notification_id", 0))
            }
            "carapp.intent.action.LOGIN_ACTION" -> {
                val encryptedData = EncryptedDataModel(encryptedData = intent.getByteArrayExtra("encryptedData"), initializationVector = intent.getByteArrayExtra("initVector"))
                val decryptedData = EncryptionHelper.decryptWithKeyStore(encryptedData)


                val serviceIntent = Intent(context, MessagingService::class.java).apply {
                    action = "messagingservice.intent.action.LOGIN"
                    putExtra("loginData", decryptedData)
                }
                context.startService(serviceIntent)


            }
        }

    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        Toast.makeText(context, notificationId.toString(), Toast.LENGTH_SHORT).show()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}