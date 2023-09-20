package com.example.messagingservice.models

data class EncryptedDataModel(
    var dataType: String? = null,
    var initializationVector: ByteArray? = null,
    var encryptedData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedDataModel

        if (dataType != other.dataType) return false
        if (encryptedData != null) {
            if (other.encryptedData == null) return false
            if (!encryptedData.contentEquals(other.encryptedData)) return false
        } else if (other.encryptedData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataType?.hashCode() ?: 0
        result = 31 * result + (encryptedData?.contentHashCode() ?: 0)
        return result
    }
}