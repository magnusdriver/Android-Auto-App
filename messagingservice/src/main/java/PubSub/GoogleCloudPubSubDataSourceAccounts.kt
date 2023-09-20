package PubSub

import android.content.Context
import android.util.Log
import com.example.messagingservice.MessagingService
import com.example.messagingservice.models.AccountMessageModel
import com.example.messagingservice.models.NotificationModel
import com.example.messagingservice.models.SubscriptionInfoMessage
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.gson.Gson
import com.google.pubsub.v1.PubsubMessage
import helpers.SharedNotificationHelper
import java.io.InputStream
import org.json.JSONObject

class GoogleCloudPubSubDataSourceAccounts(jsonCredentials: InputStream?, projectName: String, topic: String, context: Context):
    GoogleCloudPubSubDataSource<AccountMessageModel>(jsonCredentials, projectName, topic, context, clazz = AccountMessageModel::class.java){


    override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        Log.i("PUBSUB", message.data.toStringUtf8())

        var messageJsonObject = JSONObject(message.data.toStringUtf8())

        if (messageJsonObject.has("google_id")) {

            if (messageJsonObject.getString("google_id") == MessagingService.sInstance?.getLoginGoogleId()) {

                if (messageJsonObject.has("response_type")) {
                    when (messageJsonObject.getString("response_type")) {
                        "subscriptions-info" -> {
                            val gson = Gson()
                            val subscriptionsMessage = gson.fromJson(
                                message.data.toStringUtf8(),
                                SubscriptionInfoMessage::class.java
                            )
                            Log.d("PubSubAccounts", "Subscriptions message received")
                            MessagingService.sInstance?.passCampaignSubscription(
                                subscriptionsMessage.subscriptions
                            )

                        } 
                    }
                }
            }
        }


        pubSubMessages.add(
            gson.fromJson(
                message.data.toStringUtf8(),
                clazz
            )
        )

        if (SharedNotificationHelper.sInstance?.helper == null) {
            Log.i("Problema", "MessagingService sInstance is null")
        }

        consumer.ack()


    }

    fun sendMessage(message: AccountMessageModel, attributesMap: Map<String, String>?) {
        publish(topic, gson.toJson(message), attributesMap)
    }

}