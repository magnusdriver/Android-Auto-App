package PubSub

import android.content.Context
import com.example.messagingservice.models.LocationMessageModel
import com.example.messagingservice.models.NotificationModel
import java.io.InputStream

class GoogleCloudPubSubDataSourceLocations(jsonCredentials: InputStream?, projectName: String, topic: String, context: Context):
    GoogleCloudPubSubDataSource<LocationMessageModel>(jsonCredentials, projectName, topic, context, clazz = LocationMessageModel::class.java) {

    fun sendMessage(message: LocationMessageModel, attributesMap: Map<String, String>?) {
        publish(topic, gson.toJson(message), attributesMap)
    }

    }