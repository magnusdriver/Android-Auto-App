package com.example.messagingservice.models

class AccountMessageModel(
    var idToken: String? = null,
    var phone: String? = null,
    var address: String? = null,
    var oncar: Boolean? = null,
    var campaign_id: Int? = null,
    var subscription_status: Boolean? = null
){
}