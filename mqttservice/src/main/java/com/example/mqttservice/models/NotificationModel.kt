package com.example.mqttservice.models

data class NotificationModel(
    var user_id: String? = null,
    var notification_icon_type: String? = null,
    var notification_title: String? = null,
    var notification_message: String? = null,
    var notification_type: String? = null,
    var order_url: String? = null
){
}
