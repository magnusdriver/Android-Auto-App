package PubSub


import Repository.MessageRepository
import android.content.Context
import android.util.Log
import com.example.messagingservice.MessagingService
import com.example.messagingservice.models.NotificationModel
import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import helpers.SharedNotificationHelper
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.TimeUnit

/*
Code based on hofstede-matheus PubSub Android example repository. Unlicensed.
 */

open class GoogleCloudPubSubDataSource<T>(val jsonCredentials: InputStream?, val projectName: String, val topic: String, val context: Context, protected val clazz: Class<T>) : MessageRepository, PubSub,
    MessageReceiver {
    open val pubSubMessages = ArrayList<T>()
    var credentials  = GoogleCredentials.fromStream(jsonCredentials)
    private var subscriber: Subscriber? = null
    var gson = Gson()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var whileState = true


    override fun subscribe(subscription: String) {
        serviceScope.launch {

            while(whileState)
                try {
                    val subscriptionName = ProjectSubscriptionName.of(projectName, subscription)
                    Log.i("Subscription", "Starting subscription")

                    subscriber =
                        Subscriber.newBuilder(subscriptionName, this@GoogleCloudPubSubDataSource)
                                // this@GoogleCloudPubSubDataSource is a qualified this and it allows to access that class from the outer scope (The coroutine scope in this case)
                            .setCredentialsProvider { credentials }
                            .build()
                    subscriber?.startAsync()?.awaitRunning()
                    subscriber?.awaitTerminated()
                } catch (t: Throwable) {
                    Log.i("PUBSUB", t.message ?: "Something went wrong")
                }

        }
    }



    override fun publish(topicName: String, message: String, attributesMap: Map<String, String>?) {
        var publisher: Publisher? = null

        try {
            val projectTopicName = ProjectTopicName.of(projectName, topicName);
            publisher = Publisher.newBuilder(projectTopicName).setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)).build()
            val data = ByteString.copyFromUtf8(message)
            val pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .putAllAttributes(attributesMap)
                .build()
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage)

            ApiFutures.addCallback(
                messageIdFuture,
                object : ApiFutureCallback<String> {
                    override fun onSuccess(messageId: String) {
                        println("published with message id: $messageId")
                    }

                    override fun onFailure(t: Throwable) {
                        println("failed to publish: $t")
                    }
                },
                MoreExecutors.directExecutor()
            )
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    override fun getMessages(): ArrayList<NotificationModel> {
        return pubSubMessages as ArrayList<NotificationModel>
    }

    override fun sendMessage(message: NotificationModel, attributesMap: Map<String, String>?) {
        publish(topic, gson.toJson(message), attributesMap)
    }


    override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        Log.i("PUBSUB", message.data.toStringUtf8())
            pubSubMessages.add(
                gson.fromJson(
                    message.data.toStringUtf8(),
                    //NotificationModel::class.java
                    clazz
                )
            )
            if (SharedNotificationHelper.sInstance?.helper == null) {
                Log.i("Problema", "MessagingService sInstance is null")
            }

        SharedNotificationHelper.sInstance?.helper?.buildNotification(gson.fromJson(message.data.toStringUtf8(), NotificationModel::class.java))

        consumer.ack()

    }


    fun cancelSubscriptionCoroutine() {
        whileState = false
        subscriber?.stopAsync()
        subscriber?.awaitTerminated()
        serviceScope.cancel()

        Log.i("ServiceThread", "cancelSubscription called")


    }


}