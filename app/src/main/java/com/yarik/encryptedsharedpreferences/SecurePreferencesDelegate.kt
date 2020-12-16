package com.verbalize.gritx.tools.delegate

import android.content.SharedPreferences
import android.util.Base64
import com.yarik.encryptedsharedpreferences.DeviceId
import com.yarik.encryptedsharedpreferences.toBase64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


@Suppress("UNCHECKED_CAST")
class SecurePreferencesDelegate(
    private val preferences: SharedPreferences,
    private val deviceId: DeviceId,
    private val name: String,
    private val defValue: String
) : ReadWriteProperty<Any?, String> {

    private val byteName = name.toBase64()
    private val saltByteName = "${name}salt".toBase64()
    private val ivByteName = "${name}iv".toBase64()
    private val key = deviceId.value.toBase64()

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        with(preferences) {
            val encryptedData = getEncryptedData(EncryptedDataType.ENCRYPTED_PARAM)
            val encryptedSalt = getEncryptedData(EncryptedDataType.SALT_PARAM)
            val encryptedIv = getEncryptedData(EncryptedDataType.IV_PARAM)

            val map = HashMap<String, ByteArray>().apply {
                this[EncryptedDataType.SALT_PARAM.type] = encryptedSalt
                this[EncryptedDataType.IV_PARAM.type] = encryptedIv
                this[EncryptedDataType.ENCRYPTED_PARAM.type] = encryptedData
            }

            val decryptedData = decryptData(map, key)
            return decryptedData?.let { String(decryptedData) } ?: ""
        }
    }

    private fun SharedPreferences.getEncryptedData(type: EncryptedDataType): ByteArray {
        val name = type.getNameByType()
        val encryptedDataTextBase64 = getString(name, defValue) ?: defValue
        return Base64.decode(encryptedDataTextBase64, Base64.NO_WRAP)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        encryptBytes(value.toByteArray(), key).apply {
            putEncryptedData(EncryptedDataType.ENCRYPTED_PARAM)
            putEncryptedData(EncryptedDataType.SALT_PARAM)
            putEncryptedData(EncryptedDataType.IV_PARAM)
        }
    }

    private fun HashMap<String, ByteArray>.putEncryptedData(type: EncryptedDataType) {
        val encryptedDataTextBase64 = Base64.encodeToString(this[type.type], Base64.NO_WRAP)
        preferences.edit().putString(type.getNameByType(), encryptedDataTextBase64).apply()
    }

    private fun EncryptedDataType.getNameByType() = when (this) {
        EncryptedDataType.ENCRYPTED_PARAM -> byteName
        EncryptedDataType.SALT_PARAM -> saltByteName
        EncryptedDataType.IV_PARAM -> ivByteName
    }

    private fun encryptBytes(
        plainTextBytes: ByteArray,
        passwordString: String
    ): HashMap<String, ByteArray> {
        val map = HashMap<String, ByteArray>()
        try {
            val random = SecureRandom()
            val salt = ByteArray(256)
            random.nextBytes(salt)

            val passwordChar = passwordString.toCharArray()
            val pbKeySpec = PBEKeySpec(passwordChar, salt, 1324, 256)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
            val keySpec = SecretKeySpec(keyBytes, "AES")

            val ivRandom = SecureRandom()
            val iv = ByteArray(16)
            ivRandom.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            val encrypted = cipher.doFinal(plainTextBytes)
            map[EncryptedDataType.SALT_PARAM.type] = salt
            map[EncryptedDataType.IV_PARAM.type] = iv
            map[EncryptedDataType.ENCRYPTED_PARAM.type] = encrypted
        } catch (e: Exception) {

        }
        return map
    }

    private fun decryptData(
        map: HashMap<String, ByteArray>,
        passwordString: String
    ): ByteArray? {
        var decrypted: ByteArray? = null
        try {
            val salt = map[EncryptedDataType.SALT_PARAM.type]
            val iv = map[EncryptedDataType.IV_PARAM.type]
            val encrypted = map[EncryptedDataType.ENCRYPTED_PARAM.type]

            val passwordChar = passwordString.toCharArray()
            val pbKeySpec = PBEKeySpec(passwordChar, salt, 1324, 256)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
            val keySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            decrypted = cipher.doFinal(encrypted)
        } catch (e: java.lang.Exception) {
        }
        return decrypted
    }

    private enum class EncryptedDataType(val type: String) {
        ENCRYPTED_PARAM("encrypted"),
        SALT_PARAM("salt"),
        IV_PARAM("iv")
    }
}