package helpers

import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.example.messagingservice.models.EncryptedDataModel
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

class EncryptionHelper {
    companion object {
        private val ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore"
        private val ANDROID_KEY_STORE_ALIAS = "AES_KEY_CAR_APP"
        private lateinit var keyStore: KeyStore
        private var encryptCipher: Cipher? = null

        @Throws(
            KeyStoreException::class,
            NoSuchAlgorithmException::class,
            NoSuchProviderException::class,
            InvalidAlgorithmParameterException::class
        )
        fun createAndStoreSecretKey() { // Created key is stored on Android KeyStore


            val builder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
                ANDROID_KEY_STORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            val keySpec: KeyGenParameterSpec = builder
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            val aesKeyGenerator: KeyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_PROVIDER)
            aesKeyGenerator.init(keySpec)
            aesKeyGenerator.generateKey()
        }

        private fun getKey(): SecretKey {
            val existingKey = keyStore.getEntry(ANDROID_KEY_STORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            return existingKey?.secretKey ?: createKey()
        }

        private fun createKey(): SecretKey {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        ANDROID_KEY_STORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }.generateKey()
        }

        @Throws(
            KeyStoreException::class,
            UnrecoverableEntryException::class,
            NoSuchAlgorithmException::class,
            CertificateException::class,
            IOException::class,
            NoSuchPaddingException::class,
            InvalidKeyException::class,
            IllegalBlockSizeException::class,
            BadPaddingException::class
        )
        fun encryptWithKeyStore(plainText: String): EncryptedDataModel {
            // Initialize KeyStore
            val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER)
            keyStore.load(null)
            // Retrieve the key with alias androidKeyStoreAlias created before
            val keyEntry: KeyStore.SecretKeyEntry =
                keyStore.getEntry(ANDROID_KEY_STORE_ALIAS, null) as KeyStore.SecretKeyEntry
            val key: SecretKey = keyEntry.secretKey
            // Use the secret key at your convenience
            val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return EncryptedDataModel(initializationVector = cipher.iv, encryptedData = cipher.doFinal(plainText.toByteArray()))
        }

        fun decryptWithKeyStore(encryptedData: EncryptedDataModel): String? {
            // Initialize KeyStore
            val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER)
            keyStore.load(null)
            // Retrieve the key with alias androidKeyStoreAlias created before
            val keyEntry: KeyStore.SecretKeyEntry =
                keyStore.getEntry(ANDROID_KEY_STORE_ALIAS, null) as KeyStore.SecretKeyEntry
            val key: SecretKey = keyEntry.secretKey
            // Use the secret key at your convenience
            val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, encryptedData.initializationVector)
            
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedData = cipher.doFinal(encryptedData.encryptedData)
            return String(decryptedData)
        }

    }
}