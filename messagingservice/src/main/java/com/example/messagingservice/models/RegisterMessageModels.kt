package com.example.messagingservice.models

data class SubscriptionInfoMessage(
    val google_id: String,
    val response_type: String,
    val subscriptions: List<String>
)