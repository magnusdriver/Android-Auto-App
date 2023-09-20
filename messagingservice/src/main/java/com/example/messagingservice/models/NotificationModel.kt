package com.example.messagingservice.models

data class NotificationModel(
    var user_id: String? = null,
    var notification_icon_type: String? = null,
    var notification_title: String? = null,
    var notification_message: String? = null,
    var notification_type: String? = null,
    var notification_id: Int? = null,
    var registered_notification_id: Int? = null,
    var latitude: Float? = null,
    var longitude: Float? = null
){
}