package helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyBroadcastReceiverTest: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "OK_ACTION") {

        }
    }
}