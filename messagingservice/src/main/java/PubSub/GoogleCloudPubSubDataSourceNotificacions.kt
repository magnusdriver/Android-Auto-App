package PubSub

import android.content.Context
import android.util.Log
import com.example.messagingservice.models.NotificationModel
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.pubsub.v1.PubsubMessage
import helpers.SharedNotificationHelper
import java.io.InputStream

class GoogleCloudPubSubDataSourceNotificacions(jsonCredentials: InputStream?, projectName: String, topic: String, context: Context):
    GoogleCloudPubSubDataSource<NotificationModel>(jsonCredentials, projectName, topic, context, clazz = NotificationModel::class.java) {

    private var activeSubscriptions: Map<String, Boolean> = mutableMapOf()

    override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        Log.i("PUBSUB", message.data.toStringUtf8())
        pubSubMessages.add(
            gson.fromJson(
                message.data.toStringUtf8(),
                clazz
            )
        )
        if (SharedNotificationHelper.sInstance?.helper == null) {
            Log.i("Problema", "MessagingService sInstance is null")
        }

        SharedNotificationHelper.sInstance?.helper?.buildNotification(gson.fromJson(message.data.toStringUtf8(), NotificationModel::class.java))
        consumer.ack()

    }

    fun establishCampaignSubscriptions(subscriptions: List<String>){
        var newSubscriptions = subscriptions.filter { it !in activeSubscriptions.keys }
        var unsubscriptions = activeSubscriptions.keys.filter { it !in subscriptions}
        if (newSubscriptions.isNotEmpty()){
            for (newSubscription in newSubscriptions){
                subscribe(newSubscription)
                Log.d("PusbSubNotifications", "new subscription: $newSubscription")
                activeSubscriptions += mapOf<String, Boolean>(newSubscription to true)
            }
        }
        if (unsubscriptions.isNotEmpty()){
            for (unsubscription in unsubscriptions){
                activeSubscriptions -= unsubscription
            }
        }
    }

}