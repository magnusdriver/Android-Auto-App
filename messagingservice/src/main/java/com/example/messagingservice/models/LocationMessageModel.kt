package com.example.messagingservice.models

import java.sql.Timestamp

data class LocationMessageModel(
    var tokenId: String? = null,
    var latitude: Float? = null,
    var longitude: Float? = null,
    var position_time: Timestamp? = null
)