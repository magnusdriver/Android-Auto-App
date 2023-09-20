package Repository

import com.example.messagingservice.models.AccountMessageModel
import com.example.messagingservice.models.NotificationModel


interface MessageRepository {
    fun getMessages() : List<NotificationModel>
    fun sendMessage(message: NotificationModel, attributesMap: Map<String,String>?)
}