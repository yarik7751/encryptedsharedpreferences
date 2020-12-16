package com.yarik.encryptedsharedpreferences

import android.content.SharedPreferences
import com.verbalize.gritx.tools.delegate.SecurePreferencesDelegate
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_EMAIL
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_FIRST_NAME
import com.yarik.encryptedsharedpreferences.SPConstants.SP_ARGS_LAST_NAME

class CacheDataStore(sharedPreferences: SharedPreferences, deviceId: DeviceId) {

    var email: String by SecurePreferencesDelegate(
        sharedPreferences,
        deviceId,
        SP_ARGS_EMAIL,
        ""
    )

    var firstName: String by SecurePreferencesDelegate(
        sharedPreferences,
        deviceId,
        SP_ARGS_FIRST_NAME,
        ""
    )

    var lastName: String by SecurePreferencesDelegate(
        sharedPreferences,
        deviceId,
        SP_ARGS_LAST_NAME,
        ""
    )
}