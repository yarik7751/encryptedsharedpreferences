package com.yarik.encryptedsharedpreferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import by.kirich1409.viewbindingdelegate.viewBinding
import com.yarik.encryptedsharedpreferences.SPConstants.SHARED_PREFERENCES_NAME
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_EMAIL
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_FIRST_NAME
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_LAST_NAME
import com.yarik.encryptedsharedpreferences.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private val viewBinding by viewBinding(ActivityMainBinding::bind, R.id.root)

    private var sharedPreferences: SharedPreferences? = null
    private lateinit var deviceId: DeviceId
    private lateinit var cacheDataStore: CacheDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        deviceId = DeviceId(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(viewBinding) {
            btnRead.isEnabled = false
            btnWrite.isEnabled = false

            switchEncrypted.setOnCheckedChangeListener { _, _ ->
                btnRead.isEnabled = false
                btnWrite.isEnabled = false
            }

            switchCustom.setOnCheckedChangeListener { _, _ ->
                btnRead.isEnabled = false
                btnWrite.isEnabled = false
            }

            btnInit.setOnClickListener {
                resetSharedPreferences()
                sharedPreferences = initSharedPreferences()

                btnRead.isEnabled = true
                btnWrite.isEnabled = true
            }

            btnRead.setOnClickListener {
                readData()
            }

            btnWrite.setOnClickListener {
                writeData()
            }
        }
    }

    private fun initSharedPreferences(): SharedPreferences {
        val isEncrypted = viewBinding.switchEncrypted.isChecked

        val startTime = System.currentTimeMillis()

        val sharedPreferences = if (isEncrypted) {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                this,
                SHARED_PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            this.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }

        val endTime = System.currentTimeMillis()

        executionTime("INIT", startTime, endTime)

        cacheDataStore = CacheDataStore(sharedPreferences, deviceId)

        return sharedPreferences
    }

    private fun executionTime(title: String, startTime: Long, endTime: Long) {
        val executionValue = getString(R.string.execution_time, (endTime - startTime).toString())
        viewBinding.tvExecutionTime.text = "$title $executionValue"

        Log.d(TAG, viewBinding.tvExecutionTime.text.toString())
    }

    @SuppressLint("ApplySharedPref")
    private fun resetSharedPreferences() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun readData() {
        val startTime = System.currentTimeMillis()

        val isCustom = viewBinding.switchCustom.isChecked

        val email = if (isCustom) cacheDataStore.email else readSpString(SP_ARGS_EMAIL)
        val firstName = if (isCustom) cacheDataStore.firstName else readSpString(SP_ARGS_FIRST_NAME)
        val lastName = if (isCustom) cacheDataStore.lastName else readSpString(SP_ARGS_LAST_NAME)

        val endTime = System.currentTimeMillis()

        executionTime("READ", startTime, endTime)
        logXml()
    }

    private fun logXml() {
        val preferencesFile = File("${applicationInfo.dataDir}/shared_prefs/$SHARED_PREFERENCES_NAME.xml")
        val xmlData = if (preferencesFile.exists()) {
            preferencesFile.readText()
        } else ""

        Log.d(TAG, "\n$xmlData")
    }

    private fun readSpString(name: String) = sharedPreferences?.getString(name, "") ?: ""

    private fun writeData() {
        with(viewBinding) {
            val startTime = System.currentTimeMillis()

            val email = etEmail.text.toString()
            val firstName = etFirstName.text.toString()
            val lastName = etLastName.text.toString()

            if (viewBinding.switchCustom.isChecked) {
                cacheDataStore.email = email
                cacheDataStore.firstName = firstName
                cacheDataStore.lastName = lastName
            } else {
                writeSpString(SP_ARGS_EMAIL, email)
                writeSpString(SP_ARGS_FIRST_NAME, firstName)
                writeSpString(SP_ARGS_LAST_NAME, lastName)
            }

            val endTime = System.currentTimeMillis()

            executionTime("WRITE", startTime, endTime)
            //logXml()
        }
    }

    private fun writeSpString(name: String, data: String) {
        sharedPreferences?.edit()?.putString(name, data)?.apply()
    }

    companion object {
        private const val TAG = SHARED_PREFERENCES_NAME
    }
}