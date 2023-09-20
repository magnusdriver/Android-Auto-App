package PubSub

interface PubSub {
    fun subscribe(subscription: String)
    fun publish(topicName: String, message: String, attributesMap: Map<String, String>?)
}