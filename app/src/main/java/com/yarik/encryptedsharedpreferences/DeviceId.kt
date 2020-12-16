package com.yarik.encryptedsharedpreferences

import android.content.Context

class DeviceId(val context: Context) {
    val value: String
        get() = context.getDeviceId()
}